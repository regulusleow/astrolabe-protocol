//
//  RuntimeSessionModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimeSessionModelTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `handshake request decodes typed parameters`() {
        val request = codec.decodeRequest(fixture("v2/valid/handshake-request.json"))

        val parameters = codec.decodeRequestParameters(
            request,
            RuntimeHandshakeParameters.contract
        )

        assertEquals("astrolabe-host", parameters.client.name)
        assertEquals(RuntimeProtocolRange.V2, parameters.supportedProtocolRange)
    }

    @Test
    fun `handshake response decodes typed payload`() {
        val response = codec.decodeResponse(fixture("v2/valid/handshake-response.json"))

        val payload = codec.decodeSuccessPayload(
            response,
            RuntimeHandshakePayload.contract
        )

        assertEquals("ios", payload.platform)
        assertEquals(RuntimeProtocolVersion.V2, payload.negotiatedProtocolVersion)
        assertEquals("astrolabe.runtime.ios", payload.runtime.identifier.rawValue)
    }

    @Test
    fun `protocol range rejects descending and cross-major values`() {
        assertFailsWith<IllegalArgumentException> {
            RuntimeProtocolRange(
                minimum = RuntimeProtocolVersion(2, 1),
                maximum = RuntimeProtocolVersion(2, 0)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RuntimeProtocolRange(
                minimum = RuntimeProtocolVersion(1, 0),
                maximum = RuntimeProtocolVersion(2, 0)
            )
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeRequest(fixture("v2/invalid/descending-protocol-range.json")).let { request ->
                codec.decodeRequestParameters(request, RuntimeHandshakeParameters.contract)
            }
        }
    }

    @Test
    fun `typed decoding rejects a mismatched method before reading payload`() {
        val request = codec.decodeRequest(fixture("v2/valid/application-info-request.json"))
        val response = codec.decodeResponse(fixture("v2/valid/application-info-response.json"))

        assertFailsWith<RuntimeMessageException.MethodMismatch> {
            codec.decodeRequestParameters(request, RuntimeHandshakeParameters.contract)
        }
        assertFailsWith<RuntimeMessageException.MethodMismatch> {
            codec.decodeSuccessPayload(response, RuntimeHandshakePayload.contract)
        }
    }

    @Test
    fun `open identifiers reject empty or unnamespaced values`() {
        assertFailsWith<IllegalArgumentException> { RuntimeMethod("") }
        assertFailsWith<IllegalArgumentException> { RuntimeCapability("") }
        assertFailsWith<IllegalArgumentException> { RuntimeOpaqueIdentifier("") }
        assertFailsWith<IllegalArgumentException> { RuntimeNamespacedIdentifier("missingNamespace") }
    }

    @Test
    fun `error codes and extension keys follow shared identifier rules`() {
        assertEquals("nodeNotFound", RuntimeErrorCode.nodeNotFound.rawValue)
        assertEquals("vendor.customFailure", RuntimeErrorCode("vendor.customFailure").rawValue)
        assertFailsWith<IllegalArgumentException> { RuntimeErrorCode("customFailure") }
        assertFailsWith<IllegalArgumentException> {
            RuntimeExtensionMap(mapOf("invalid" to kotlinx.serialization.json.JsonNull))
        }
    }

    private fun fixture(path: String): ByteArray = File(
        checkNotNull(javaClass.classLoader.getResource(path)).toURI()
    ).readBytes()
}
