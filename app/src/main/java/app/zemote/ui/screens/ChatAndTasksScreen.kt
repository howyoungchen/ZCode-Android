package app.zemote.ui.screens

import app.zemote.R

import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.zemote.protocol.ConvKinds
import app.zemote.protocol.ConvRow
import app.zemote.protocol.PendingInteraction
import app.zemote.protocol.BackgroundWork
import app.zemote.protocol.TaskEntry
import app.zemote.state.AppSessionViewModel
import app.zemote.state.AppSettings
import app.zemote.ui.theme.ThemeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ────────────────────────── 对话页 ──────────────────────────

/** 对话页：对齐官方移动端远控页 —— 任务会话头 + 任务标签行 + 文档流时间线 + 官方发送栏 */
@Composable
fun ChatScreen(
    workspaceKey: String,
    sessionId: String?,
    session: AppSessionViewModel,
    onBack: () -> Unit,
    onOpenSubagent: (String, String, String) -> Unit = { _, _, _ -> },
    readOnly: Boolean = false,
    themeManager: ThemeManager? = null,
) {
    val accountId = session.activeId
    var repo by remember { mutableStateOf<app.zemote.protocol.ConversationV4Session?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    // sessionId 变化时重置上传状态和文件列表，避免跨会话残留
    var pendingFiles by remember(sessionId) { mutableStateOf(listOf<PendingFile>()) }
    var uploadStatus by remember(sessionId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    // 自动跟随状态：留在这一层（低频状态，发送时置位），时间线内部只读不写。
    // 这里刻意用显式 MutableState 而不是 `by remember { mutableStateOf(...) }`：
    // 只有拿到那个 State 实例，传给 MessageTimeline 的读取才是稳定引用。
    // 官方移动端没有跟随开关：贴底时自动跟随、上翻时不打扰，这里保持同样行为。
    val autoFollowState = remember { mutableStateOf(true) }
    var autoFollow by autoFollowState
    // 发送失败提示（服务端拒绝 / 上传失败），短暂展示后自动消失
    var sendError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sendError) {
        if (sendError != null) {
            delay(5_000)
            sendError = null
        }
    }

    // 系统文件选择器：图片和任意文件均可选，选中即加入待发列表
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: return@mapNotNull null
                        val name = queryDisplayName(context, uri)
                            ?: uri.lastPathSegment?.substringAfterLast('/')
                            ?: context.getString(R.string.attach)
                        PendingFile(name, context.contentResolver.getType(uri) ?: guessMime(name), bytes)
                    }.getOrNull()
                }
            }
            pendingFiles = pendingFiles + loaded
        }
    }

    val deviceNotConnectedText = stringResource(R.string.device_not_connected)
    val deviceNotConnectedRetryText = stringResource(R.string.device_not_connected_retry)

    // 打开会话（含自愈）：设备未就绪重试 → 打开 → 若 4 秒后历史/模型仍为空，
    // 说明这条桥在服务端已失效（如被抢占后遗留），销毁重建整条通道再试一次
    LaunchedEffect(accountId, workspaceKey, sessionId) {
        if (accountId == null) {
            error = deviceNotConnectedText
            return@LaunchedEffect
        }
        // 快速进入时设备可能仍在重连（connections 里还没有 client），有限次重试而不是永久空白
        var opened: app.zemote.protocol.ConversationV4Session? = null
        for (attempt in 1..5) {
            opened = runCatching { session.conversationFor(accountId, workspaceKey) }
                .getOrNull()
            if (opened != null) break
            delay(1000)
        }
        if (opened == null) {
            error = deviceNotConnectedRetryText
            return@LaunchedEffect
        }
        repo = opened
        // 立即启动异步加载（握手+订阅+历史），不阻塞界面渲染
        // 用 scope.launch 而非裸 launch：composable 销毁时自动取消，防止泄漏
        scope.launch { opened.openConversation(sessionId) }
        scope.launch { runCatching { opened.openSessionsIndex() } }
    }

    // 注意：这里刻意不订阅 repo.rows——行数据的订阅放在 MessageTimeline 内部，
    // 否则每个流式 token 都会让整个聊天页（顶栏 / 输入栏 / 队列卡片）一起重组。
    //
    // 同理，只在这里订阅「页面骨架真正需要」的低频状态。config / usage / modelOptions /
    // stopWorkId / followupMode / queueItems / autoDrain 全部下沉到 ComposerSection，
    // 因为其中 `usage` 在流式输出期间会随每个 `state.updated` 补丁更新（约 16 次/秒），
    // 订阅在这一层等于每帧都把整个聊天页（含 MessageTimeline 与所有可见消息）重组一遍
    // ——这是「UI 卡顿、交互响应迟缓」的主要根因。
    val working by (repo?.agentWorking?.collectAsState() ?: remember { mutableStateOf(false) })
    val historyState by (repo?.historyState?.collectAsState()
        ?: remember { mutableStateOf(app.zemote.protocol.HistoryState.LOADING) })
    val activeId by (repo?.activeSessionId?.collectAsState() ?: remember { mutableStateOf(sessionId) })

    // 任务面板开关（面板内部自己订阅 pendingInteractions / backgroundWorks）
    var showTaskPanel by remember { mutableStateOf(false) }

    // 附件内容加载（收到的图片消息按 ref 拉取渲染）。
    // 这个 lambda 会一路传到每个可见的时间线条目，**必须实例稳定**：
    // 用 rememberUpdatedState 持有最新的 repo / activeId，再 remember 出唯一的 lambda 实例。
    // 旧实现每次重组都新建 lambda，MessageTimeline 参数恒不相等 → 永远无法跳过重组，
    // 所有可见消息都会跟着重组（Markdown 文本重新布局），卡顿被进一步放大。
    val repoState = rememberUpdatedState(repo)
    val activeIdState = rememberUpdatedState(activeId)
    val loadAttachment: suspend (String) -> app.zemote.protocol.AttachmentData? = remember {
        { ref ->
            val r = repoState.value
            val sid = activeIdState.value
            if (r == null || sid == null) null
            else runCatching { r.attachmentRead(sid, ref) }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // 顶栏对齐官方 mobileShell：44dp 高、底色 --color-header、底部 1px 分隔线，
        // 内容 = 返回 + 固定标题「任务会话」+ 主题菜单（palette）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(app.zemote.ui.theme.headerColor())
                .height(44.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back_home),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                stringResource(R.string.sessions_title),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ThemeMenuButton(themeManager)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        // 任务标签行：官方在标题栏与消息流之间展示当前任务（文件夹图标 + 任务名 + 面板开关）
        // 标题数据来自 sessions-index 推送，抽成独立组件避免整页跟着重组。
        TaskTabRow(
            repo = repo,
            activeId = activeId,
            enabled = repo != null && error == null,
            onTogglePanel = { showTaskPanel = !showTaskPanel },
        )

        val errorMessage = error
        if (errorMessage != null) {
            CenterHint(
                icon = { Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp)) },
                title = stringResource(R.string.cannot_open_chat),
                body = errorMessage,
            )
        } else if (repo == null) {
            // 会话通道建立中：快速进入时等待设备就绪/bridge 打开，绝不留白屏
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.opening_session),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            MessageTimeline(
                repo = repo!!,
                workspaceKey = workspaceKey,
                sessionId = sessionId,
                activeId = activeId,
                listState = listState,
                autoFollow = autoFollow,
                working = working,
                historyState = historyState,
                loadAttachment = loadAttachment,
                onOpenSubagent = onOpenSubagent,
                scope = scope,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            if (!readOnly) {
                // 发送失败提示：服务端拒绝时输入框已恢复，这里给出原因
                sendError?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                // 发送区整体下沉到 ComposerSection：它内部才订阅 config / usage /
                // modelOptions / queueItems 等高频状态。这些 flow 原本订阅在 ChatScreen
                // 顶层（见上方注释），流式期间会把整页拖进高频重组。
                ComposerSection(
                    repo = repo,
                    input = input,
                    onInputChange = { input = it },
                    working = working,
                    enabled = repo != null && error == null,
                    activeId = activeId,
                    pendingFiles = pendingFiles,
                    uploadStatus = uploadStatus,
                    onRemoveFile = { f -> pendingFiles = pendingFiles - f },
                    onAttach = { pickFiles.launch(arrayOf("*/*")) },
                    onSend = { queued ->
                        val text = input
                        val files = pendingFiles
                        input = ""
                        autoFollow = true
                        scope.launch {
                            runCatching {
                                val repo0 = repo ?: return@launch
                                var target = activeId
                                // 官方语义：只有 accepted/duplicate/noop 才算发出去；
                                // 其余状态要恢复输入框并提示，否则消息会"凭空消失"
                                var outcome: app.zemote.protocol.SendOutcome? = null
                                if (files.isNotEmpty()) {
                                    // 官方路径：附件需先有 sessionId 才能上传 →
                                    // createSession → attachmentPut → sendText(attachments)
                                    if (target == null) {
                                        target = repo0.createSession()
                                            ?: throw IllegalStateException(context.getString(R.string.create_session_failed))
                                    }
                                    val descriptors = mutableListOf<Map<String, Any?>>()
                                    files.forEachIndexed { i, f ->
                                        uploadStatus = context.getString(R.string.uploading_files, i + 1, files.size)
                                        val up = repo0.attachmentPut(target, f.name, f.mime, f.bytes) { p ->
                                            uploadStatus = context.getString(R.string.uploading_progress, i + 1, files.size, (p * 100).toInt())
                                        }
                                        if (up.ref.isNullOrBlank()) throw IllegalStateException(context.getString(R.string.attach_failed, f.name))
                                        descriptors.add(mapOf(
                                            "ref" to up.ref,
                                            "fileName" to up.fileName,
                                            "mime" to up.mime,
                                            "bytes" to up.bytes,
                                        ))
                                    }
                                    uploadStatus = null
                                    pendingFiles = emptyList()
                                    outcome = repo0.sendText(text, target, attachments = descriptors)
                                } else {
                                    // AI 回复中 → 官方 queue 语义（排队）；空闲 → startNow。
                                    // 后续更新完全由订阅帧（row.appended / row.delta）推送，不做轮询
                                    // 发送时若 target 为 null，sendText 会先 createSession 再发送
                                    outcome = repo0.sendText(
                                        text,
                                        target,
                                        requestedDelivery = if (queued) "queue" else "startNow",
                                    )
                                }
                                (outcome as? app.zemote.protocol.SendOutcome.Accepted)?.sessionId?.let { target = it }
                                if (outcome is app.zemote.protocol.SendOutcome.Rejected) {
                                    throw IllegalStateException(
                                        context.getString(
                                            R.string.send_rejected,
                                            outcome.reasonCode ?: outcome.message ?: "",
                                        ),
                                    )
                                }
                            }.onFailure {
                                // 失败即恢复输入内容与待发附件，并提示原因
                                input = text
                                pendingFiles = files
                                uploadStatus = null
                                sendError = it.message ?: context.getString(R.string.send_rejected, "")
                            }
                        }
                    },
                    onStop = {
                        scope.launch { runCatching { repo?.stop(activeId) } }
                    },
                )
            }
        }

        // 权限审批 / 用户输入弹窗：出现新的待响应请求时自动弹出
        InteractionDialogHost(repo)

        // 任务面板（全屏覆盖）：不可见时完全不订阅后台状态
        TaskPanelHost(
            repo = repo,
            visible = showTaskPanel,
            onDismiss = { showTaskPanel = false },
        )
    }
}

