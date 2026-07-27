//
//  RuntimeApplicationModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimeApplicationModelTest {
    private val codec = RuntimeMessageCodec()

    @Test
    fun `application info fixtures decode through typed contracts`() {
        val request = codec.decodeRequest(fixture("v2/valid/application-info-request.json"))
        val response = codec.decodeResponse(fixture("v2/valid/application-info-response.json"))

        codec.decodeRequestParameters(request, RuntimeApplicationInfoParameters.contract)
        val payload = codec.decodeSuccessPayload(response, RuntimeApplicationInfoPayload.contract)

        assertEquals("com.example.demo", payload.application.identifier)
        assertEquals(3.0, payload.environment.display.logicalToPixelScale.x)
        assertEquals("phone", payload.environment.extensions?.values?.get("ios.uikit.interfaceIdiom")?.toString()?.trim('"'))
    }

    @Test
    fun `display facts require units`() {
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                fixture("v2/invalid/display-size-missing-unit.json"),
                RuntimeDisplayInfo.serializer()
            )
        }
    }

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
