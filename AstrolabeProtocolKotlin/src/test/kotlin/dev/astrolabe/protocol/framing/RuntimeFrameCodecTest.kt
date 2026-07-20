//
//  RuntimeFrameCodecTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimeFrameCodecTest {
    @Test
    fun `encode writes payload length in network byte order`() {
        val payload = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())

        val frame = RuntimeFrameCodec().encode(payload)

        assertContentEquals(byteArrayOf(0, 0, 0, 3), frame.copyOfRange(0, 4))
        assertContentEquals(payload, frame.copyOfRange(4, frame.size))
    }

    @Test
    fun `stream decoder handles fragmented and consecutive frames`() {
        val codec = RuntimeFrameCodec()
        val first = codec.encode("first".encodeToByteArray())
        val second = codec.encode("second".encodeToByteArray())
        val decoder = codec.makeStreamDecoder()

        assertEquals(emptyList(), decoder.append(first.copyOfRange(0, 3)))
        assertEquals(
            listOf("first", "second"),
            decoder.append(first.copyOfRange(3, first.size) + second)
                .map { it.decodeToString() }
        )
        assertEquals(0, decoder.pendingByteCount)
    }

    @Test
    fun `codec rejects empty and oversized payloads`() {
        val codec = RuntimeFrameCodec(maximumPayloadSize = 2)

        assertFailsWith<RuntimeFrameException.EmptyPayload> {
            codec.encode(byteArrayOf())
        }
        assertFailsWith<RuntimeFrameException.PayloadTooLarge> {
            codec.encode(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun `stream decoder rejects invalid declared payload lengths`() {
        val decoder = RuntimeFrameCodec(maximumPayloadSize = 2).makeStreamDecoder()

        assertFailsWith<RuntimeFrameException.EmptyPayload> {
            decoder.append(byteArrayOf(0, 0, 0, 0))
        }

        decoder.reset()
        assertFailsWith<RuntimeFrameException.PayloadTooLarge> {
            decoder.append(byteArrayOf(0, 0, 0, 3))
        }
    }
}