// ────────────────────────── 顶栏组件（独立重组域） ──────────────────────────

/**
 * 任务标签行：官方 mobileShell 在标题栏下方的一行 —— 文件夹图标 + 当前任务名 + 面板开关。
 * 任务名订阅 sessions-index（桌面端重命名实时更新），抽成独立组件把重组限制在这一行。
 */
@Composable
private fun TaskTabRow(
    repo: app.zemote.protocol.ConversationV4Session?,
    activeId: String?,
    enabled: Boolean,
    onTogglePanel: () -> Unit,
) {
    val sessionEntries by (repo?.sessionEntries?.collectAsState()
        ?: remember { mutableStateOf(emptyList<app.zemote.protocol.SessionEntry>()) })
    val pendingInteractions by (repo?.pendingInteractions?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })
    val backgroundWorks by (repo?.backgroundWorks?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })
    val sessionTitle = sessionEntries
        .firstOrNull { it.sessionId == activeId }
        ?.title?.trim()?.ifBlank { null }
    val hasPending = pendingInteractions.isNotEmpty()
    val hasBackground = backgroundWorks.any { it.status == "running" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(app.zemote.ui.theme.headerColor())
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = sessionTitle ?: stringResource(R.string.new_chat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(
                onClick = onTogglePanel,
                modifier = Modifier.size(34.dp),
                enabled = enabled,
            ) {
                Icon(
                    Icons.Rounded.ViewSidebar,
                    contentDescription = stringResource(R.string.tasks_panel),
                    tint = if (hasPending || hasBackground) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (hasPending) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(7.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                )
            }
        }
    }
}

/**
 * 主题菜单：官方 mobileShell 头部右侧的 palette 图标，点开可选系统默认 / 浅色 / 深色。
 * themeManager 为空（如子智能体只读页）时不显示。
 */
@Composable
fun ThemeMenuButton(themeManager: ThemeManager?) {
    if (themeManager == null) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Rounded.Palette,
                contentDescription = stringResource(R.string.theme_menu),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val state by themeManager.state.collectAsState()
            val modes = listOf(
                ThemeManager.ThemeMode.FOLLOW_SYSTEM to R.string.theme_system,
                ThemeManager.ThemeMode.LIGHT to R.string.theme_light,
                ThemeManager.ThemeMode.DARK to R.string.theme_dark,
            )
            for ((mode, label) in modes) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                            if (state.mode == mode) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        }
                    },
                    onClick = { open = false; themeManager.setMode(mode) },
                )
            }
        }
    }
}

// ────────────────────────── 交互响应 / 任务面板（独立重组域） ──────────────────────────

/**
 * 权限审批 / 用户输入弹窗宿主。
 *
 * 修复的缺陷：旧实现用一个 `selectedInteraction` 状态驱动弹窗，但**全仓库没有任何
 * 地方把它置为非 null** —— 这个弹窗永远不会出现。桌面端发起权限审批时，界面只是
 * 在任务面板按钮上多一个小红点；用户不主动点开任务面板就完全看不到，而 Agent 会
 * 一直阻塞等待答复。现在改为订阅 `pendingInteractions`，出现新的待响应请求就自动弹出，
 * 每个 `requestId` 只自动弹一次（用户手动关掉后不会再弹回来）。
 */
