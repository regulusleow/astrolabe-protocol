//
//  ProtocolFixtureConformanceTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtocolFixtureConformanceTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `every valid fixture has an explicit Kotlin conformance path`() {
        val cases = mapOf<String, (ByteArray) -> Unit>(
            "application-info-request.json" to { decodeRequest(RuntimeApplicationInfoParameters.contract, it) },
            "application-info-response.json" to { decodeResponse(RuntimeApplicationInfoPayload.contract, it) },
            "apply-attribute-patch-request.json" to { decodeRequest(RuntimeApplyAttributePatchParameters.contract, it) },
            "apply-attribute-patch-response.json" to { decodeResponse(RuntimeAttributePatch.applyContract, it) },
            "cancel-request.json" to { decodeRequest(RuntimeCancelRequestParameters.contract, it) },
            "cancel-response.json" to { decodeResponse(RuntimeCancelRequestPayload.contract, it) },
            "clear-attribute-patches-request.json" to { decodeRequest(RuntimeClearAttributePatchesParameters.contract, it) },
            "clear-attribute-patches-response.json" to { decodeResponse(RuntimeClearAttributePatchesPayload.contract, it) },
            "handshake-request.json" to { decodeRequest(RuntimeHandshakeParameters.contract, it) },
            "handshake-response.json" to { decodeResponse(RuntimeHandshakePayload.contract, it) },
            "hierarchy-snapshot-request.json" to { decodeRequest(RuntimeHierarchySnapshotParameters.contract, it) },
            "hierarchy-snapshot-response.json" to { decodeResponse(RuntimeHierarchySnapshotPayload.contract, it) },
            "list-attribute-patches-request.json" to { decodeRequest(RuntimeListAttributePatchesParameters.contract, it) },
            "list-attribute-patches-response.json" to { decodeResponse(RuntimeAttributePatchListPayload.contract, it) },
            "node-detail-failure-response.json" to { codec.decodeResponse(it) },
            "node-detail-request.json" to { decodeRequest(RuntimeNodeDetailParameters.contract, it) },
            "node-detail-response.json" to { decodeResponse(RuntimeNodeDetailPayload.contract, it) },
            "patchable-attributes-request.json" to { decodeRequest(RuntimePatchableAttributesParameters.contract, it) },
            "patchable-attributes-response.json" to { decodeResponse(RuntimePatchableAttributesPayload.contract, it) },
            "revert-attribute-patch-request.json" to { decodeRequest(RuntimeRevertAttributePatchParameters.contract, it) },
            "revert-attribute-patch-response.json" to { decodeResponse(RuntimeRevertAttributePatchPayload.contract, it) },
            "unknown-method-failure-response.json" to { codec.decodeResponse(it) },
            "unknown-method-request.json" to { codec.decodeRequest(it) },
            "vector-attribute-value.json" to { codec.decodeValue(it, RuntimeAttributeValue.serializer()) }
        )

        assertEquals(fixtureNames("v2/valid"), cases.keys)
        cases.forEach { (name, operation) -> operation(fixture("v2/valid/$name")) }
    }

    @Test
    fun `every invalid fixture has an explicit Kotlin rejection path`() {
        val cases = mapOf<String, (ByteArray) -> Unit>(
            "attribute-identifier-not-namespaced.json" to { source ->
                decodeRequest(RuntimeApplyAttributePatchParameters.contract, source)
            },
            "coordinate-rect-missing-unit.json" to { source ->
                codec.decodeValue(source, RuntimeCoordinateRect.serializer())
            },
            "descending-protocol-range.json" to { source ->
                decodeRequest(RuntimeHandshakeParameters.contract, source)
            },
            "display-size-missing-unit.json" to { source ->
                codec.decodeValue(source, RuntimeDisplayInfo.serializer())
            },
            "extension-key-not-namespaced.json" to { source ->
                codec.decodeValue(source, RuntimeExtensionMap.serializer())
            },
            "failure-response-contains-payload.json" to { source -> codec.decodeResponse(source) },
            "inconsistent-visibility.json" to { source ->
                codec.decodeValue(source, RuntimeNodeVisibility.serializer())
            },
            "node-id-is-number.json" to { source ->
                decodeRequest(RuntimeNodeDetailParameters.contract, source)
            },
            "patch-pattern-not-namespaced.json" to { source ->
                codec.decodeValue(source, RuntimePatchableAttribute.serializer())
            },
            "request-missing-request-id.json" to { source -> codec.decodeRequest(source) },
            "request-uses-v1.json" to { source -> codec.decodeRequest(source) },
            "success-response-contains-error.json" to { source -> codec.decodeResponse(source) },
            "unknown-response-status.json" to { source -> codec.decodeResponse(source) },
            "unsafe-integer-attribute-value.json" to { source ->
                decodeRequest(RuntimeApplyAttributePatchParameters.contract, source)
            }
        )

        assertEquals(fixtureNames("v2/invalid"), cases.keys)
        cases.forEach { (name, operation) ->
            assertFailsWith<RuntimeMessageException> {
                operation(fixture("v2/invalid/$name"))
            }
        }
    }

    private fun <T> decodeRequest(contract: RuntimeMethodContract<T>, source: ByteArray) {
        val request = codec.decodeRequest(source)
        val parameters = codec.decodeRequestParameters(request, contract)
        val encoded = codec.encodeRequest(request.requestID, contract, parameters)
        codec.decodeRequestParameters(codec.decodeRequest(encoded), contract)
    }

    private fun <T> decodeResponse(contract: RuntimeMethodContract<T>, source: ByteArray) {
        val response = codec.decodeResponse(source)
        val payload = codec.decodeSuccessPayload(response, contract)
        val encoded = codec.encodeSuccessResponse(response.requestID, contract, payload)
        codec.decodeSuccessPayload(codec.decodeResponse(encoded), contract)
    }

    private fun fixtureNames(path: String): Set<String> = fixtureDirectory(path)
        .listFiles()
        .orEmpty()
        .filter { it.extension == "json" }
        .map { it.name }
        .toSet()

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()

    private fun fixtureDirectory(path: String): File = File(
        checkNotNull(javaClass.classLoader.getResource(path)).toURI()
    )
}
