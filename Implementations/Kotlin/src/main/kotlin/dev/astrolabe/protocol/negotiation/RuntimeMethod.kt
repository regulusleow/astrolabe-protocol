//
//  RuntimeMethod.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/** Open wire method identifier. */
@JvmInline
@Serializable
public value class RuntimeMethod(
    /** Raw method value sent on the wire. */
    public val rawValue: String
) {
    init {
        require(rawValue.isNotEmpty() && rawValue.length <= MAXIMUM_METHOD_LENGTH) {
            "Runtime method must contain between 1 and 256 characters"
        }
    }

    public companion object {
        /** Opens a protocol session and negotiates capabilities. */
        public val handshake: RuntimeMethod = RuntimeMethod("handshake")
        /** Returns application, target, environment, and display facts. */
        public val applicationInfo: RuntimeMethod = RuntimeMethod("applicationInfo")
        /** Captures the runtime hierarchy. */
        public val hierarchySnapshot: RuntimeMethod = RuntimeMethod("hierarchySnapshot")
        /** Returns detailed facts for one node. */
        public val nodeDetail: RuntimeMethod = RuntimeMethod("nodeDetail")
        /** Describes attributes accepted by the patch runtime. */
        public val patchableAttributes: RuntimeMethod = RuntimeMethod("patchableAttributes")
        /** Applies one temporary attribute patch. */
        public val applyAttributePatch: RuntimeMethod = RuntimeMethod("applyAttributePatch")
        /** Lists currently active attribute patches. */
        public val listAttributePatches: RuntimeMethod = RuntimeMethod("listAttributePatches")
        /** Reverts one active attribute patch. */
        public val revertAttributePatch: RuntimeMethod = RuntimeMethod("revertAttributePatch")
        /** Reverts every active attribute patch. */
        public val clearAttributePatches: RuntimeMethod = RuntimeMethod("clearAttributePatches")
        /** Attempts to cancel one in-flight request. */
        public val cancelRequest: RuntimeMethod = RuntimeMethod("cancelRequest")
    }
}

/** Binds one method identifier to the serializer for its parameters or payload. */
public class RuntimeMethodContract<T>(
    /** Method accepted by this contract. */
    public val method: RuntimeMethod,
    /** Serializer for the method-specific value. */
    public val serializer: KSerializer<T>
)

private const val MAXIMUM_METHOD_LENGTH: Int = 256
