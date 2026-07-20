//
//  RuntimeNodeDetail.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Namespaced semantic or platform attribute identifier. */
@JvmInline
@Serializable
public value class RuntimeAttributeIdentifier(
    /** Raw namespaced attribute identifier. */
    public val rawValue: String
) {
    init {
        require(RuntimeNamespacedIdentifier.isValid(rawValue)) { "Attribute identifier must be namespaced" }
    }
}

/** Namespaced category grouping related detail attributes. */
@JvmInline
@Serializable
public value class RuntimeAttributeCategory(
    /** Raw namespaced category identifier. */
    public val rawValue: String
) {
    init {
        require(RuntimeNamespacedIdentifier.isValid(rawValue)) { "Attribute category must be namespaced" }
    }
}

/** Parameters for reading one node's detail sections. */
@Serializable
public data class RuntimeNodeDetailParameters(
    /** Node whose detail sections are requested. */
    public val nodeID: RuntimeOpaqueIdentifier
) {
    public companion object {
        /** Typed request contract for the node-detail method. */
        public val contract: RuntimeMethodContract<RuntimeNodeDetailParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.nodeDetail, serializer())
        }
    }
}

/** One typed runtime attribute. */
@Serializable
public data class RuntimeAttribute(
    /** Namespaced semantic or platform attribute identifier. */
    public val identifier: RuntimeAttributeIdentifier,
    /** Typed attribute value. */
    public val value: RuntimeAttributeValue
)

/** Ordered attributes belonging to one detail category. */
@Serializable
public data class RuntimeAttributeSection(
    /** Namespaced category grouping related attributes. */
    public val category: RuntimeAttributeCategory,
    /** Attributes collected for this category. */
    public val attributes: List<RuntimeAttribute>
)

/** Successful node-detail response payload. */
@Serializable
public data class RuntimeNodeDetailPayload(
    /** Node represented by these detail sections. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Ordered semantic and platform detail sections. */
    public val sections: List<RuntimeAttributeSection>,
    /** Optional namespaced detail metadata. */
    public val extensions: RuntimeExtensionMap? = null
) {
    public companion object {
        /** Typed success-payload contract for the node-detail method. */
        public val contract: RuntimeMethodContract<RuntimeNodeDetailPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.nodeDetail, serializer())
        }
    }
}
