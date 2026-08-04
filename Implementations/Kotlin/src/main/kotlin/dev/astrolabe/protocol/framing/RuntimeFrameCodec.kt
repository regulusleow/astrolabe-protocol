//
//  RuntimeFrameCodec.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

/** Encodes and decodes length-prefixed Astrolabe wire frames. */
public class RuntimeFrameCodec(
    /** Maximum number of payload bytes accepted in one frame. */
    public val maximumPayloadSize: Int = DEFAULT_MAXIMUM_PAYLOAD_SIZE
) {
    init {
        require(maximumPayloadSize > 0) {
            "maximumPayloadSize must be greater than zero"
        }
    }

    /** Creates one network-byte-order frame for [payload]. */
    public fun encode(payload: ByteArray): ByteArray {
        if (payload.isEmpty()) {
            throw RuntimeFrameException.EmptyPayload
        }
        if (payload.size > maximumPayloadSize) {
            throw RuntimeFrameException.PayloadTooLarge(
                actual = payload.size.toLong(),
                maximum = maximumPayloadSize
            )
        }

        val length = payload.size
        return ByteArray(FRAME_HEADER_SIZE + length).also { frame ->
            frame[0] = (length ushr 24).toByte()
            frame[1] = (length ushr 16).toByte()
            frame[2] = (length ushr 8).toByte()
            frame[3] = length.toByte()
            payload.copyInto(frame, destinationOffset = FRAME_HEADER_SIZE)
        }
    }

    /** Creates a stateful decoder for fragmented byte streams. */
    public fun makeStreamDecoder(): RuntimeFrameStreamDecoder =
        RuntimeFrameStreamDecoder(maximumPayloadSize)

    public companion object {
        /** Default upper bound for one framed JSON payload. */
        public const val DEFAULT_MAXIMUM_PAYLOAD_SIZE: Int = 16 * 1024 * 1024

        internal const val FRAME_HEADER_SIZE: Int = 4
    }
}

/** Decodes zero or more complete frames from fragmented input. */
public class RuntimeFrameStreamDecoder internal constructor(
    private val maximumPayloadSize: Int
) {
    private val header = ByteArray(RuntimeFrameCodec.FRAME_HEADER_SIZE)
    private var headerByteCount: Int = 0
    private var payload: ByteArray? = null
    private var payloadByteCount: Int = 0

    /** Number of bytes retained while waiting for a complete frame. */
    public val pendingByteCount: Int
        get() = headerByteCount + payloadByteCount

    /** Appends bytes and returns every complete payload now available. */
    public fun append(data: ByteArray): List<ByteArray> {
        val payloads = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            if (payload == null) {
                val headerBytesToCopy = minOf(
                    RuntimeFrameCodec.FRAME_HEADER_SIZE - headerByteCount,
                    data.size - offset
                )
                data.copyInto(
                    destination = header,
                    destinationOffset = headerByteCount,
                    startIndex = offset,
                    endIndex = offset + headerBytesToCopy
                )
                headerByteCount += headerBytesToCopy
                offset += headerBytesToCopy
                if (headerByteCount < RuntimeFrameCodec.FRAME_HEADER_SIZE) {
                    continue
                }
                payload = ByteArray(validatedPayloadLength())
            }

            val currentPayload = payload ?: continue
            val payloadBytesToCopy = minOf(
                currentPayload.size - payloadByteCount,
                data.size - offset
            )
            data.copyInto(
                destination = currentPayload,
                destinationOffset = payloadByteCount,
                startIndex = offset,
                endIndex = offset + payloadBytesToCopy
            )
            payloadByteCount += payloadBytesToCopy
            offset += payloadBytesToCopy
            if (payloadByteCount == currentPayload.size) {
                payloads += currentPayload
                resetFrameState()
            }
        }
        return payloads
    }

    /** Discards any incomplete frame bytes. */
    public fun reset() {
        resetFrameState()
    }

    private fun validatedPayloadLength(): Int {
        val payloadLength = readPayloadLength()
        if (payloadLength == 0L) {
            throw RuntimeFrameException.EmptyPayload
        }
        if (payloadLength > maximumPayloadSize.toLong()) {
            throw RuntimeFrameException.PayloadTooLarge(
                actual = payloadLength,
                maximum = maximumPayloadSize
            )
        }
        return payloadLength.toInt()
    }

    private fun resetFrameState() {
        headerByteCount = 0
        payload = null
        payloadByteCount = 0
    }

    private fun readPayloadLength(): Long =
        (0 until RuntimeFrameCodec.FRAME_HEADER_SIZE).fold(0L) { length, index ->
            (length shl 8) or (header[index].toLong() and 0xFF)
        }
}

/** Failures produced while reading or writing a wire frame. */
public sealed class RuntimeFrameException(message: String) : Exception(message) {
    /** The protocol does not permit an empty frame payload. */
    public data object EmptyPayload : RuntimeFrameException("Frame payload cannot be empty")

    /** A declared or encoded payload exceeds the configured limit. */
    public class PayloadTooLarge(
        /** Number of payload bytes declared or supplied. */
        public val actual: Long,
        /** Maximum number of payload bytes accepted. */
        public val maximum: Int
    ) : RuntimeFrameException("Frame payload size $actual exceeds maximum $maximum")
}
