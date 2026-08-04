//
//  RuntimeNodeDetailModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimeNodeDetailModelTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `node detail fixtures decode through typed contracts`() {
        val request = codec.decodeRequest(fixture("v2/Fixtures/valid/node-detail-request.json"))
        val response = codec.decodeResponse(fixture("v2/Fixtures/valid/node-detail-response.json"))

        val parameters = codec.decodeRequestParameters(request, RuntimeNodeDetailParameters.contract)
        val payload = codec.decodeSuccessPayload(response, RuntimeNodeDetailPayload.contract)

        assertEquals(parameters.nodeID, payload.nodeID)
        assertEquals("common.text", payload.sections.first().category.rawValue)
    }

    @Test
    fun `node identifiers and attribute identifiers retain wire constraints`() {
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            val request = codec.decodeRequest(fixture("v2/Fixtures/invalid/node-id-is-number.json"))
            codec.decodeRequestParameters(request, RuntimeNodeDetailParameters.contract)
        }
        assertFailsWith<IllegalArgumentException> {
            RuntimeAttributeIdentifier("content")
        }
    }

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
