//
//  RuntimeMessageCodec.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Encodes and decodes platform-neutral Astrolabe wire envelopes. */
public class RuntimeMessageCodec {
    private val json = Json {
        encodeDefaults = false
        explicitNulls = true
        ignoreUnknownKeys = true
    }
    private val documentValidator = RuntimeJsonDocumentValidator(json)

    /** Decodes any valid JSON document without applying method-specific typing. */
    public fun decodeDocument(data: ByteArray): JsonElement = parseDocument(data)

    /** Decodes a standalone JSON value through an explicit wire serializer. */
    public fun <T> decodeValue(data: ByteArray, serializer: KSerializer<T>): T = wrapDecode {
        json.decodeFromJsonElement(serializer, parseDocument(data))
    }

    /** Encodes a standalone value through an explicit wire serializer. */
    public fun <T> encodeValue(value: T, serializer: KSerializer<T>): ByteArray =
        encodeDocument(json.encodeToJsonElement(serializer, value))

    /** Decodes and validates a request envelope. */
    public fun decodeRequest(data: ByteArray): RuntimeRequestEnvelope = wrapDecode {
        val document = requireObject(parseDocument(data))
        val request = json.decodeFromJsonElement<RuntimeRequestEnvelope>(document)
        validateRequest(request)
        request
    }

    /** Encodes a request envelope. */
    public fun encodeRequest(request: RuntimeRequestEnvelope): ByteArray {
        validateRequest(request)
        return encodeDocument(json.encodeToJsonElement(request))
    }

    /** Encodes a typed request without exposing JSON assembly to the caller. */
    public fun <T> encodeRequest(
        requestID: String,
        contract: RuntimeMethodContract<T>,
        parameters: T
    ): ByteArray {
        val document = json.encodeToJsonElement(contract.serializer, parameters)
        val parameterObject = runCatching { document.jsonObject }.getOrElse { error ->
            throw RuntimeMessageException.InvalidEnvelope("Request parameters must encode as an object", error)
        }
        return encodeRequest(
            RuntimeRequestEnvelope(
                requestID = requestID,
                protocolVersion = RuntimeProtocolVersion.V2,
                method = contract.method,
                parameters = parameterObject
            )
        )
    }

    /** Decodes method-specific request parameters from an already validated envelope. */
    public fun <T> decodeRequestParameters(
        request: RuntimeRequestEnvelope,
        contract: RuntimeMethodContract<T>
    ): T = wrapDecode {
        validateMethod(request.method, contract.method)
        json.decodeFromJsonElement(contract.serializer, request.parameters)
    }

    /** Decodes and validates a response envelope. */
    public fun decodeResponse(data: ByteArray): RuntimeResponseEnvelope = wrapDecode {
        val document = requireObject(parseDocument(data))
        val requestID = document.requireString("requestID")
        validateRequestID(requestID)
        val protocolVersion = json.decodeFromJsonElement<RuntimeProtocolVersion>(
            document.requireMember("protocolVersion")
        )
        validateProtocolVersion(protocolVersion)
        val method = RuntimeMethod(document.requireString("method"))

        val outcome = when (document.requireString("status")) {
            "success" -> {
                if ("error" in document || "payload" !in document) {
                    throw RuntimeMessageException.InvalidEnvelope(
                        "Successful response must contain payload and cannot contain error"
                    )
                }
                RuntimeResponseOutcome.Success(document.getValue("payload"))
            }
            "failure" -> {
                if ("payload" in document || "error" !in document) {
                    throw RuntimeMessageException.InvalidEnvelope(
                        "Failed response must contain error and cannot contain payload"
                    )
                }
                RuntimeResponseOutcome.Failure(
                    json.decodeFromJsonElement(document.getValue("error"))
                )
            }
            else -> throw RuntimeMessageException.InvalidEnvelope("Unknown response status")
        }

        RuntimeResponseEnvelope(
            requestID = requestID,
            protocolVersion = protocolVersion,
            method = method,
            outcome = outcome
        )
    }

    /** Encodes a response envelope. */
    public fun encodeResponse(response: RuntimeResponseEnvelope): ByteArray {
        validateRequestID(response.requestID)
        validateProtocolVersion(response.protocolVersion)

        val document = buildJsonObject {
            put("requestID", response.requestID)
            put("protocolVersion", json.encodeToJsonElement(response.protocolVersion))
            put("method", response.method.rawValue)
            when (val outcome = response.outcome) {
                is RuntimeResponseOutcome.Success -> {
                    put("status", "success")
                    put("payload", outcome.payload)
                }
                is RuntimeResponseOutcome.Failure -> {
                    put("status", "failure")
                    put("error", json.encodeToJsonElement(outcome.error))
                }
            }
        }
        return encodeDocument(document)
    }

