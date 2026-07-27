//
//  RuntimePatchingModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimePatchingModelTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `every patch request and response fixture decodes through its typed contract`() {
        decodeRoundTrip(
            "apply-attribute-patch",
            RuntimeApplyAttributePatchParameters.contract,
            RuntimeAttributePatch.applyContract
        )
        decodeRoundTrip(
            "list-attribute-patches",
            RuntimeListAttributePatchesParameters.contract,
            RuntimeAttributePatchListPayload.contract
        )
        decodeRoundTrip(
            "revert-attribute-patch",
            RuntimeRevertAttributePatchParameters.contract,
            RuntimeRevertAttributePatchPayload.contract
        )
        decodeRoundTrip(
            "clear-attribute-patches",
            RuntimeClearAttributePatchesParameters.contract,
            RuntimeClearAttributePatchesPayload.contract
        )
        decodeRoundTrip(
            "patchable-attributes",
            RuntimePatchableAttributesParameters.contract,
            RuntimePatchableAttributesPayload.contract
        )
    }

    @Test
    fun `patch identifiers and patterns reject ambiguous values`() {
        val request = codec.decodeRequest(fixture("v2/invalid/attribute-identifier-not-namespaced.json"))

        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeRequestParameters(request, RuntimeApplyAttributePatchParameters.contract)
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                fixture("v2/invalid/patch-pattern-not-namespaced.json"),
                RuntimePatchableAttribute.serializer()
            )
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            val unsafeRequest = codec.decodeRequest(
                fixture("v2/invalid/unsafe-integer-attribute-value.json")
            )
            codec.decodeRequestParameters(unsafeRequest, RuntimeApplyAttributePatchParameters.contract)
        }
    }

    @Test
    fun `patch count cannot be negative`() {
        assertFailsWith<IllegalArgumentException> {
            RuntimeClearAttributePatchesPayload(emptyList(), -1)
        }
    }

    private fun <Parameters, Payload> decodeRoundTrip(
        fixturePrefix: String,
        requestContract: RuntimeMethodContract<Parameters>,
        payloadContract: RuntimeMethodContract<Payload>
    ) {
        val request = codec.decodeRequest(fixture("v2/valid/$fixturePrefix-request.json"))
        val response = codec.decodeResponse(fixture("v2/valid/$fixturePrefix-response.json"))

        codec.decodeRequestParameters(request, requestContract)
        codec.decodeSuccessPayload(response, payloadContract)
        assertEquals(request.method, response.method)
    }

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