@Composable
private fun InteractionDialogHost(repo: app.zemote.protocol.ConversationV4Session?) {
    val interactions by (repo?.pendingInteractions?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })
    val scope = rememberCoroutineScope()
    // 已自动弹过的 requestId（纯记账，不参与组合观察）
    val shown = remember { mutableSetOf<String>() }
    var current by remember { mutableStateOf<PendingInteraction?>(null) }

    LaunchedEffect(interactions) {
        if (current != null) return@LaunchedEffect
        val next = interactions.firstOrNull { it.requestId !in shown } ?: return@LaunchedEffect
        if (shown.size > 256) shown.clear()
        shown.add(next.requestId)
        current = next
    }

    val inter = current ?: return
    InteractionDialog(
        interaction = inter,
        onRespond = { optionId, freeText, action ->
            current = null
            scope.launch { repo?.respondInteraction(inter.requestId, optionId, freeText, action) }
        },
        onDismiss = { current = null },
    )
}

/**
 * 任务面板宿主。
 * `visible` 为 false 时直接返回、**不建立任何订阅** —— 否则后台任务状态的频繁更新
 * 会持续重组聊天页。
 */
@Composable
private fun TaskPanelHost(
    repo: app.zemote.protocol.ConversationV4Session?,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val scope = rememberCoroutineScope()
    val interactions by (repo?.pendingInteractions?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })
    val works by (repo?.backgroundWorks?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })

    TaskPanel(
        interactions = interactions,
        works = works,
        onRespond = { inter, optId, freeText, action ->
            scope.launch {
                repo?.respondInteraction(inter.requestId, optId, freeText, action)
                onDismiss()
            }
        },
        onCancel = { workId ->
            scope.launch { repo?.cancelBackgroundWork(workId) }
        },
        onDismiss = onDismiss,
    )
}

// ────────────────────────── 发送区（独立重组域） ──────────────────────────

/**
 * 发送区：队列卡片 + 待发附件条 + 输入栏。
 *
 * 单独抽出来的唯一目的是**限制重组范围**：`convConfig` / `usage` / `modelOptions` /
 * `stopWorkId` / `followupMode` / `queueItems` / `autoDrain` 只在这里订阅。
 * 这些状态在流式输出期间更新极频繁（尤其 `usage` 随每个 `state.updated` 补丁，
 * 约 16 次/秒），订阅在 ChatScreen 顶层会让整个聊天页连同 MessageTimeline、
 * 所有可见消息一起重组 —— 这正是「UI 卡顿、交互响应迟缓」的主要根因。
 */
