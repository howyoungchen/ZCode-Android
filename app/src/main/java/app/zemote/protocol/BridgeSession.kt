package app.zemote.protocol

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A workspace bridge session. Holds the rpc-frame transport and IPC channels. */
class BridgeSession(
    var bridge: Map<String, Any>,
    private val relayClient: RelayClient,
    private val onDispose: (BridgeSession) -> Unit,
    private val onLog: ((String) -> Unit)?,
) {
    val degraded = MutableStateFlow<String?>(null)
    val recovered = MutableStateFlow(0)

    private var _disposed = false
    private var _transport: RpcFrameTransport
    private var _channels: ChannelClient
    /** CoroutineScope for the relay-payloads listener; restarted on swapBridge. */
    private var relayListenerScope: CoroutineScope? = null
    /** relay 链路恢复回调的注销句柄（随 transport 一起换绑） */
    private var linkRestoredHandle: (() -> Unit)? = null

    val workspaceKey: String? get() = bridge["workspaceKey"] as? String
    val initialTaskId: String? get() = bridge["initialTaskId"] as? String
    val bridgeSessionId: String get() = bridge["bridgeSessionId"] as? String ?: ""

    init {
        _transport = buildTransport(relayClient)
        _channels = buildChannels(_transport)
        // Wire assembled IPC bodies → channel client
        _transport.onMessage = { frame -> _channels.handleMessage(frame) }
        // Listen for relay payloads and route rpc-frame(-ack) to the transport
        startRelayListener()
        wireLinkRestored()
    }

    /** 链路恢复后重发未确认帧（传输层的重放缓冲在 rpc 断链时兜底） */
    private fun wireLinkRestored() {
        linkRestoredHandle?.invoke()
        linkRestoredHandle = relayClient.addOnLinkRestored { _transport.replayUnacknowledged() }
    }

    var isRecovering = false
    val isDisposed get() = _disposed

    private fun startRelayListener() {
        // Cancel any existing listener before starting a new one
        relayListenerScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        relayListenerScope = scope
        scope.launch {
            relayClient.payloads.collect { payload ->
                val type = payload["zcode_type"] as? String
                val bsid = payload["bridgeSessionId"] as? String
                if (type in listOf("rpc-frame", "rpc-frame-ack") && bsid == bridgeSessionId) {
                    _transport.acceptPayload(payload)
                }
            }
        }
    }

    private fun buildTransport(relay: RelayClient): RpcFrameTransport = RpcFrameTransport(
        bridgeSessionId = bridgeSessionId,
        // bridge map 来自 JSON，数字一律是 Double，必须用 Number 安全转换
        bridgeGeneration = (bridge["bridgeGeneration"] as? Number)?.toInt(),
        recoveryId = bridge["recoveryId"] as? String,
        sendPayload = { relay.send(it) },
        onLog = onLog,
    )

    private fun buildChannels(transport: RpcFrameTransport): ChannelClient = ChannelClient(
        sendBody = { transport.sendMessage(it) },
        onLog = onLog,
    )

    /**
     * Swaps in a newly-opened bridge (used after reopen during recovery).
     * Rebuilds the transport + channel stack and restarts the relay listener
     * so it targets the new bridgeSessionId.
     */
    internal fun swapBridge(newBridge: Map<String, Any>, newRelay: RelayClient) {
        bridge = newBridge
        _transport.dispose()
        _channels.dispose()
        relayListenerScope?.cancel()
        relayListenerScope = null
        _transport = buildTransport(newRelay)
        _channels = buildChannels(_transport)
        // 重置 ready：新 bridge 的 Initialize 帧未到达，需重新等待
        _channels.resetReady()
        // Re-wire: assembled IPC bodies → new channel client
        _transport.onMessage = { frame -> _channels.handleMessage(frame) }
        // Start fresh listener for the new bridge session
        startRelayListener()
        // 换绑链路恢复回调到新 transport，并递增 recovered 通知会话层重建
        wireLinkRestored()
        recovered.value += 1
    }

    val channelsClient: ChannelClient get() = _channels

    fun dispose() {
        if (_disposed) return
        _disposed = true
        degraded.value = null
        linkRestoredHandle?.invoke()
        linkRestoredHandle = null
        relayListenerScope?.cancel()
        relayListenerScope = null
        _transport.dispose()
        _channels.dispose()
        onDispose(this)
    }
}

