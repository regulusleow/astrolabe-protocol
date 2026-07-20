//
//  RuntimeSessionModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.io.File
import kotlinx.serialization.json.JsonPrimitive
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
    fun `typed handshake values round trip without exposing JSON assembly`() {
        val sourceRequest = codec.decodeRequest(fixture("v2/valid/handshake-request.json"))
        val parameters = codec.decodeRequestParameters(sourceRequest, RuntimeHandshakeParameters.contract)
        val encodedRequest = codec.encodeRequest(
            requestID = sourceRequest.requestID,
            contract = RuntimeHandshakeParameters.contract,
            parameters = parameters
        )

        val sourceResponse = codec.decodeResponse(fixture("v2/valid/handshake-response.json"))
        val payload = codec.decodeSuccessPayload(sourceResponse, RuntimeHandshakePayload.contract)
        val encodedResponse = codec.encodeSuccessResponse(
            requestID = sourceResponse.requestID,
            contract = RuntimeHandshakePayload.contract,
            payload = payload
        )

        val decodedRequest = codec.decodeRequest(encodedRequest)
        val decodedResponse = codec.decodeResponse(encodedResponse)
        assertEquals(parameters, codec.decodeRequestParameters(decodedRequest, RuntimeHandshakeParameters.contract))
        assertEquals(payload, codec.decodeSuccessPayload(decodedResponse, RuntimeHandshakePayload.contract))
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
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                fixture("v2/invalid/extension-key-not-namespaced.json"),
                RuntimeExtensionMap.serializer()
            )
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.encodeValue(
                RuntimeExtensionMap(
                    mapOf("vendor.unsafeInteger" to JsonPrimitive(9_007_199_254_740_992L))
                ),
                RuntimeExtensionMap.serializer()
            )
        }
    }

    private fun fixture(path: String): ByteArray = File(
        checkNotNull(javaClass.classLoader.getResource(path)).toURI()
    ).readBytes()
}