@Composable
private fun ComposerSection(
    repo: app.zemote.protocol.ConversationV4Session?,
    input: String,
    onInputChange: (String) -> Unit,
    working: Boolean,
    enabled: Boolean,
    activeId: String?,
    pendingFiles: List<PendingFile>,
    uploadStatus: String?,
    onRemoveFile: (PendingFile) -> Unit,
    onAttach: () -> Unit,
    onSend: (queued: Boolean) -> Unit,
    onStop: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val convConfig by (repo?.convConfig?.collectAsState() ?: remember { mutableStateOf(null) })
    val usage by (repo?.usage?.collectAsState() ?: remember { mutableStateOf(null) })
    val modelOptions by (repo?.modelOptions?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val stopWorkId by (repo?.stopWorkId?.collectAsState() ?: remember { mutableStateOf(null) })
    val followupMode by (repo?.followupMode?.collectAsState() ?: remember { mutableStateOf(null) })
    val queueItems by (repo?.queueItems?.collectAsState()
        ?: remember { mutableStateOf(emptyList<app.zemote.protocol.QueueItem>()) })
    val autoDrain by (repo?.autoDrain?.collectAsState() ?: remember { mutableStateOf(true) })

    // 排队消息卡片：AI 工作中发送的内容进入队列，可立即发送/编辑/删除（官方队列语义）
    QueueBar(
        items = queueItems,
        autoDrain = autoDrain,
        onSendNow = { id -> scope.launch { runCatching { repo?.sendQueuedNow(id) } } },
        onEdit = { id, text -> scope.launch { runCatching { repo?.editQueueItem(id, text) } } },
        onDelete = { id -> scope.launch { runCatching { repo?.deleteQueueItem(id) } } },
        onToggleAutoDrain = { on -> scope.launch { runCatching { repo?.setAutoDrain(on) } } },
        onReorder = { ids -> scope.launch { runCatching { repo?.reorderQueueItem(ids) } } },
    )

    if (pendingFiles.isNotEmpty() || uploadStatus != null) {
        PendingFilesBar(
            files = pendingFiles,
            status = uploadStatus,
            onRemove = onRemoveFile,
        )
    }

    // 是否已有历史消息（决定占位文案）：不订阅 rows，只在重组时读一次当前值。
    // ComposerSection 本就随 usage 等高频状态频繁重组，这里读到的值足够新鲜。
    val hasHistory = repo?.rows?.value?.isNotEmpty() == true

    ComposerBar(
        text = input,
        onTextChange = onInputChange,
        working = working,
        enabled = enabled,
        hasHistory = hasHistory,
        config = convConfig,
        usage = usage,
        modelOptions = modelOptions,
        stopWorkId = stopWorkId,
        followupMode = followupMode,
        onAttach = onAttach,
        onThoughtSelect = { level ->
            scope.launch { runCatching { repo?.setThought(level) } }
        },
        onModelSelect = { provider, model ->
            scope.launch { runCatching { repo?.setModel(provider, model) } }
        },
        onSend = onSend,
        onStop = onStop,
    )
}

// ────────────────────────── 消息时间线（独立重组域） ──────────────────────────

/**
 * 消息时间线。
 *
 * 单独抽成一个 composable 是为了**把重组范围限制在时间线内部**：`repo.rows` 只在这里订阅，
 * 流式输出时不会带着顶栏、输入栏、队列卡片一起重组（旧实现整页订阅 rows，
 * 每个 token 都会让整页重组一遍，这是卡顿的主因之一）。
 */
@Composable
private fun MessageTimeline(
    repo: app.zemote.protocol.ConversationV4Session,
    workspaceKey: String,
    sessionId: String?,
    activeId: String?,
    listState: LazyListState,
    autoFollow: Boolean,
    working: Boolean,
    historyState: app.zemote.protocol.HistoryState,
    loadAttachment: suspend (String) -> app.zemote.protocol.AttachmentData?,
    onOpenSubagent: (String, String, String) -> Unit,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val rows by repo.rows.collectAsState()
    // hasMore 是可观察的 StateFlow（见 ConversationV4Session.hasMoreFlow）。
    // 旧实现里 `hasMore` 只是普通 var，`repo.hasOlderHistory` 在组合中读取它却
    // 不产生订阅 —— 服务端翻页结果变化时不会触发重组，于是出现「明明还有更早
    // 历史，顶部按钮却不出现」这种与预期不符的行为。
    val hasMoreFlag by repo.hasMoreFlow.collectAsState()
    val hasOlder = remember(rows, hasMoreFlag) { repo.hasOlderHistory }

    // 行内容变化时重建展示项；LazyColumn 用稳定 key，未变化的条目会被自动跳过。
    // 上一次的结果放在普通引用里而不是 Compose state —— 组合期间写 state 会引入
    // 额外的重组轮次，这里只需要它作为「实例复用池」，不参与订阅。
    val prevItemsRef = remember {
        java.util.concurrent.atomic.AtomicReference<List<DisplayItem>>(emptyList())
    }
    val displayItems = remember(rows) {
        buildDisplayItems(rows, prevItemsRef.get()).also { prevItemsRef.set(it) }
    }

    // 是否显示「回到最新消息」按钮：用户上翻时出现
    var showScrollToBottom by remember { mutableStateOf(false) }
    // 向上加载更多历史的状态
    var isLoadingOlder by remember { mutableStateOf(false) }
    var loadOlderError by remember { mutableStateOf<String?>(null) }

    val isFirstItemVisible by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && info.visibleItemsInfo.firstOrNull()?.index == 0
        }
    }
    val isLastItemVisible by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
        }
    }

    // 当最后一项不可见时（用户上翻了），显示回到最新消息按钮
    LaunchedEffect(isLastItemVisible, displayItems.isNotEmpty()) {
        showScrollToBottom = !isLastItemVisible && displayItems.isNotEmpty()
    }

    // ── 列表条目结构：DSL 与锚点索引共用同一组条件，避免两处判断不一致导致滚动位置算错 ──
    val showLoading = rows.isEmpty() && historyState == app.zemote.protocol.HistoryState.LOADING
    val showFailed = rows.isEmpty() && historyState == app.zemote.protocol.HistoryState.FAILED
    val showEmpty = rows.isEmpty() && historyState == app.zemote.protocol.HistoryState.EMPTY
    // 注意：不把 isLoadingOlder 放进条件里 —— 加载中让按钮原地变转圈，
    // 否则条目会在加载开始/结束时被移除又插入，列表整体跳动一下。
    val showOlderButton = isFirstItemVisible && activeId != null &&
        loadOlderError == null && (hasOlder || isLoadingOlder)
    val showOlderError = loadOlderError != null

    /**
     * 末尾锚点项的索引，从数据推算而不是读 `layoutInfo`。
     * 布局在组合之后才更新，用 `layoutInfo.totalItemsCount - 1` 取会滞后一帧，
     * 新消息到达时滚动会差一条。顺序：可选状态条目 → displayItems → 可选 working → 锚点。
     */
    val anchorIndex = listOf(showLoading, showFailed, showEmpty, showOlderButton, showOlderError)
        .count { it } + displayItems.size + (if (working) 1 else 0)

    // 提到 items 外面：以前每个条目、每次重组都会新建一个 lambda，导致捕获它的
    // FadeInContainer / TimelineRow 参数恒不相等，永远无法跳过重组。
    val openSub: (ConvRow) -> Unit = remember(workspaceKey, sessionId, onOpenSubagent) {
        { row -> row.childSessionId?.let { cid -> onOpenSubagent(workspaceKey, cid, sessionId ?: "") } }
    }

    // 到达列表顶部时自动加载更早的历史（服务端确实还有更早的行才触发）。
    // 把 hasOlder 也作为 key：翻页结果变化时能重新触发（Boolean 相等比较，
    // 值没变就不会重启 effect，不会造成重复加载）。
    LaunchedEffect(isFirstItemVisible, displayItems.isNotEmpty(), hasOlder) {
        if (!isFirstItemVisible || isLoadingOlder) return@LaunchedEffect
        val sid = activeId ?: return@LaunchedEffect
        if (!hasOlder) return@LaunchedEffect
        isLoadingOlder = true
        loadOlderError = null
        // 直接在本 effect 里挂起，加载期间 isLoadingOlder 保持 true，
        // 避免按钮和滚动触发在两帧内重复发起同一次翻页。
        try {
            repo.loadOlderMessages(sid)
        } catch (t: Throwable) {
            loadOlderError = t.message ?: t.javaClass.simpleName
        } finally {
            isLoadingOlder = false
        }
    }

    // 新消息 / 条目数变化：贴底。
    // 列表末尾放了一个 0 高度的锚点项，"滚到最后一个 index"就等价于"贴到底部"，
    // 这样最后一条很长时也不会只把它的顶部露出来。
    var lastScrollTarget by remember { mutableStateOf(-1) }
    LaunchedEffect(anchorIndex, autoFollow) {
        if (!autoFollow || displayItems.isEmpty()) return@LaunchedEffect
        // 只有当前已经贴底时才继续跟随，防止用户上翻时被强制弹回
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (lastVisible < info.totalItemsCount - 1) return@LaunchedEffect
        if (anchorIndex != lastScrollTarget) {
            lastScrollTarget = anchorIndex
            listState.scrollToItem(anchorIndex)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // 官方移动端消息流：水平 16dp、条目间距紧凑，正文的呼吸感由条目自身 padding 提供
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 90.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.loading_chat),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (showFailed) {
                // 订阅/拉取都失败时给出明确反馈和重试入口，而不是一片空白
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 90.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.fetch_sessions_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { scope.launch { repo.retryHistory(activeId) } }) {
                            Text(stringResource(R.string.retry_connect))
                        }
                    }
                }
            } else if (showEmpty) {
                item {
                    Surface(
                        color = app.zemote.ui.theme.cardContainerColor(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.session_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            // 加载更多历史按钮：还有更早的行时显示在列表顶部
            if (showOlderButton) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isLoadingOlder) return@clickable
                                isLoadingOlder = true
                                loadOlderError = null
                                scope.launch {
                                    try {
                                        repo.loadOlderMessages(activeId)
                                    } catch (t: Throwable) {
                                        loadOlderError = t.message ?: t.javaClass.simpleName
                                    } finally {
                                        isLoadingOlder = false
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoadingOlder) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = stringResource(R.string.load_older_messages),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.load_older_messages),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (showOlderError) {
                item {
                    Text(
                        loadOlderError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            itemsIndexed(displayItems, key = { _, item -> item.key }) { index, item ->
                when (item) {
                    is DisplayItem.Single -> {
                        // 思考行的「持续了 N 秒」用下一行的时间戳推算；只有思考行才扫描
                        val nextIssuedAt = if (item.row.kind == ConvKinds.REASONING) {
                            displayItems.drop(index + 1)
                                .firstOrNull { it is DisplayItem.Single && it.row.issuedAt != null }
                                ?.let { (it as DisplayItem.Single).row.issuedAt }
                        } else null
                        if (item.row.kind == ConvKinds.USER_INPUT) {
                            TimelineRow(item.row, loadAttachment, nextIssuedAt, onOpenSubagent = openSub)
                        } else {
                            // AI 产生的内容淡入，更灵动
                            FadeInContainer(item.key) {
                                TimelineRow(item.row, loadAttachment, nextIssuedAt, onOpenSubagent = openSub)
                            }
                        }
                    }
                }
            }

            if (working) {
                // 官方移动端：生成中只在末尾放一个小 spinner，不占文案
                item {
                    CircularProgressIndicator(
                        color = app.zemote.ui.theme.subtlestColor(),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    )
                }
            }

            // 末尾锚点：滚到它 = 贴到底部（见上面的自动跟随逻辑）
            item(key = "bottom-anchor") { Spacer(modifier = Modifier.height(0.dp)) }
        }

        // 右下角浮动按钮：官方样式的白色小胶囊（回到最新消息），用户上翻时出现
        AnimatedVisibility(
            visible = showScrollToBottom,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 10.dp),
        ) {
            Surface(
                onClick = {
                    showScrollToBottom = false
                    scope.launch { listState.animateScrollToItem(anchorIndex) }
                },
                shape = CircleShape,
                color = app.zemote.ui.theme.cardContainerColor(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.scroll_to_bottom),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ────────────────────────── 时间线渲染 ──────────────────────────

/**
 * 单条时间线行。对齐官方文档流：
 * 助手正文通栏、用户右对齐小气泡、思考/工具各占一行灰色摘要（无卡片边框）。
 */
@Composable
private fun TimelineRow(
    row: ConvRow,
    loadAttachment: suspend (String) -> app.zemote.protocol.AttachmentData?,
    nextIssuedAt: Long? = null,
    onOpenSubagent: (ConvRow) -> Unit = {},
) {
    when (row.kind) {
        ConvKinds.USER_INPUT -> Box(modifier = Modifier.padding(vertical = 4.dp)) {
            UserBubble(row, loadAttachment)
        }
        ConvKinds.ASSISTANT_TEXT -> if (row.text.isNotBlank()) {
            // 官方助手正文：通栏大字（16sp）、前后留白明显大于工具行
            app.zemote.ui.components.MarkdownText(
                markdown = row.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
        }
        ConvKinds.REASONING -> ReasoningRow(row, nextIssuedAt)
        ConvKinds.TOOL_CALL -> ToolSummaryRow(row, onOpenSubagent)
        ConvKinds.SUBAGENT -> if (row.summaryText.isNotBlank() || row.text.isNotBlank()) {
            ToolSummaryRow(
                row.copy(toolName = "subagent", inputText = row.summaryText.ifBlank { row.text }),
                onOpenSubagent,
            )
        }
        // 图片类消息：占位卡片展示，绝不出现加载失败的破图
        ConvKinds.IMAGE, "screenshot" -> ImagePlaceholder(row)
        else -> Unit
    }
}

/** 图片占位卡片（后续接入图片传输后在此渲染真实内容） */
@Composable
private fun ImagePlaceholder(row: ConvRow) {
    Surface(
        color = app.zemote.ui.theme.cardContainerColor(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                row.text.ifBlank { stringResource(R.string.image_message) },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UserBubble(row: ConvRow, loadAttachment: suspend (String) -> app.zemote.protocol.AttachmentData?) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        if (row.text.isNotBlank() || row.inputText.isNotBlank() || row.attachments.isEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // 官方用户消息：右对齐中性气泡（ml-auto），rounded-lg + --color-secondary
                // （浅 #e6e6e6 / 深 #363636），内边距 px-4 py-3，文字 text-ui-base
                Surface(
                    color = app.zemote.ui.theme.userBubbleColor(),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .padding(vertical = 2.dp),
                ) {
                    Text(
                        row.text.ifBlank { row.inputText },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
        row.attachments.forEach { att ->
            Spacer(modifier = Modifier.height(6.dp))
            if (att["mime"]?.startsWith("image/") == true) {
                ImageAttachmentView(
                    ref = att["ref"].orEmpty(),
                    fileName = att["fileName"] ?: stringResource(R.string.image),
                    loadAttachment = loadAttachment,
                )
            } else {
                AttachmentChip(fileName = att["fileName"] ?: stringResource(R.string.attach))
            }
        }
    }
}

/** 消息内的文件附件 chip */
@Composable
private fun AttachmentChip(fileName: String) {
    Surface(
        color = app.zemote.ui.theme.cardContainerColor(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                fileName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }
    }
}

/** 消息内的图片附件：按 ref 经 attachmentReadV4 拉取后渲染，绝不破图 */
@Composable
private fun ImageAttachmentView(
    ref: String,
    fileName: String,
    loadAttachment: suspend (String) -> app.zemote.protocol.AttachmentData?,
) {
    if (ref.isEmpty()) {
        AttachmentChip(fileName)
        return
    }
    var bitmap by remember(ref) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(ref) { mutableStateOf(false) }
    // 组件销毁或 ref 变化时回收旧 bitmap，防止内存泄漏
    DisposableEffect(ref) {
        onDispose { bitmap?.recycle(); bitmap = null }
    }
    DisposableEffect(Unit) {
        // 用户离开对话页时（ref 未变）也回收，防止整屏图片 bitmap 滞留在内存
        onDispose { bitmap?.recycle(); bitmap = null }
    }
    LaunchedEffect(ref) {
        val oldBmp = bitmap
        val data = runCatching { loadAttachment(ref) }.getOrNull()
        val bmp = data?.bytes?.let { bytes ->
            runCatching {
                // 大图降采样解码（最长边 ~2048px），防止整图 bitmap 爆内存
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2048) sample *= 2
                BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            }.getOrNull()
        }
        if (oldBmp != null && oldBmp != bmp) oldBmp.recycle()
        if (bmp != null) bitmap = bmp else failed = true
    }
    Surface(
        color = app.zemote.ui.theme.cardContainerColor(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = fileName,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .padding(4.dp),
            )
            failed -> Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    fileName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
            }
            else -> Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.image_loading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ────────────────────────── 排队消息 ──────────────────────────

/** 排队消息卡片：对齐官方 web 队列展示（立即发送 / 编辑 / 删除 / 拖动排序 + 自动发送开关） */
@Composable
private fun QueueBar(
    items: List<app.zemote.protocol.QueueItem>,
    autoDrain: Boolean,
    onSendNow: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleAutoDrain: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    if (items.isEmpty()) return
    var editTarget by remember { mutableStateOf<app.zemote.protocol.QueueItem?>(null) }
    var deleteTarget by remember { mutableStateOf<app.zemote.protocol.QueueItem?>(null) }

    // 本地顺序：拖动过程即时重排（乐观更新），松手后发 reorderQueueItem 由服务端确认
    var order by remember(items) { mutableStateOf(items) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeight = 34.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }

    Surface(
        color = app.zemote.ui.theme.cardContainerColor(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.queue_title, order.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (autoDrain) stringResource(R.string.auto_send_on) else stringResource(R.string.auto_send_off),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onToggleAutoDrain(!autoDrain) },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            order.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .zIndex(if (dragId == item.queueItemId) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragId == item.queueItemId) dragOffsetY else 0f
                        }
                        .pointerInput(item.queueItemId, order.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragId = item.queueItemId
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetY += drag.y
                                    var i = order.indexOfFirst { it.queueItemId == dragId }
                                    if (i < 0) return@detectDragGesturesAfterLongPress
                                    // 拖过相邻行高度就交换位置
                                    while (dragOffsetY > rowHeightPx && i < order.lastIndex) {
                                        order = order.toMutableList().apply {
                                            add(i + 1, removeAt(i))
                                        }
                                        i++
                                        dragOffsetY -= rowHeightPx
                                    }
                                    while (dragOffsetY < -rowHeightPx && i > 0) {
                                        order = order.toMutableList().apply {
                                            add(i - 1, removeAt(i))
                                        }
                                        i--
                                        dragOffsetY += rowHeightPx
                                    }
                                },
                                onDragEnd = {
                                    onReorder(order.map { it.queueItemId })
                                    dragId = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    dragId = null
                                    dragOffsetY = 0f
                                },
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.DragIndicator,
                        contentDescription = stringResource(R.string.drag_reorder),
                        tint = if (dragId == item.queueItemId) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.queue_item_index, index + 1, item.text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onSendNow(item.queueItemId) }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.send_now),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = { editTarget = item }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    IconButton(onClick = { deleteTarget = item }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }

    editTarget?.let { target ->
        var editText by remember(target.queueItemId) { mutableStateOf(target.text) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(stringResource(R.string.queue_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    maxLines = 4,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = editText.trim()
                    editTarget = null
                    if (t.isNotEmpty()) onEdit(target.queueItemId, t)
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.queue_delete_title)) },
            text = { Text(target.text, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    onDelete(target.queueItemId)
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

// ────────────────────────── 附件选择 ──────────────────────────

/** 待发送的附件（已在本地读入内存） */
private data class PendingFile(val name: String, val mime: String, val bytes: ByteArray)

/** 待发附件条：发送栏上方展示已选文件/上传进度 */
@Composable
private fun PendingFilesBar(
    files: List<PendingFile>,
    status: String?,
    onRemove: (PendingFile) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status != null) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        files.forEach { f ->
            Surface(
                color = app.zemote.ui.theme.cardContainerColor(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        f.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )
                    IconButton(onClick = { onRemove(f) }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.remove),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

/** SAF 查询文件显示名 */
private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }

/** 扩展名 → MIME 兜底（contentResolver.getType 为空时使用，对齐官方猜测表） */
private fun guessMime(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (ext) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        "txt", "md", "log" -> "text/plain"
        "json" -> "application/json"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }
}

/**
 * 思考行：对齐官方 chat.reasoning —— 单行「🧠 思考 · 持续了 N 秒」，
 * 全部最浅灰（--color-foreground-subtlest），无边框无卡片；
 * 流式中显示「正在思考 · <滚动预览>」。点击展开完整思考文本。
 */
@Composable
private fun ReasoningRow(row: ConvRow, nextIssuedAt: Long?) {
    val streaming = row.state == null || row.state !in app.zemote.protocol.COMPLETE_STATES
    var expanded by remember(row.rowId) { mutableStateOf(false) }
    var userToggled by remember(row.rowId) { mutableStateOf(false) }
    LaunchedEffect(streaming) {
        if (!userToggled) expanded = false
    }
    val subtlest = app.zemote.ui.theme.subtlestColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                userToggled = true
                expanded = !expanded
            }
            .padding(vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = subtlest,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (streaming) {
                Text(
                    stringResource(R.string.reasoning_thinking),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (row.text.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "· " + row.text.lineSequence().lastOrNull { it.isNotBlank() }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtlest,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                // 持续时长：issuedAt 到下一行 issuedAt 的差值；拿不到时按官方文案说「持续了几秒」
                val durationText = if (row.issuedAt != null && nextIssuedAt != null) {
                    val seconds = ((nextIssuedAt - row.issuedAt) / 1000L).coerceAtLeast(0L)
                    if (seconds > 5) stringResource(R.string.reasoning_seconds, seconds)
                    else stringResource(R.string.reasoning_few_seconds)
                } else {
                    stringResource(R.string.reasoning_few_seconds)
                }
                Text(
                    stringResource(R.string.reasoning_thought),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = subtlest,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("·", style = MaterialTheme.typography.bodyMedium, color = subtlest)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtlest,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (expanded) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = subtlest,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        // 展开态：完整思考内容，直切无动画
        if (expanded && row.text.isNotBlank()) {
            Text(
                row.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 4.dp),
            )
        }
    }
}

/**
 * 工具行：对齐官方 ToolLayout 单行摘要 ——
 * [图标] [类型标签] [来源徽章] · [摘要] [+N] [运行 spinner / 失败]，
 * 无边框无卡片，点击展开原始输出。
 */
@Composable
private fun ToolSummaryRow(row: ConvRow, onOpenSubagent: (ConvRow) -> Unit = {}) {
    var expanded by remember(row.rowId) { mutableStateOf(false) }
    val ctx = LocalContext.current
    val running = row.toolStatus == null || row.toolStatus == "running" || row.toolStatus == "pending"
    val failed = row.toolStatus == "error"
    val subtlest = app.zemote.ui.theme.subtlestColor()

    // 摘要与标签的解析有 JSON 开销，key 带上内容避免每帧重解析
    val (kindLabel, summaryText) = remember(row.rowId, row.toolName, row.inputText, row.summaryText) {
        val name = row.toolName?.lowercase()
        val summary = summarizeToolInput(name, row.inputText.ifBlank { row.summaryText })
            .ifBlank { row.text }
        toolKindLabel(ctx, name, running) to summary
    }
    // MCP 工具（toolName 形如 mcp__server__tool）拆出服务器名做来源徽章
    val sourceLabel = row.toolName?.takeIf { it.startsWith("mcp__", ignoreCase = true) }
        ?.split("__")?.getOrNull(1)?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                toolKindIcon(row.toolName?.lowercase()),
                contentDescription = null,
                tint = subtlest,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                kindLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (sourceLabel != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = app.zemote.ui.theme.backgroundAltColor(),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtlest,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (summaryText.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtlest,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtlest,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            // 官方 diff 绿：浅 green-600 / 深 green-500
            if (row.additions != null && row.additions > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                val diffGreen = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                    app.zemote.ui.theme.DiffAdded
                } else {
                    app.zemote.ui.theme.DiffAddedDark
                }
                Text(
                    "+${row.additions}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = diffGreen,
                )
            }
            if (running) {
                Spacer(modifier = Modifier.width(6.dp))
                CircularProgressIndicator(
                    color = subtlest,
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(12.dp),
                )
            } else if (failed) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.tool_failed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // 子智能体入口
            if (row.childSessionId != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.subagent_open),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clickable { onOpenSubagent(row) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        // 展开态：原始输出（等宽小字），直切无动画
        if (expanded && row.outputText.isNotBlank()) {
            Text(
                row.outputText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 24.dp, top = 4.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

/** 官方工具类型 → 图标 */
private fun toolKindIcon(tool: String?): androidx.compose.ui.graphics.vector.ImageVector = when (tool) {
    "terminal", "bash", "run_command", "execute" -> Icons.Rounded.Terminal
    "read", "grep", "glob", "search", "websearch", "web_fetch" -> Icons.Rounded.Search
    "edit", "multiedit", "notebookedit", "write" -> Icons.Rounded.Edit
    "delete", "rm", "remove" -> Icons.Rounded.Delete
    "task", "subagent" -> Icons.Rounded.SmartToy
    "skill", "skills" -> Icons.Rounded.AutoAwesome
    "todo", "todowrite", "todos" -> Icons.Rounded.Checklist
    else -> if (tool?.startsWith("mcp__") == true) Icons.Rounded.Extension else Icons.AutoMirrored.Rounded.InsertDriveFile
}

/** 官方工具类型 → 标签（运行中换成「正在执行/正在读取…」等运行态文案） */
private fun toolKindLabel(ctx: android.content.Context, tool: String?, running: Boolean): String = when (tool) {
    "terminal", "bash", "run_command", "execute" ->
        if (running) ctx.getString(R.string.tool_running) else ctx.getString(R.string.tool_kind_terminal)
    "read" ->
        if (running) ctx.getString(R.string.tool_reading) else ctx.getString(R.string.tool_kind_read)
    "grep", "glob", "search", "websearch", "web_fetch" ->
        if (running) ctx.getString(R.string.tool_searching) else ctx.getString(R.string.tool_kind_search)
    "write" ->
        if (running) ctx.getString(R.string.tool_writing) else ctx.getString(R.string.tool_kind_write)
    "edit", "multiedit", "notebookedit" ->
        if (running) ctx.getString(R.string.tool_editing) else ctx.getString(R.string.tool_kind_edit)
    "delete", "rm", "remove" ->
        if (running) ctx.getString(R.string.tool_deleting) else ctx.getString(R.string.tool_kind_delete)
    "task", "subagent" -> ctx.getString(R.string.tool_kind_subagent)
    "skill", "skills" -> ctx.getString(R.string.tool_kind_skill)
    "todo", "todowrite", "todos" -> ctx.getString(R.string.tool_kind_todo)
    else -> if (tool?.startsWith("mcp__") == true) ctx.getString(R.string.tool_kind_mcp)
    else ctx.getString(R.string.tool_kind_generic)
}

/** 时间线显示项：普通行单条展示 */
private sealed interface DisplayItem {
    val key: String

    data class Single(val row: ConvRow) : DisplayItem {
        override val key get() = "r-${row.rowId}"
    }
}

/**
 * 把行列表映射成展示项（每行独立展示，对齐官方文档流）。
 *
 * `prev` 是上一次的结果：流式输出期间每 60ms 就会重跑一次，而真正变化的通常只有最后一行。
 * 这里对每个槽位做「实例比对」，命中就复用旧对象，避免每次都重建整张列表 —— 否则
 * 一次长会话每帧要分配上千个 DisplayItem，GC 抖动会直接表现为滚动卡顿。
 *
 * 之所以能安全地按 `===` 比对：ConversationV4 在内容未变时始终复用同一个 ConvRow 实例
 * （见 appendToRow / flushPendingDeltas），只有真正被改写的行才会产生新实例。
 */
private fun buildDisplayItems(rows: List<ConvRow>, prev: List<DisplayItem>): List<DisplayItem> {
    val out = ArrayList<DisplayItem>(if (prev.isEmpty()) 16 else prev.size)
    for (row in rows) {
        val cached = prev.getOrNull(out.size)
        out.add(
            if (cached is DisplayItem.Single && cached.row === row) cached
            else DisplayItem.Single(row)
        )
    }
    return out
}

// ────────────────────────── 交互响应弹窗 ──────────────────────────

/** 权限审批 / 用户输入弹窗 */
@Composable
private fun InteractionDialog(
    interaction: PendingInteraction,
    onRespond: (optionId: String?, freeText: String?, action: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(interaction.requestId) { mutableStateOf("") }
    val ctx = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (interaction.kind == "permission")
                Text(stringResource(R.string.permission_request_title))
            else
                Text(stringResource(R.string.elicitation_request_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (interaction.body.isNotBlank()) {
                    Text(interaction.body, style = MaterialTheme.typography.bodyMedium)
                }
                if (interaction.options.isNotEmpty()) {
                    interaction.options.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRespond(opt.optionId, null, null) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (opt.kind == "deny") Icons.Rounded.Close
                                else Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (opt.kind == "deny") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opt.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (interaction.kind != "permission") {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
            }
        },
        confirmButton = {
            if (interaction.options.isEmpty() && interaction.kind != "permission") {
                TextButton(
                    onClick = { onRespond(null, text.trim(), null) },
                    enabled = text.isNotBlank(),
                ) { Text(stringResource(R.string.elicitation_submit)) }
            }
        },
        dismissButton = {
            if (interaction.options.isEmpty()) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

// ────────────────────────── 任务面板 ──────────────────────────

/** 任务面板：显示待响应交互 + 后台运行中的任务 */
@Composable
private fun TaskPanel(
    interactions: List<PendingInteraction>,
    works: List<BackgroundWork>,
    onRespond: (PendingInteraction, String?, String?, String?) -> Unit,
    onCancel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.tasks_panel), style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 待响应交互
            if (interactions.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.permission_request_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                items(interactions, key = { it.requestId }) { inter ->
                    Surface(
                        color = app.zemote.ui.theme.cardContainerColor(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (inter.body.isNotBlank()) {
                                Text(inter.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (inter.options.isNotEmpty()) {
                                inter.options.forEach { opt ->
                                    val isDeny = opt.kind == "deny"
                                    FilledTonalIconButton(
                                        onClick = { onRespond(inter, opt.optionId, null, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (isDeny) Icons.Rounded.Close else Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = if (isDeny) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(opt.label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            } else if (inter.kind != "permission") {
                                // 用户输入型交互
                                var text by remember(inter.requestId) { mutableStateOf("") }
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    placeholder = { Text(stringResource(R.string.elicitation_request_title)) },
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                                    FilledTonalIconButton(
                                        onClick = { onRespond(inter, null, text.trim(), null) },
                                        enabled = text.isNotBlank(),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(R.string.elicitation_submit))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 后台任务
            val runningWorks = works.filter { it.status == "running" }
            if (runningWorks.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.task_running),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                items(runningWorks, key = { it.workId }) { work ->
                    Surface(
                        color = app.zemote.ui.theme.cardContainerColor(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (work.kind == "subagent") Icons.Rounded.SmartToy
                                else Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                work.title.ifBlank { "${work.kind}" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (work.cancellable) {
                                IconButton(
                                    onClick = { onCancel(work.workId) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Cancel,
                                        contentDescription = stringResource(R.string.task_cancel),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (interactions.isEmpty() && runningWorks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.task_no_work), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** AI 消息淡入容器：首次进入组合时从透明渐变到完全不透明 */
@Composable
private fun FadeInContainer(key: Any?, content: @Composable () -> Unit) {
    var shown by remember(key) { mutableStateOf(false) }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 260),
        label = "aiFadeIn",
    )
    LaunchedEffect(key) { shown = true }
    Box(
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}

/**
 * 从官方 toolCall 的 inputText（JSON）提取摘要：
 * 文件类工具 → 文件名；bash/终端 → 命令描述（不显示文件名）；其余 → 原始一行。
 */
private fun summarizeToolInput(tool: String?, raw: String): String {
    if (raw.isBlank()) return ""
    if (!raw.startsWith("{")) return raw.substringBefore('\n')
    return try {
        val obj = org.json.JSONObject(raw)
        when (tool) {
            "edit", "write", "read", "multiedit", "notebookedit" ->
                fileNameOf(obj.optString("filePath").ifBlank {
                    obj.optString("file_path").ifBlank { obj.optString("notebook_path") }
                }).ifBlank { obj.optString("description") }
            else ->
                obj.optString("description").ifBlank {
                    obj.optString("command").ifBlank {
                        obj.optString("pattern").ifBlank { obj.optString("query").ifBlank { raw } }
                    }
                }
        }
    } catch (_: Exception) {
        raw.substringBefore('\n')
    }
}

private fun fileNameOf(path: String): String =
    path.substringAfterLast('\\').substringAfterLast('/').trim()

// ────────────────────────── 发送栏（官方 composer 布局） ──────────────────────────

/**
 * 发送栏：对齐官方 composer —— 大圆角输入卡（卡底 + 1px 边框），
 * 上输入框（占位文案随状态切换），下控制条：
 * 左：附件「+」；右：上下文% · 模型胶囊 · 推理强度胶囊 · 黑色圆角方块（停止/发送）。
 * 官方发送键为 rounded-lg + --color-brand（浅色黑 / 深色白）实心方块；
 * AI 工作中输入文字时发送键执行官方 enqueue 语义（加入队列），停止键降为描边方块。
 */
@Composable
private fun ComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    working: Boolean,
    enabled: Boolean,
    hasHistory: Boolean,
    config: app.zemote.protocol.ConvConfig?,
    usage: app.zemote.protocol.ConvUsage?,
    modelOptions: List<app.zemote.protocol.ModelOption>,
    stopWorkId: String?,
    followupMode: String?,
    onAttach: () -> Unit,
    onThoughtSelect: (String) -> Unit,
    onModelSelect: (provider: String, model: String) -> Unit,
    onSend: (queued: Boolean) -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .navigationBarsPadding(),
    ) {
        Surface(
            color = app.zemote.ui.theme.cardContainerColor(),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                // ── 输入框（卡内无边框，占满宽度）；占位文案对齐官方 chat.placeholder ──
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            when {
                                working && hasHistory -> stringResource(R.string.followup_queue)
                                hasHistory -> stringResource(R.string.followup_ask)
                                else -> stringResource(R.string.placeholder_new)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    minLines = 1,
                    maxLines = 6,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    // 回车换行，发送走右侧按钮（多行输入）
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(2.dp))
                // ── 控制条：官方一行式工具条 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // ── 左：附件「+」 ──
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AttachmentButton(onClick = onAttach)
                    }

                    // ── 右：上下文 · 模型 · 推理强度 · 停止/发送 ──
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UsageButton(usage = usage)
                        ModelButton(
                            config = config,
                            modelOptions = modelOptions,
                            onSelect = onModelSelect,
                        )
                        ThoughtLevelButton(
                            config = config,
                            onSelect = onThoughtSelect,
                        )
                        if (working && text.isBlank()) {
                            // 生成中且无草稿：唯一的黑色方块是停止
                            StopButton(onClick = onStop)
                        } else {
                            // 有草稿（或空闲）：黑色方块是发送；生成中发送即官方 enqueue 语义
                            if (working) {
                                StopButton(onClick = onStop, secondary = true)
                            }
                            SendButton(
                                label = if (working) stringResource(R.string.enqueue)
                                else stringResource(R.string.send),
                                enabled = enabled && text.isNotBlank(),
                                onClick = { onSend(working) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────── 按钮组件（官方 composer 工具条） ──────────────────────────

/** 附件按钮：加号幽灵图标，唤起系统文件选择器（图片/任意文件） */
@Composable
private fun AttachmentButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(R.string.attach),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 思考等级胶囊：官方「🧠 最高 ⌄」样式 —— 图标 + 当前等级文字 + 下拉箭头。
 * 当前模型不支持思考（等级列表为空）时不显示。
 */
@Composable
private fun ThoughtLevelButton(
    config: app.zemote.protocol.ConvConfig?,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val current = config?.thought
    val levels = config?.thoughtLevels ?: emptyList()
    if (levels.isEmpty()) return

    Box {
        Row(
            modifier = Modifier
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = stringResource(R.string.thought_level),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                thoughtLabel(ctx, current ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (level in levels) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(thoughtLabel(ctx, level), style = MaterialTheme.typography.bodyMedium)
                            if (level == current) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        }
                    },
                    onClick = { open = false; onSelect(level) },
                )
            }
        }
    }
}

/**
 * 模型胶囊：官方「GLM-5.3 ⌄」样式 —— 当前模型名 + 下拉箭头。
 */
@Composable
private fun ModelButton(
    config: app.zemote.protocol.ConvConfig?,
    modelOptions: List<app.zemote.protocol.ModelOption>,
    onSelect: (provider: String, model: String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val currentProvider = config?.provider
    val currentModel = config?.model

    Box {
        Row(
            modifier = Modifier
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                shortModelName(currentModel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp),
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.model_label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (modelOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.model_empty), style = MaterialTheme.typography.bodyMedium) },
                    onClick = { open = false },
                )
            } else {
                for (opt in modelOptions) {
                    val isSelected = opt.provider == currentProvider && opt.model == currentModel
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(opt.label, style = MaterialTheme.typography.bodyMedium)
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        onClick = { open = false; onSelect(opt.provider, opt.model) },
                    )
                }
            }
        }
    }
}

/** 上下文用量：官方以百分比小字呈现，颜色随用量分档，点击弹出明细 */
@Composable
private fun UsageButton(usage: app.zemote.protocol.ConvUsage?) {
    var open by remember { mutableStateOf(false) }
    if (usage == null || usage.maxTokens == 0L) return

    val ratio = usage.ratio.coerceIn(0f, 1f)
    // 官方上下文配色以 sky 为主：低用量 sky，中段 warning 黄，将满 error 红
    val color = when {
        ratio < 0.5f -> MaterialTheme.colorScheme.secondary
        ratio < 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Box {
        Text(
            "${(ratio * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
        val ctx2 = LocalContext.current
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        "${formatToken(usage.usedTokens)} / ${formatToken(usage.maxTokens)} tokens",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                onClick = { open = false },
            )
            if (usage.hitRate != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.usage_cache_hit, (usage.hitRate * 100).toInt()), style = MaterialTheme.typography.bodyMedium) },
                    onClick = { open = false },
                )
            }
            for ((source, chars) in usage.breakdown) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.usage_chars, sourceLabel(ctx2, source), "${(chars / 1000).toInt()}k"), style = MaterialTheme.typography.bodySmall)
                    },
                    onClick = { open = false },
                )
            }
        }
    }
}

/**
 * 停止按钮：官方生成中按钮。默认黑色圆角方块（rounded-lg + brand）；
 * [secondary] 为 true 时降级为描边方块（草稿存在、右侧让位给发送键时）。
 */
@Composable
private fun StopButton(onClick: () -> Unit, secondary: Boolean = false) {
    if (secondary) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.stop_gen),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    } else {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.stop_gen),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/**
 * 发送按钮：官方 rounded-lg + --color-brand 黑色圆角方块 + 向上箭头。
 * 生成中时执行官方 enqueue 语义（加入队列）。
 */
@Composable
private fun SendButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ────────────────────────── 辅助函数 ──────────────────────────

private fun thoughtLabel(ctx: android.content.Context, level: String): String = when (level.lowercase()) {
    "off", "none", "disabled" -> ctx.getString(R.string.thought_off)
    "nothink", "no-think", "no_think" -> ctx.getString(R.string.thought_no)
    "on", "enabled" -> ctx.getString(R.string.thought_on)
    "low", "light", "minimal", "shallow" -> ctx.getString(R.string.thought_low)
    "medium", "balanced", "default" -> ctx.getString(R.string.thought_medium)
    "high" -> ctx.getString(R.string.thought_high)
    "xhigh", "extra-high", "extra_high", "very-high", "very_high" -> ctx.getString(R.string.thought_vhigh)
    "max", "maximum" -> ctx.getString(R.string.thought_max)
    else -> level
}

/** 来源名称中文映射 */
private fun sourceLabel(ctx: android.content.Context, source: String): String = when (source) {
    "messages" -> ctx.getString(R.string.usage_messages)
    "system_tool_schemas" -> ctx.getString(R.string.usage_system_tools)
    "mcp_tool_schemas" -> ctx.getString(R.string.usage_mcp_tools)
    "system_prompt" -> ctx.getString(R.string.usage_system_prompt)
    "skills" -> ctx.getString(R.string.usage_skills)
    "meta_user_context" -> ctx.getString(R.string.usage_other)
    else -> source
}

/** token 数值格式化 */
private fun formatToken(n: Long): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000 -> "${n / 1_000}K"
    else -> n.toString()
}

/** 模型短名：官方胶囊里显示的当前模型名（如 GLM-4.6），去掉路径前缀并规范化首字母 */
private fun shortModelName(model: String?): String {
    if (model.isNullOrBlank()) return ""
    val name = model.substringAfterLast('/')
    return name.replaceFirstChar { it.uppercase() }
}

/** 模型显示名称（带 provider 前缀，便于区分不同提供商的同名模型） */
private fun modelLabel(provider: String, model: String): String {
    val p = when (provider.lowercase()) {
        "zai", "zcode" -> "ZAI"
        "glm" -> "GLM"
        "deepseek" -> "DeepSeek"
        "anthropic" -> "Anthropic"
        "openai" -> "OpenAI"
        "qwen" -> "Qwen"
        "moonshot" -> "Moonshot"
        else -> provider
    }
    return "$p · $model"
}

@Composable
private fun CenterHint(
    icon: @Composable () -> Unit,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) { icon() }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (body != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
