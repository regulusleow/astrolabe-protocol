//
//  RuntimeNodeDetailModelTest.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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

    @Test
    fun `standalone vector attribute values decode with signed components`() {
        val value = codec.decodeValue(
            fixture("v2/Fixtures/valid/vector-attribute-value.json"),
            RuntimeAttributeValue.serializer()
        )

        assertEquals(RuntimeAttributeValue.Vector(RuntimeVector(-2.0, 4.0, RuntimeMeasurementUnit.logical)), value)
    }

    @Test
    fun `attribute value discriminator must be a string`() {
        val error = assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                """{"type":1,"value":"opaque"}""".encodeToByteArray(),
                RuntimeAttributeValue.serializer()
            )
        }

        assertIs<kotlinx.serialization.SerializationException>(error.cause)
    }

    @Test
    fun `integer attribute value must contain a JSON number`() {
        assertEquals(
            RuntimeAttributeValue.Integer(42),
            codec.decodeValue(
                """{"type":"integer","value":42.0}""".encodeToByteArray(),
                RuntimeAttributeValue.serializer()
            )
        )
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                """{"type":"integer","value":"42"}""".encodeToByteArray(),
                RuntimeAttributeValue.serializer()
            )
        }
        assertFailsWith<RuntimeMessageException.InvalidDocument> {
            codec.decodeValue(
                """{"type":"integer","value":42.000000000000001}""".encodeToByteArray(),
                RuntimeAttributeValue.serializer()
            )
        }
    }

    @Test
    fun `common and extension attribute values round trip symmetrically`() {
        val values = listOf(
            RuntimeAttributeValue.Null,
            RuntimeAttributeValue.BooleanValue(true),
            RuntimeAttributeValue.Integer(42),
            RuntimeAttributeValue.Number(1.5),
            RuntimeAttributeValue.StringValue("value"),
            RuntimeAttributeValue.StringList(listOf("a", "b")),
            RuntimeAttributeValue.Measurement(RuntimeMeasurement(12.0, RuntimeMeasurementUnit.logical)),
            RuntimeAttributeValue.Point(
                RuntimeCoordinatePoint(1.0, 2.0, RuntimeCoordinateSpace.screen, RuntimeMeasurementUnit.logical)
            ),
            RuntimeAttributeValue.Size(RuntimeMeasuredSize(3.0, 4.0, RuntimeMeasurementUnit.pixel)),
            RuntimeAttributeValue.Vector(RuntimeVector(-2.0, 4.0, RuntimeMeasurementUnit.logical)),
            RuntimeAttributeValue.Rect(
                RuntimeCoordinateRect(
                    0.0,
                    0.0,
                    10.0,
                    20.0,
                    RuntimeCoordinateSpace.local,
                    RuntimeMeasurementUnit.logical
                )
            ),
            RuntimeAttributeValue.Insets(
                RuntimeInsets(1.0, 2.0, 3.0, 4.0, RuntimeMeasurementUnit.logical)
            ),
            RuntimeAttributeValue.Color(RuntimeColor("srgb", 0.1, 0.2, 0.3, 1.0)),
            RuntimeAttributeValue.ArrayValue(JsonArray(listOf(JsonPrimitive("item")))),
            RuntimeAttributeValue.ObjectValue(JsonObject(mapOf("key" to JsonPrimitive(true)))),
            RuntimeAttributeValue.Extension(
                RuntimeNamespacedIdentifier("vendor.custom.value"),
                JsonPrimitive("opaque")
            )
        )

        values.forEach { value ->
            val encoded = codec.encodeValue(value, RuntimeAttributeValue.serializer())
            assertEquals(value, codec.decodeValue(encoded, RuntimeAttributeValue.serializer()))
        }
    }

    private fun fixture(path: String): ByteArray = checkNotNull(
        javaClass.classLoader.getResource(path)
    ).readBytes()
}