    /** Encodes a typed successful response without exposing JSON assembly to the caller. */
    public fun <T> encodeSuccessResponse(
        requestID: String,
        contract: RuntimeMethodContract<T>,
        payload: T
    ): ByteArray = encodeResponse(
        RuntimeResponseEnvelope(
            requestID = requestID,
            protocolVersion = RuntimeProtocolVersion.V2,
            method = contract.method,
            outcome = RuntimeResponseOutcome.Success(
                json.encodeToJsonElement(contract.serializer, payload)
            )
        )
    )

    /** Encodes a structured failure for one method. */
    public fun encodeFailureResponse(
        requestID: String,
        method: RuntimeMethod,
        error: RuntimeError
    ): ByteArray = encodeResponse(
        RuntimeResponseEnvelope(
            requestID = requestID,
            protocolVersion = RuntimeProtocolVersion.V2,
            method = method,
            outcome = RuntimeResponseOutcome.Failure(error)
        )
    )

    /** Decodes the successful payload from an already validated response envelope. */
    public fun <T> decodeSuccessPayload(
        response: RuntimeResponseEnvelope,
        contract: RuntimeMethodContract<T>
    ): T {
        validateMethod(response.method, contract.method)
        val outcome = response.outcome as? RuntimeResponseOutcome.Success
            ?: throw RuntimeMessageException.InvalidEnvelope("Response does not contain a success payload")
        return wrapDecode {
            json.decodeFromJsonElement(contract.serializer, outcome.payload)
        }
    }

    private fun parseDocument(data: ByteArray): JsonElement = wrapDecode {
        documentValidator.validate(data)
        json.parseToJsonElement(data.decodeToString())
    }

    private fun encodeDocument(document: JsonElement): ByteArray = wrapEncode {
        val data = json.encodeToString(document).encodeToByteArray()
        documentValidator.validate(data)
        data
    }

    private fun requireObject(document: JsonElement): JsonObject =
        runCatching { document.jsonObject }.getOrElse { error ->
            throw RuntimeMessageException.InvalidDocument("Wire message root must be an object", error)
        }

    private fun validateRequest(request: RuntimeRequestEnvelope) {
        validateRequestID(request.requestID)
        validateProtocolVersion(request.protocolVersion)
    }

    private fun validateRequestID(requestID: String) {
        runCatching { UUID.fromString(requestID) }.getOrElse { error ->
            throw RuntimeMessageException.InvalidEnvelope("requestID must be a UUID", error)
        }
    }

    private fun validateProtocolVersion(version: RuntimeProtocolVersion) {
        if (!version.isSupported) {
            throw RuntimeMessageException.UnsupportedProtocolVersion(version)
        }
    }

    private fun validateMethod(actual: RuntimeMethod, expected: RuntimeMethod) {
        if (actual != expected) {
            throw RuntimeMessageException.MethodMismatch(expected = expected, actual = actual)
        }
    }

    private fun JsonObject.requireMember(name: String): JsonElement =
        this[name] ?: throw RuntimeMessageException.InvalidEnvelope("Missing required member: $name")

    private fun JsonObject.requireString(name: String): String {
        val primitive = requireMember(name) as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            throw RuntimeMessageException.InvalidEnvelope("Member $name must be a string")
        }
        return primitive.content
    }

    private inline fun <T> wrapDecode(operation: () -> T): T = try {
        operation()
    } catch (error: RuntimeMessageException) {
        throw error
    } catch (error: SerializationException) {
        throw RuntimeMessageException.InvalidDocument("Unable to decode JSON document", error)
    } catch (error: IllegalArgumentException) {
        throw RuntimeMessageException.InvalidDocument("Unable to decode JSON document", error)
    }

    private inline fun <T> wrapEncode(operation: () -> T): T = try {
        operation()
    } catch (error: RuntimeMessageException) {
        throw error
    } catch (error: SerializationException) {
        throw RuntimeMessageException.InvalidDocument("Unable to encode JSON document", error)
    } catch (error: IllegalArgumentException) {
        throw RuntimeMessageException.InvalidDocument("Unable to encode JSON document", error)
    }
}

/** Failures produced while reading or writing a wire message. */
public sealed class RuntimeMessageException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    /** The bytes do not contain an accepted JSON document. */
    public class InvalidDocument(message: String, cause: Throwable? = null) :
        RuntimeMessageException(message, cause)

    /** Required envelope members are missing, malformed, or inconsistent. */
    public class InvalidEnvelope(message: String, cause: Throwable? = null) :
        RuntimeMessageException(message, cause)

    /** The message uses a protocol version this package cannot decode. */
    public class UnsupportedProtocolVersion(
        /** Unsupported version read from the message. */
        public val version: RuntimeProtocolVersion
    ) : RuntimeMessageException("Unsupported protocol version: ${version.major}.${version.minor}")

    /** A typed contract was used with a different wire method. */
    public class MethodMismatch(
        /** Method required by the typed contract. */
        public val expected: RuntimeMethod,
        /** Method present in the decoded envelope. */
        public val actual: RuntimeMethod
    ) : RuntimeMessageException(
        "Method mismatch: expected ${expected.rawValue}, received ${actual.rawValue}"
    )
}
