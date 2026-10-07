package app.zemote.protocol

import java.util.concurrent.ConcurrentHashMap

/**
 * RPC frame transport: logical messages split into frames with CRC32 checksum.
 *
 * Outbound: [sendMessage()] takes the full IPC frame body (with or without the
 *   13-byte framing header — [ChannelClient] includes it), fragments it, and
 *   sends rpc-frames. Frames are kept in an outbound replay buffer until the
 *   desktop acknowledges them (rpc-frame-ack with ackMessageSeq); after a link
 *   loss all unacknowledged frames are retransmitted via [replayUnacknowledged]
 *   (the desktop tolerates duplicates by design — this is the official
 *   l2t transport's replay mechanism).
 *
 * Inbound:  [acceptPayload()] reassembles fragments, verifies CRC32, and emits
 *   the complete assembled bytes (13-byte header + encoded value-list) to
 *   [onMessage]. The caller (ChannelClient.handleMessage) decodes the
 *   value-list header itself. Do NOT pre-strip the IPC framing header here.
 *   Every complete inbound message is acknowledged with rpc-frame-ack; a lost
 *   ack makes the desktop's replay buffer expire (replayGraceExceeded) and
 *   degrade the bridge, so the ack is retried until it is actually sent.
 */
class RpcFrameTransport(
    val bridgeSessionId: String,
    val bridgeGeneration: Int? = null,
    val recoveryId: String? = null,
    /** 返回是否已真正写入 socket：false = 链路断开（帧保留在重放缓冲里等恢复） */
    private val sendPayload: (Map<String, Any>) -> Boolean,
    private val onLog: ((String) -> Unit)? = null,
) {
    companion object {
        const val MAX_FRAGMENT_BYTES = 512 * 1024
        const val MAX_MESSAGE_BYTES = 16 * 1024 * 1024
        const val MAX_FRAGMENTS = 64

        /** 出站重放缓冲上限（对齐官方 replayBufferMaxBytes，超限丢最老批次） */
        const val MAX_REPLAY_BYTES = 8L * 1024 * 1024
    }

    private var seq = 0
    private var messageSeq = 0

    private val assemblies = ConcurrentHashMap<Int, Assembly>()

    /** 已发送未确认的出站消息（按 messageSeq 升序），每批带重发游标 */
    private class OutBatch(
        val messageSeq: Int,
        val frames: List<Map<String, Any>>,
        val outerBytes: Long,
        var nextFrameIndex: Int = 0,
    )

    private val outbox = ArrayDeque<OutBatch>()
    private var replayBytes = 0L

    /**
     * 待发送的入站数据 ack（最新一条即可：ack N 蕴含 ≤N 全部已收）。
     * 发送失败时保留，下一次发送机会最优先补发。
     */
    private var pendingAck: Int? = null

    /** 计数器、重放缓冲与发送游标的互斥：多个协程会并发调用 sendMessage */
    private val sendLock = Any()
    private var disposed = false

    /**
     * Callback fired when a complete, CRC32-verified message is reassembled.
     * The [frame] is the FULL assembled bytes including the 13-byte IPC framing
     * header — the caller is responsible for decoding the value-list inside.
     */
    var onMessage: ((ByteArray) -> Unit)? = null

    private val identity: Map<String, Any?>
        get() = buildMap {
            put("bridgeSessionId", bridgeSessionId)
            bridgeGeneration?.let { put("bridgeGeneration", it) }
            recoveryId?.let { put("recoveryId", it) }
        }

    /**
     * Fragments [bytes] into rpc-frames and queues them for delivery.
     * [bytes] should be the complete IPC message (with 13-byte framing header
     * if present — [ChannelClient] includes it).
     */
    fun sendMessage(bytes: ByteArray) {
        if (bytes.isEmpty()) throw IllegalArgumentException("empty message")
        if (bytes.size > MAX_MESSAGE_BYTES) throw IllegalArgumentException("message too large")
        val fragmentCount = (bytes.size + MAX_FRAGMENT_BYTES - 1) / MAX_FRAGMENT_BYTES
        if (fragmentCount > MAX_FRAGMENTS) throw IllegalArgumentException("fragment limit exceeded")
        synchronized(sendLock) {
            if (disposed) return
            val msgSeq = ++messageSeq
            val checksum = Crc32.hexOf(bytes)
            // 整条消息的所有帧一次性编好（seq 在锁内自增，杜绝并发重复/跳号）
            val frames = ArrayList<Map<String, Any>>(fragmentCount)
            var outer = 0L
            for (i in 0 until fragmentCount) {
                val start = i * MAX_FRAGMENT_BYTES
                val end = minOf(start + MAX_FRAGMENT_BYTES, bytes.size)
                val chunk = ByteArray(end - start)
                System.arraycopy(bytes, start, chunk, 0, chunk.size)
                seq++
                val frame = buildMap<String, Any> {
                    identity.filterValues { it != null }.forEach { (k, v) -> put(k, v as Any) }
                    put("zcode_type", "rpc-frame")
                    put("seq", seq)
                    put("messageSeq", msgSeq)
                    put("fragmentIndex", i)
                    put("fragmentCount", fragmentCount)
                    put("messageBytes", bytes.size)
                    put("checksum", mapOf("algorithm" to "crc32", "value" to checksum))
                    put("dataBase64", android.util.Base64.encodeToString(chunk, android.util.Base64.NO_WRAP))
                }
                frames.add(frame)
                outer += (frame["dataBase64"] as String).length.toLong()
            }
            outbox.addLast(OutBatch(msgSeq, frames, outer))
            replayBytes += outer
            trimReplayLocked()
            flushLocked()
        }
    }

    /**
     * 链路恢复（relay 重新 PAIRED）后重发全部未确认消息。
     * 游标归零后从头重发，已到达的帧在桌面端按重放（duplicate）处理并回 ack。
     */
    fun replayUnacknowledged() {
        synchronized(sendLock) {
            if (disposed) return
            if (outbox.isEmpty() && pendingAck == null) return
            if (outbox.isNotEmpty()) {
                onLog?.invoke("[rpc] replay ${outbox.size} unacked message(s) after link restored")
                outbox.forEach { it.nextFrameIndex = 0 }
            }
            flushLocked()
        }
    }

    /**
     * 按序发送：先补发挂起的入站 ack，再推进各批次游标。
     * 发送失败（socket 已关闭，sendPayload 返回 false）立即停在当前帧，
     * 游标不前进 —— 等链路恢复后由 [replayUnacknowledged] 重发。
     */
    private fun flushLocked() {
        pendingAck?.let { ack ->
            if (sendPayload(buildMap<String, Any> {
                    identity.filterValues { it != null }.forEach { (k, v) -> put(k, v as Any) }
                    put("zcode_type", "rpc-frame-ack")
                    put("ackMessageSeq", ack)
                })
            ) {
                pendingAck = null
            } else {
                return
            }
        }
        outer@ for (batch in outbox) {
            while (batch.nextFrameIndex < batch.frames.size) {
                if (sendPayload(batch.frames[batch.nextFrameIndex])) {
                    batch.nextFrameIndex++
                } else {
                    break@outer
                }
            }
        }
    }

    /** 重放缓冲超限时丢最老批次（正常流量下 ack 很快到达，几乎不会触发） */
    private fun trimReplayLocked() {
        while (replayBytes > MAX_REPLAY_BYTES && outbox.size > 1) {
            val oldest = outbox.removeFirst()
            replayBytes -= oldest.outerBytes
            onLog?.invoke("[rpc] replay buffer overflow, drop messageSeq=${oldest.messageSeq}")
        }
    }

    /**
     * Receives an incoming rpc-frame payload and assembles fragments.
     * Emits the complete assembled message (with 13-byte IPC header intact) to
     * [onMessage] once all fragments arrive and CRC32 checks out.
     */
    fun acceptPayload(payload: Map<String, Any>): Boolean {
        val type = payload["zcode_type"] as? String ?: return false
        if (type != "rpc-frame" && type != "rpc-frame-ack") return false

        if (type == "rpc-frame-ack") {
            // 桌面端确认收到我方的出站消息：从重放缓冲移除 ≤ackMessageSeq 的批次
            val ackSeq = (payload["ackMessageSeq"] as? Number)?.toInt()
                ?: (payload["messageSeq"] as? Number)?.toInt()
            if (ackSeq != null) {
                synchronized(sendLock) {
                    while (outbox.isNotEmpty() && outbox.first().messageSeq <= ackSeq) {
                        replayBytes -= outbox.removeFirst().outerBytes
                    }
                }
            }
            return true
        }

        @Suppress("UNCHECKED_CAST")
        val dataBase64 = payload["dataBase64"] as? String
        val msgSeqVal = (payload["messageSeq"] as? Number)?.toInt() ?: return false
        val fragIdx = (payload["fragmentIndex"] as? Number)?.toInt() ?: return false
        val fragCount = (payload["fragmentCount"] as? Number)?.toInt() ?: return false
        val msgBytes = (payload["messageBytes"] as? Number)?.toInt() ?: return false
        val checksum = (payload["checksum"] as? Map<*, *>)?.get("value") as? String

        if (dataBase64 == null) return true
        val chunk = try { android.util.Base64.decode(dataBase64, android.util.Base64.NO_WRAP) } catch (_: Exception) { return true }

        val assembly = assemblies.getOrPut(msgSeqVal) {
            Assembly(crc32 = checksum ?: "", count = fragCount)
        }
        assembly.fragments[fragIdx] = chunk
        assembly.lastSeen = System.currentTimeMillis()

        if (assembly.fragments.size == fragCount && assembly.fragments.all { it != null }) {
            // Reassemble into complete message (includes 13-byte IPC header)
            val assembled = ByteArray(msgBytes)
            var offset = 0
            for (i in 0 until fragCount) {
                val frag = assembly.fragments[i]!!
                System.arraycopy(frag, 0, assembled, offset, frag.size)
                offset += frag.size
            }
            // Verify CRC32
            val actualChecksum = Crc32.hexOf(assembled)
            if (actualChecksum != assembly.crc32) {
                onLog?.invoke("[rpc] CRC32 mismatch for msg $msgSeqVal (expected=${assembly.crc32} actual=$actualChecksum)")
                assemblies.remove(msgSeqVal)
                return true
            }
            // 收到完整消息后必须 ack，否则服务端会重传并最终判定 rpc-transport-fault；
            // ack 走队列化补发（链路断开时挂起重试，见 pendingAck）
            queueAck(msgSeqVal)
            // Emit complete assembled message (with framing header) — caller decodes
            onMessage?.invoke(assembled)
            assemblies.remove(msgSeqVal)
        }
        return true
    }

    private fun queueAck(messageSeq: Int) {
        synchronized(sendLock) {
            if (disposed) return
            pendingAck = messageSeq
            flushLocked()
        }
    }

    data class Assembly(
        val crc32: String,
        val count: Int,
        val fragments: Array<ByteArray?> = arrayOfNulls(count),
        var lastSeen: Long = System.currentTimeMillis(),
    )

    fun dispose() {
        synchronized(sendLock) {
            disposed = true
            outbox.clear()
            replayBytes = 0
            pendingAck = null
        }
        assemblies.clear()
    }
}
