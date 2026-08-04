//
//  RuntimeCancellation.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import java.util.UUID
import kotlinx.serialization.Serializable

/** Parameters for cancelling one in-flight request. */
@Serializable
public data class RuntimeCancelRequestParameters(
    /** Request identifier whose operation should be cancelled. */
    public val targetRequestID: String
) {
    init {
        requireValidRequestIdentifier(targetRequestID)
    }

    public companion object {
        /** Typed request contract for the cancellation method. */
        public val contract: RuntimeMethodContract<RuntimeCancelRequestParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.cancelRequest, serializer())
        }
    }
}

/** Successful cancellation response payload. */
@Serializable
public data class RuntimeCancelRequestPayload(
    /** Request identifier supplied by the cancellation request. */
    public val targetRequestID: String,
    /** Whether an active operation accepted cancellation. */
    public val cancellationAccepted: Boolean
) {
    init {
        requireValidRequestIdentifier(targetRequestID)
    }

    public companion object {
        /** Typed success-payload contract for the cancellation method. */
        public val contract: RuntimeMethodContract<RuntimeCancelRequestPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.cancelRequest, serializer())
        }
    }
}

private fun requireValidRequestIdentifier(value: String) {
    require(runCatching { UUID.fromString(value) }.isSuccess) { "Request identifier must be a UUID" }
}
