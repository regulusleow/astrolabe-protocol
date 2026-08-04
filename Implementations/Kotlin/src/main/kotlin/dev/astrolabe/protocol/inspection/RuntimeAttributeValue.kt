//
//  RuntimeAttributeValue.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/** Closed common attribute values plus namespaced extension values. */
@Serializable(with = RuntimeAttributeValueSerializer::class)
public sealed interface RuntimeAttributeValue {
    /** Explicit JSON null. */
    public data object Null : RuntimeAttributeValue

    /** Boolean attribute value. */
    public data class BooleanValue(
        /** Wrapped Boolean. */
        public val value: Boolean
    ) : RuntimeAttributeValue

    /** JSON-safe signed integer attribute value. */
    public data class Integer(
        /** Wrapped integer. */
        public val value: Long
    ) : RuntimeAttributeValue {
        init {
            require(value in MINIMUM_SAFE_INTEGER..MAXIMUM_SAFE_INTEGER) {
                "Integer attribute value exceeds the JSON safe range"
            }
        }
    }

    /** Finite floating-point attribute value. */
    public data class Number(
        /** Wrapped number. */
        public val value: Double
    ) : RuntimeAttributeValue {
        init {
            require(value.isFinite()) { "Number attribute value must be finite" }
        }
    }

    /** String attribute value. */
    public data class StringValue(
        /** Wrapped string. */
        public val value: String
    ) : RuntimeAttributeValue

    /** String-list attribute value. */
    public data class StringList(
        /** Wrapped strings. */
        public val value: List<String>
    ) : RuntimeAttributeValue

    /** Scalar measurement attribute value. */
    public data class Measurement(
        /** Wrapped measurement. */
        public val value: RuntimeMeasurement
    ) : RuntimeAttributeValue

    /** Coordinate point attribute value. */
    public data class Point(
        /** Wrapped point. */
        public val value: RuntimeCoordinatePoint
    ) : RuntimeAttributeValue

    /** Measured-size attribute value. */
    public data class Size(
        /** Wrapped size. */
        public val value: RuntimeMeasuredSize
    ) : RuntimeAttributeValue

    /** Signed vector attribute value. */
    public data class Vector(
        /** Wrapped vector. */
        public val value: RuntimeVector
    ) : RuntimeAttributeValue

    /** Coordinate rectangle attribute value. */
    public data class Rect(
        /** Wrapped rectangle. */
        public val value: RuntimeCoordinateRect
    ) : RuntimeAttributeValue

    /** Insets attribute value. */
    public data class Insets(
        /** Wrapped insets. */
        public val value: RuntimeInsets
    ) : RuntimeAttributeValue

    /** Color attribute value. */
    public data class Color(
        /** Wrapped color. */
        public val value: RuntimeColor
    ) : RuntimeAttributeValue

    /** Attributed text-run value. */
    public data class TextRuns(
        /** Wrapped text runs. */
        public val value: List<RuntimeTextRun>
    ) : RuntimeAttributeValue

    /** Layout-relation value. */
    public data class LayoutRelations(
        /** Wrapped layout relations. */
        public val value: List<RuntimeLayoutRelation>
    ) : RuntimeAttributeValue

    /** Arbitrary JSON array value. */
    public data class ArrayValue(
        /** Wrapped JSON values. */
        public val value: JsonArray
    ) : RuntimeAttributeValue

    /** Arbitrary JSON object value. */
    public data class ObjectValue(
        /** Wrapped JSON members. */
        public val value: JsonObject
    ) : RuntimeAttributeValue

    /** Namespaced producer-defined attribute value. */
    public data class Extension(
        /** Namespaced value type. */
        public val type: RuntimeNamespacedIdentifier,
        /** Opaque JSON value interpreted by the producer. */
        public val value: JsonElement
    ) : RuntimeAttributeValue
}

