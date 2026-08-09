//
//  RuntimeMessage.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Open runtime error code. */
@JvmInline
@Serializable
public value class RuntimeErrorCode(
    /** Raw error code sent on the wire. */
    public val rawValue: String
) {
    init {
        require(isCommonRuntimeErrorCode(rawValue) || RuntimeNamespacedIdentifier.isValid(rawValue)) {
            "Runtime error code must be built-in or namespaced"
        }
    }

    public companion object {
        /** The frame header or payload is malformed. */
        public val malformedFrame: RuntimeErrorCode = RuntimeErrorCode("malformedFrame")
        /** The declared frame payload exceeds the accepted limit. */
        public val frameTooLarge: RuntimeErrorCode = RuntimeErrorCode("frameTooLarge")
        /** The decoded message does not satisfy the wire contract. */
        public val malformedMessage: RuntimeErrorCode = RuntimeErrorCode("malformedMessage")
        /** The peer requested an unsupported protocol version. */
        public val unsupportedProtocolVersion: RuntimeErrorCode = RuntimeErrorCode("unsupportedProtocolVersion")
        /** The peer requested an unsupported method. */
        public val unsupportedMethod: RuntimeErrorCode = RuntimeErrorCode("unsupportedMethod")
        /** The request requires a completed handshake. */
        public val handshakeRequired: RuntimeErrorCode = RuntimeErrorCode("handshakeRequired")
        /** The request requires an unavailable capability. */
        public val capabilityUnavailable: RuntimeErrorCode = RuntimeErrorCode("capabilityUnavailable")
        /** Method parameters are invalid. */
        public val invalidParameters: RuntimeErrorCode = RuntimeErrorCode("invalidParameters")
        /** The requested node no longer exists. */
        public val nodeNotFound: RuntimeErrorCode = RuntimeErrorCode("nodeNotFound")
        /** The requested attribute cannot be patched. */
        public val unsupportedAttribute: RuntimeErrorCode = RuntimeErrorCode("unsupportedAttribute")
        /** The supplied attribute value is invalid. */
        public val invalidAttributeValue: RuntimeErrorCode = RuntimeErrorCode("invalidAttributeValue")
        /** The requested patch does not exist. */
        public val patchNotFound: RuntimeErrorCode = RuntimeErrorCode("patchNotFound")
        /** The patch conflicts with current runtime state. */
        public val patchConflict: RuntimeErrorCode = RuntimeErrorCode("patchConflict")
        /** The original value could not be restored. */
        public val patchRestorationFailed: RuntimeErrorCode = RuntimeErrorCode("patchRestorationFailed")
        /** The runtime cannot accept more concurrent work. */
        public val tooManyRequests: RuntimeErrorCode = RuntimeErrorCode("tooManyRequests")
        /** The request was cancelled. */
        public val requestCancelled: RuntimeErrorCode = RuntimeErrorCode("requestCancelled")
        /** The request exceeded its execution deadline. */
        public val requestTimedOut: RuntimeErrorCode = RuntimeErrorCode("requestTimedOut")
        /** The runtime failed for an implementation-specific reason. */
        public val internalFailure: RuntimeErrorCode = RuntimeErrorCode("internalFailure")
    }
}

private fun isCommonRuntimeErrorCode(value: String): Boolean = when (value) {
    "malformedFrame",
    "frameTooLarge",
    "malformedMessage",
    "unsupportedProtocolVersion",
    "unsupportedMethod",
    "handshakeRequired",
    "capabilityUnavailable",
    "invalidParameters",
    "nodeNotFound",
    "unsupportedAttribute",
    "invalidAttributeValue",
    "patchNotFound",
    "patchConflict",
    "patchRestorationFailed",
    "tooManyRequests",
    "requestCancelled",
    "requestTimedOut",
    "internalFailure" -> true
    else -> false
}

/** Structured protocol failure. */
@Serializable
public data class RuntimeError(
    /** Machine-readable error code. */
    public val code: RuntimeErrorCode,
    /** Human-readable failure description. */
    public val message: String,
    /** Optional action that can resolve the failure. */
    public val recoverySuggestion: String?,
    /** Optional structured diagnostic values. */
    public val details: JsonObject? = null,
    /** Optional namespaced error extensions. */
    public val extensions: RuntimeExtensionMap? = null
)

/** Platform-neutral request envelope. */
@Serializable
public data class RuntimeRequestEnvelope(
    /** UUID correlating this request with its response. */
    public val requestID: String,
    /** Protocol version used by this message. */
    public val protocolVersion: RuntimeProtocolVersion,
    /** Operation requested from the Runtime. */
    public val method: RuntimeMethod,
    /** Method-specific parameters preserved as a JSON object. */
    public val parameters: JsonObject
)

/** Platform-neutral response envelope. */
public data class RuntimeResponseEnvelope(
    /** UUID copied from the request. */
    public val requestID: String,
    /** Protocol version used by this message. */
    public val protocolVersion: RuntimeProtocolVersion,
    /** Method copied from the request. */
    public val method: RuntimeMethod,
    /** Successful payload or structured failure. */
    public val outcome: RuntimeResponseOutcome
)

/** Mutually exclusive response result. */
public sealed interface RuntimeResponseOutcome {
    /** Successful response payload. */
    public data class Success(
        /** Method-specific payload. */
        public val payload: JsonElement
    ) : RuntimeResponseOutcome

    /** Failed response error. */
    public data class Failure(
        /** Structured protocol error. */
        public val error: RuntimeError
    ) : RuntimeResponseOutcome
}
