//
//  RuntimeMessageCodecTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RuntimeMessageCodecTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `request envelope preserves method parameters and extensions`() {
        val source = fixture("v2/valid/unknown-method-request.json")

        val request = codec.decodeRequest(source)
        val encoded = codec.encodeRequest(request)
        val decoded = codec.decodeRequest(encoded)

        assertEquals("vendor.experimentalInspection", decoded.method.rawValue)
        assertEquals(JsonPrimitive("compact"), decoded.parameters["mode"])
        assertEquals(request, decoded)
    }

    @Test
    fun `response envelope enforces status member exclusivity`() {
        val success = codec.decodeResponse(fixture("v2/valid/handshake-response.json"))
        val failure = codec.decodeResponse(fixture("v2/valid/node-detail-failure-response.json"))

        assertIs<RuntimeResponseOutcome.Success>(success.outcome)
        assertIs<RuntimeResponseOutcome.Failure>(failure.outcome)
        assertFailsWith<RuntimeMessageException.InvalidEnvelope> {
            codec.decodeResponse(fixture("v2/invalid/success-response-contains-error.json"))
        }
        assertFailsWith<RuntimeMessageException.InvalidEnvelope> {
            codec.decodeResponse(fixture("v2/invalid/failure-response-contains-payload.json"))
        }
    }

    @Test
    fun `codec rejects unsupported protocol versions and scalar roots`() {
        assertFailsWith<RuntimeMessageException.UnsupportedProtocolVersion> {
            codec.decodeRequest(fixture("v2/invalid/request-uses-v1.json"))
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeRequest("[]".encodeToByteArray())
        }
    }

    @Test
    fun `codec rejects duplicate keys trailing data and unsafe integers`() {
        val invalidDocuments = listOf(
            """{"method":"handshake","method":"nodeDetail"}""",
            """{"method":"handshake","\u006dethod":"nodeDetail"}""",
            """{"method":"handshake"} true""",
            """{"value":9007199254740992}"""
        )

        invalidDocuments.forEach { source ->
            assertFailsWith<RuntimeMessageException.InvalidDocument> {
                codec.decodeDocument(source.encodeToByteArray())
            }
        }
    }

    @Test
    fun `codec rejects byte order marks and invalid UTF-8`() {
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeDocument(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "{}".encodeToByteArray())
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeDocument(byteArrayOf(0xC3.toByte(), 0x28))
        }
    }

    @Test
    fun `codec decodes every valid request and response fixture`() {
        validFixtureNames()
            .filterNot { it == "vector-attribute-value.json" }
            .forEach { name ->
                val source = fixture("v2/valid/$name")
                val document = codec.decodeDocument(source)
                val objectValue = assertIs<JsonObject>(document)
                when {
                    "status" in objectValue -> codec.decodeResponse(source)
                    "method" in objectValue -> codec.decodeRequest(source)
                    else -> error("Unexpected protocol fixture: $name")
                }
            }
    }

    @Test
    fun `codec decodes standalone valid attribute values`() {
        val document = codec.decodeDocument(fixture("v2/valid/vector-attribute-value.json"))

        assertIs<JsonObject>(document)
    }

    private fun validFixtureNames(): List<String> = checkNotNull(
        javaClass.classLoader.getResource("v2/valid")
    ).toURI().let(::File).listFiles()
        ?.filter { it.extension == "json" }
        ?.map { it.name }
        ?.sorted()
        ?: error("Protocol fixtures are unavailable")

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
