//
//  RuntimeCancellationModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeCancellationModelTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `cancellation fixtures decode through typed contracts`() {
        val request = codec.decodeRequest(fixture("v2/valid/cancel-request.json"))
        val response = codec.decodeResponse(fixture("v2/valid/cancel-response.json"))

        val parameters = codec.decodeRequestParameters(request, RuntimeCancelRequestParameters.contract)
        val payload = codec.decodeSuccessPayload(response, RuntimeCancelRequestPayload.contract)

        assertEquals(parameters.targetRequestID, payload.targetRequestID)
        assertEquals(true, payload.cancellationAccepted)
    }

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