/** Serializer for the discriminator-based attribute-value wire shape. */
public object RuntimeAttributeValueSerializer : KSerializer<RuntimeAttributeValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RuntimeAttributeValue") {
        element<String>("type")
        element<JsonElement>("value")
    }

    override fun deserialize(decoder: Decoder): RuntimeAttributeValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("RuntimeAttributeValue requires JSON")
        val document = jsonDecoder.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("RuntimeAttributeValue must be an object")
        if (document.keys != EXPECTED_KEYS) {
            throw SerializationException("RuntimeAttributeValue must contain only type and value")
        }
        val typeElement = document.getValue("type") as? JsonPrimitive
            ?: throw SerializationException("RuntimeAttributeValue type must be a string")
        if (!typeElement.isString) {
            throw SerializationException("RuntimeAttributeValue type must be a string")
        }
        val type = typeElement.content
        val value = document.getValue("value")
        return decodeValue(type, value, jsonDecoder)
    }

    override fun serialize(encoder: Encoder, value: RuntimeAttributeValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("RuntimeAttributeValue requires JSON")
        val pair = encodeValue(value, jsonEncoder)
        jsonEncoder.encodeJsonElement(buildJsonObject {
            put("type", pair.first)
            put("value", pair.second)
        })
    }

    private fun decodeValue(
        type: String,
        value: JsonElement,
        decoder: JsonDecoder
    ): RuntimeAttributeValue = when (type) {
        "null" -> {
            if (value !is JsonNull) throw SerializationException("Null attribute value must contain JSON null")
            RuntimeAttributeValue.Null
        }
        "boolean" -> RuntimeAttributeValue.BooleanValue(decoder.json.decodeFromJsonElement(value))
        "integer" -> decodeInteger(value)
        "number" -> RuntimeAttributeValue.Number(decoder.json.decodeFromJsonElement(value))
        "string" -> RuntimeAttributeValue.StringValue(decoder.json.decodeFromJsonElement(value))
        "stringList" -> RuntimeAttributeValue.StringList(decoder.json.decodeFromJsonElement(value))
        "measurement" -> RuntimeAttributeValue.Measurement(decoder.json.decodeFromJsonElement(value))
        "point" -> RuntimeAttributeValue.Point(decoder.json.decodeFromJsonElement(value))
        "size" -> RuntimeAttributeValue.Size(decoder.json.decodeFromJsonElement(value))
        "vector" -> RuntimeAttributeValue.Vector(decoder.json.decodeFromJsonElement(value))
        "rect" -> RuntimeAttributeValue.Rect(decoder.json.decodeFromJsonElement(value))
        "insets" -> RuntimeAttributeValue.Insets(decoder.json.decodeFromJsonElement(value))
        "color" -> RuntimeAttributeValue.Color(decoder.json.decodeFromJsonElement(value))
        "textRuns" -> RuntimeAttributeValue.TextRuns(decoder.json.decodeFromJsonElement(value))
        "layoutRelations" -> RuntimeAttributeValue.LayoutRelations(decoder.json.decodeFromJsonElement(value))
        "array" -> RuntimeAttributeValue.ArrayValue(value as? JsonArray
            ?: throw SerializationException("Array attribute value must contain a JSON array"))
        "object" -> RuntimeAttributeValue.ObjectValue(value as? JsonObject
            ?: throw SerializationException("Object attribute value must contain a JSON object"))
        else -> RuntimeAttributeValue.Extension(RuntimeNamespacedIdentifier(type), value)
    }

    private fun decodeInteger(value: JsonElement): RuntimeAttributeValue.Integer {
        val primitive = value as? JsonPrimitive
            ?: throw SerializationException("Integer attribute value must contain a JSON number")
        if (primitive.isString) {
            throw SerializationException("Integer attribute value must contain a JSON number")
        }
        val number = try {
            BigDecimal(primitive.content)
        } catch (error: NumberFormatException) {
            throw SerializationException("Integer attribute value must contain an integer", error)
        }
        if (number.stripTrailingZeros().scale() > 0) {
            throw SerializationException("Integer attribute value must contain an integer")
        }
        val integer = try {
            number.longValueExact()
        } catch (error: ArithmeticException) {
            throw SerializationException("Integer attribute value exceeds the signed 64-bit range", error)
        }
        return RuntimeAttributeValue.Integer(integer)
    }

    private fun encodeValue(
        value: RuntimeAttributeValue,
        encoder: JsonEncoder
    ): Pair<String, JsonElement> = when (value) {
        RuntimeAttributeValue.Null -> "null" to JsonNull
        is RuntimeAttributeValue.BooleanValue -> "boolean" to JsonPrimitive(value.value)
        is RuntimeAttributeValue.Integer -> "integer" to JsonPrimitive(value.value)
        is RuntimeAttributeValue.Number -> "number" to JsonPrimitive(value.value)
        is RuntimeAttributeValue.StringValue -> "string" to JsonPrimitive(value.value)
        is RuntimeAttributeValue.StringList ->
            "stringList" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Measurement ->
            "measurement" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Point -> "point" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Size -> "size" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Vector -> "vector" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Rect -> "rect" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Insets -> "insets" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.Color -> "color" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.TextRuns ->
            "textRuns" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.LayoutRelations ->
            "layoutRelations" to encoder.json.encodeToJsonElement(value.value)
        is RuntimeAttributeValue.ArrayValue -> "array" to value.value
        is RuntimeAttributeValue.ObjectValue -> "object" to value.value
        is RuntimeAttributeValue.Extension -> value.type.rawValue to value.value
    }

    private val EXPECTED_KEYS: Set<String> = setOf("type", "value")
}

private const val MAXIMUM_SAFE_INTEGER: Long = 9_007_199_254_740_991
private const val MINIMUM_SAFE_INTEGER: Long = -MAXIMUM_SAFE_INTEGER
