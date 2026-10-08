package app.zemote.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 工作区 Git 操作（官方 gitService：通道 RPC 名与服务方法名一致）。
 *
 * 消息名与字段对照官方 Web 客户端 `gitService` 接口：getRepositorySummary /
 * getLocalBranches / switchBranch / getChanges / stagePaths / commit / push / getIdentity。
 * 面板「Git 工具」的数据源；只在用户点按时才发请求，不做轮询。
 */
class GitService(
    private val channels: ChannelClient,
    private val workspacePath: String,
    private val workspaceIdentity: String? = null,
) {
    private fun args(extra: Map<String, Any?> = emptyMap()): Map<String, Any?> = buildMap {
        put("workspacePath", workspacePath)
        if (!workspaceIdentity.isNullOrBlank()) put("workspaceIdentity", workspaceIdentity)
        putAll(extra)
    }

    private suspend fun rpc(method: String, extra: Map<String, Any?> = emptyMap()): Any? =
        runCatching {
            channels.call(ChannelClient.Channel.GIT, method, listOf(args(extra)), timeoutMs = 20_000)
        }.getOrNull()

    /** 当前仓库概要（分支 / 脏状态）；无仓库或不可用时返回 null */
    suspend fun repositorySummary(): GitSummary? = withContext(Dispatchers.IO) {
        val res = rpc("getRepositorySummary") as? Map<*, *> ?: return@withContext null
        GitSummary(
            branchName = res["branchName"]?.toString(),
            trackingBranchName = res["trackingBranchName"]?.toString(),
            headRefType = res["headRefType"]?.toString() ?: "branch",
            ahead = (res["ahead"] as? Number)?.toInt() ?: 0,
            behind = (res["behind"] as? Number)?.toInt() ?: 0,
            isDirty = res["isDirty"] == true,
            isGitAvailable = res["isGitAvailable"] == true,
            isRepository = res["isRepository"] == true,
            repoRoot = res["repoRoot"]?.toString(),
        )
    }

    /** 本地分支列表（currentBranch 为当前分支名） */
    suspend fun localBranches(): GitBranches? = withContext(Dispatchers.IO) {
        val res = rpc("getLocalBranches") as? Map<*, *> ?: return@withContext null
        val branches = (res["branches"] as? List<*>).orEmpty().mapNotNull { b ->
            when (b) {
                is String -> b
                is Map<*, *> -> b["name"]?.toString()
                else -> null
            }
        }
        GitBranches(
            currentBranch = res["currentBranch"]?.toString()
                ?: (res["currentBranch"] as? Map<*, *>)?.get("name")?.toString(),
            branches = branches,
        )
    }

    /** 切换分支；被工作区脏状态阻塞时 ok=false 并携带 issues */
    suspend fun switchBranch(targetBranchName: String): GitSwitchResult? = withContext(Dispatchers.IO) {
        val res = rpc("switchBranch", mapOf("targetBranchName" to targetBranchName)) as? Map<*, *>
            ?: return@withContext null
        GitSwitchResult(
            ok = res["ok"] == true,
            branchName = res["branchName"]?.toString(),
            issues = (res["issues"] as? List<*>).orEmpty().mapNotNull { it ->
                (it as? Map<*, *>)?.get("code")?.toString()
            },
        )
    }

    /** 工作区文件更改（sourceId 取 unstaged / staged） */
    suspend fun changes(sourceId: String): List<GitFileChange> = withContext(Dispatchers.IO) {
        val res = rpc("getChanges", mapOf("sourceId" to sourceId)) as? List<*> ?: return@withContext emptyList()
        res.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val path = (m["path"] ?: m["stagePath"])?.toString() ?: return@mapNotNull null
            GitFileChange(
                path = path,
                additions = (m["additions"] as? Number)?.toInt() ?: 0,
                deletions = (m["deletions"] as? Number)?.toInt() ?: 0,
                status = m["status"]?.toString(),
            )
        }
    }

    /** 暂存文件 */
    suspend fun stagePaths(paths: List<String>): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            channels.call(ChannelClient.Channel.GIT, "stagePaths", listOf(args(mapOf("paths" to paths))), timeoutMs = 20_000)
            true
        }.getOrDefault(false)
    }

    /** 提交（stagedOnly=true 只提交已暂存） */
    suspend fun commit(message: String, paths: List<String>? = null, stagedOnly: Boolean = true): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = buildMap<String, Any?> {
                    put("message", message)
                    if (!paths.isNullOrEmpty()) put("paths", paths)
                    put("stagedOnly", stagedOnly)
                }
                channels.call(ChannelClient.Channel.GIT, "commit", listOf(args(payload)), timeoutMs = 30_000)
                true
            }.getOrDefault(false)
        }

    /** 推送当前分支 */
    suspend fun push(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            channels.call(ChannelClient.Channel.GIT, "push", listOf(args()), timeoutMs = 60_000)
            true
        }.getOrDefault(false)
    }
}

/** git 仓库概要（官方 getRepositorySummary） */
data class GitSummary(
    val branchName: String?,
    val trackingBranchName: String?,
    val headRefType: String,
    val ahead: Int,
    val behind: Int,
    val isDirty: Boolean,
    val isGitAvailable: Boolean,
    val isRepository: Boolean,
    val repoRoot: String?,
)

/** 本地分支列表 */
data class GitBranches(
    val currentBranch: String?,
    val branches: List<String>,
)

/** 切换分支结果 */
data class GitSwitchResult(
    val ok: Boolean,
    val branchName: String?,
    val issues: List<String>,
)

/** 单个文件更改 */
data class GitFileChange(
    val path: String,
    val additions: Int,
    val deletions: Int,
    val status: String?,
)
