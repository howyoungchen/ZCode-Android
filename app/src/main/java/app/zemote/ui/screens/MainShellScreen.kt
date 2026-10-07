package app.zemote.ui.screens

import app.zemote.R
import app.zemote.ui.logger.ZemoteLogger

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.zemote.protocol.TaskEntry
import app.zemote.state.Account
import app.zemote.state.AccountStore
import app.zemote.state.AppSessionViewModel
import app.zemote.state.ConnectionState
import app.zemote.ui.component.DeviceAvatar
import app.zemote.ui.component.StatusDot
import app.zemote.ui.theme.ThemeManager
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * 连接后主界面：对齐官方移动端远控页（webRemoteControl.mobileHome）——
 * 大标题头部 + 提示卡 + 「当前设备上的工作区和任务」区块 + 可展开的工作区卡片。
 * 进入即自动发起连接，状态机驱动 连接动画 → 仪表盘 / 错误重试 的全过程切换。
 */
@Composable
fun MainShellScreen(
    account: Account,
    session: AppSessionViewModel,
    store: AccountStore,
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    themeManager: ThemeManager? = null,
) {
    val uiState by session.uiState.collectAsState()
    val accounts by store.accounts.collectAsState()
    var showDeviceSheet by remember { mutableStateOf(false) }

    // 进入页面自动连接（重复调用安全：内部有互斥与复用）
    LaunchedEffect(account.id) { session.connect(account) }

    // 连接成功后会启动 KeepAliveService 前台服务（常驻通知，防进程被系统回收）。
    // Android 13+ 通知是运行时权限：清单里声明了 POST_NOTIFICATIONS，
    // 但**全仓没有任何地方申请过它** —— 结果是保活通知被系统静默丢弃，
    // 用户既看不到"正在保持连接"的提示，也无法点通知回到 App。
    // 这里放在真正要建立连接的页面申请，语义最贴切（而不是一启动就弹窗打断用户）。
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 拒绝不影响连接，只是保活通知不可见 */ }
    LaunchedEffect(account.id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val status = uiState.statuses[account.id] ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // 头部：官方 mobileHome 头 —— 标题 + 副标题 + 主题菜单，底色 --color-header + 分隔线
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(app.zemote.ui.theme.headerColor())
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.dashboard_connected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 切换已配对设备（App 特有入口，官方单设备页面没有）
            IconButton(onClick = { showDeviceSheet = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.SwapHoriz,
                    contentDescription = stringResource(R.string.switch_device),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            ThemeMenuButton(themeManager)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        AnimatedContent(
            targetState = status.state,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(initialScale = 0.96f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(tween(220)) + scaleOut(targetScale = 0.96f, animationSpec = tween(220)))
            },
            label = "shellState",
        ) { state ->
            when (state) {
                ConnectionState.CONNECTING -> ConnectingContent(account)
                ConnectionState.ERROR -> ErrorContent(
                    message = status.message ?: stringResource(R.string.connect_failed),
                    onRetry = { session.connect(account) },
                )
                else -> {
                    val client = session.clientOf(account.id)
                    if (client == null) {
                        ErrorContent(message = stringResource(R.string.device_not_connected), onRetry = { session.connect(account) })
                    } else {
                        DashboardContent(
                            client = client,
                            session = session,
                            accountId = account.id,
                            onOpenTask = { workspaceKey, taskId ->
                                onNavigateToChat(workspaceKey, taskId)
                            },
                            onStartDraft = { workspaceKey ->
                                onNavigateToChat(workspaceKey, "new")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDeviceSheet) {
        DeviceSwitchSheet(
            accounts = accounts,
            activeId = uiState.activeId,
            statuses = uiState.statuses,
            onSwitch = {
                showDeviceSheet = false
                session.switchTo(it)
            },
            onDisconnect = { session.disconnect(it) },
            onAddDevice = {
                showDeviceSheet = false
                onBack()
            },
            onDismiss = { showDeviceSheet = false },
        )
    }
}

@Composable
private fun ConnectingContent(account: Account) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            StatusDot(
                color = MaterialTheme.colorScheme.primary,
                pulsing = true,
                size = 96,
            )
            DeviceAvatar(id = account.id, iconSize = 34, corner = 24)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.pairing_with, account.label), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            stringResource(R.string.pairing_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(52.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.connect_failed), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.retry_connect))
        }
    }
}

/**
 * 仪表盘：官方 mobileHome 主体 —— 提示卡 + 区块头（统计 + 刷新）+ 工作区卡片列表。
 * bootstrap 一次同时取工作区与任务，按 workspaceIdentity / workspacePath 分组进卡。
 */
@Composable
private fun DashboardContent(
    client: app.zemote.protocol.ZemoteClient,
    session: AppSessionViewModel,
    accountId: String,
    onOpenTask: (workspaceKey: String, taskId: String) -> Unit,
    onStartDraft: (workspaceKey: String) -> Unit,
) {
    var workspaces by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<DashboardTask>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }
    // suspend 块里不能用 stringResource，先在组合期取好
    val unnamedTitle = stringResource(R.string.unnamed_session)

    suspend fun refresh() {
        loading = true
        error = null
        try {
            val result = client.bootstrap()
            ZemoteLogger.info("bootstrap", result.toString())
            @Suppress("UNCHECKED_CAST")
            workspaces = (result["workspaces"] as? List<Map<String, Any>>) ?: emptyList()
            // 任务与工作区同源解析（等价 fetchTasksFromBootstrap 的全量模式，不再二次握手）
            val rawTasks = result["tasks"] as? List<*> ?: emptyList<Any>()
            tasks = rawTasks.mapNotNull { t ->
                val m = t as? Map<*, *> ?: return@mapNotNull null
                val id = m["taskId"]?.toString() ?: return@mapNotNull null
                DashboardTask(
                    entry = TaskEntry(
                        taskId = id,
                        title = m["title"]?.toString() ?: unnamedTitle,
                        status = m["displayStatus"]?.toString(),
                        workspacePath = m["workspacePath"]?.toString(),
                        workspaceLabel = m["workspaceLabel"]?.toString(),
                        updatedAt = (m["updatedAt"] as? Number)?.toLong(),
                    ),
                    identity = m["workspaceIdentity"]?.toString(),
                )
            }
            // 缓存每个工作区的原始 map（V4 会话握手的 scopeParams 需要）
            workspaces.forEach { ws ->
                val key = (ws["workspaceIdentity"] as? String)
                    ?: (ws["workspacePath"] as? String)
                    ?: return@forEach
                session.cacheWorkspaceScope(accountId, key, ws)
            }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    LaunchedEffect(client, retryKey) { refresh() }

    // 连接恢复后自动重新加载（断线重连、被其他客户端抢占后抢回等场景）
    LaunchedEffect(client) {
        var wasPaired = false
        client.state.collect { st ->
            if (st == app.zemote.protocol.ZemoteClient.ZemoteClientState.PAIRED) {
                if (wasPaired) refresh()
                wasPaired = true
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 官方提示卡：rounded-lg 边框卡 + 次要文字
        item {
            Surface(
                color = app.zemote.ui.theme.cardContainerColor(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.dashboard_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        // 区块头：标题 + 统计 + 刷新（官方另有折叠/整理，均为持久偏好，App 不做）
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dashboard_section),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        stringResource(R.string.dashboard_summary, workspaces.size, tasks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { retryKey++ },
                    modifier = Modifier.size(32.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.dashboard_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        // 官方错误条：destructive 边框卡
        error?.let { err ->
            item {
                Surface(
                    color = app.zemote.ui.theme.cardContainerColor(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.load_workspaces_failed, err),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        if (loading && workspaces.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.loading_workspaces),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!loading && error == null && workspaces.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.no_workspaces),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(workspaces, key = { w ->
            (w["workspaceIdentity"] as? String ?: w["workspacePath"] as? String ?: w.hashCode().toString())
        }) { workspace ->
            WorkspaceCard(
                workspace = workspace,
                tasks = tasksOf(tasks, workspace),
                onOpenTask = onOpenTask,
                onStartDraft = onStartDraft,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** 仪表盘任务：TaskEntry + 归属工作区的 workspaceIdentity（TaskEntry 本身不带） */
private data class DashboardTask(val entry: TaskEntry, val identity: String?)

/** 任务归属：按 workspaceIdentity / workspacePath 与卡片键匹配 */
private fun tasksOf(tasks: List<DashboardTask>, workspace: Map<String, Any>): List<TaskEntry> {
    val identity = workspace["workspaceIdentity"] as? String
    val path = workspace["workspacePath"] as? String
    val keys = listOfNotNull(identity, path).toSet()
    return tasks
        .filter { t -> keys.contains(t.identity) || keys.contains(t.entry.workspacePath) }
        .map { it.entry }
        .sortedByDescending { it.updatedAt ?: 0L }
}

/**
 * 工作区卡片：官方 mobileHome 卡 —— rounded-lg 边框卡、图标方块、类型徽章、
 * 等宽路径、更新时间、任务数 + chevron，展开后内嵌任务列表（分隔线隔开）。
 * 「+」为官方的新建任务入口（outline 小按钮）。
 */
@Composable
private fun WorkspaceCard(
    workspace: Map<String, Any>,
    tasks: List<TaskEntry>,
    onOpenTask: (workspaceKey: String, taskId: String) -> Unit,
    onStartDraft: (workspaceKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val key = workspace["workspaceIdentity"] as? String
        ?: workspace["workspacePath"] as? String ?: ""
    var expanded by remember(key) { mutableStateOf(false) }

    val title = (workspace["label"] as? String)
        ?: (workspace["workspacePath"] as? String)?.let(::lastPathSegment)
        ?: workspace["workspaceIdentity"] as? String ?: stringResource(R.string.unknown_workspace)
    val path = workspace["workspacePath"] as? String ?: ""
    val kind = workspace["kind"] as? String ?: ""
    val updatedTs = sequenceOf(
        workspace["updatedAt"],
        workspace["lastModified"],
        workspace["lastActivityAt"],
    ).filterIsInstance<Number>().firstOrNull()?.toLong()

    Surface(
        color = app.zemote.ui.theme.cardContainerColor(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // ── 卡头：图标方块 + 名称/徽章/路径/更新时间 + 任务数/chevron + 新建 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 官方图标方块：size-8 rounded-lg bg-surface，图标 subtle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(app.zemote.ui.theme.surfaceTintColor(), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // 官方类型徽章：描边胶囊（本地/远程/对话）
                        WorkspaceKindBadge(kind)
                    }
                    if (path.isNotEmpty()) {
                        Text(
                            path,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = app.zemote.ui.theme.subtlestColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    updatedTs?.let {
                        Text(
                            stringResource(R.string.ws_updated_at, relativeTime(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.ws_task_count, tasks.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                // 官方「+」新建任务：outline 小方块按钮
                Surface(
                    onClick = { onStartDraft(key) },
                    shape = RoundedCornerShape(8.dp),
                    color = app.zemote.ui.theme.surfaceTintColor(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.new_session),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
            // ── 展开态：任务列表（官方 border-t + px-2 py-2） ──
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (tasks.isEmpty()) {
                        Text(
                            stringResource(R.string.ws_task_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                    tasks.forEach { task ->
                        TaskRow(
                            task = task,
                            onClick = { onOpenTask(key, task.taskId) },
                        )
                    }
                }
            }
        }
    }
}

/** 官方类型徽章：rounded-full 描边胶囊（本地 / 远程 / 对话） */
@Composable
private fun WorkspaceKindBadge(kind: String) {
    val label = when (kind) {
        "remote" -> stringResource(R.string.ws_kind_remote)
        "conversation" -> stringResource(R.string.ws_kind_conversation)
        else -> stringResource(R.string.ws_kind_local)
    }
    Surface(
        color = app.zemote.ui.theme.surfaceTintColor(),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/**
 * 任务行：官方 min-h-12 圆角行 —— 标题 + 工作区/时间 + 状态胶囊。
 * 运行中 = accent 底 + spinner；已完成 = 实心绿 + 对勾；错误 = 实心红；空闲 = 描边胶囊。
 */
@Composable
private fun TaskRow(task: TaskEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 官方任务行只显示相对时间（工作区归属由卡片标题表达）
            if (task.updatedAt != null) {
                Text(
                    relativeTime(task.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        TaskStatusPill(task.status)
    }
}

/** 官方状态胶囊（p4 配色）：running=accent底+brand40描边；completed=实心success；error=实心destructive */
@Composable
private fun TaskStatusPill(status: String?) {
    val dark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val success = if (dark) app.zemote.ui.theme.StatusSuccessDark else app.zemote.ui.theme.StatusSuccess
    when (status) {
        "running" -> Surface(
            color = app.zemote.ui.theme.accentColor(),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier.padding(start = 7.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 1.2.dp,
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.status_running),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        "completed" -> Surface(
            color = success,
            contentColor = androidx.compose.ui.graphics.Color.White,
            shape = RoundedCornerShape(50),
        ) {
            Row(
                modifier = Modifier.padding(start = 7.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.status_completed),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
        "error" -> Surface(
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                stringResource(R.string.status_error),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        else -> Surface(
            color = app.zemote.ui.theme.surfaceTintColor(),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                stringResource(R.string.status_idle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/** 相对时间：刚刚 / N 分 / N 小时 / N 天（更早显示日期） */
@Composable
private fun relativeTime(ts: Long): String {
    val delta = max(0L, System.currentTimeMillis() - ts)
    return when {
        delta < 60_000L -> stringResource(R.string.rel_now)
        delta < 3_600_000L -> stringResource(R.string.rel_minutes, delta / 60_000L)
        delta < 86_400_000L -> stringResource(R.string.rel_hours, delta / 3_600_000L)
        delta < 30L * 86_400_000L -> stringResource(R.string.rel_days, delta / 86_400_000L)
        else -> java.text.SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(ts))
    }
}

/**
 * 取路径最后一段（Windows `\` 与 POSIX `/` 都算分隔符）。
 *
 * 旧实现把 `split("[\\\\/]".toRegex())` 直接写在 `WorkspaceCard` 的组合体里：
 * Kotlin 的 `String.toRegex()` **没有缓存**，所以每一次重组都会重新编译一次正则
 * （工作区数量 × 重组次数）。这里改成纯字符切分，连正则都不需要。
 */
private fun lastPathSegment(path: String): String? =
    path.split('/', '\\').lastOrNull { it.isNotEmpty() }
