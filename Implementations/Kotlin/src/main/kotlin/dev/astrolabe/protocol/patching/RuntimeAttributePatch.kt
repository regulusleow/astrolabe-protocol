//
//  RuntimeAttributePatch.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Parameters for applying one temporary attribute patch. */
@Serializable
public data class RuntimeApplyAttributePatchParameters(
    /** Node receiving the temporary patch. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Namespaced attribute to patch. */
    public val attributeIdentifier: RuntimeAttributeIdentifier,
    /** Requested typed value. */
    public val value: RuntimeAttributeValue
) {
    public companion object {
        /** Typed request contract for applying an attribute patch. */
        public val contract: RuntimeMethodContract<RuntimeApplyAttributePatchParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.applyAttributePatch, serializer())
        }
    }
}

/** Active temporary attribute patch. */
@Serializable
public data class RuntimeAttributePatch(
    /** Opaque identifier for this active patch. */
    public val patchID: RuntimeOpaqueIdentifier,
    /** Patched node identifier. */
    public val nodeID: RuntimeOpaqueIdentifier,
    /** Patched namespaced attribute. */
    public val attributeIdentifier: RuntimeAttributeIdentifier,
    /** Value captured before the first patch when available. */
    public val originalValue: RuntimeAttributeValue?,
    /** Value requested by the Host. */
    public val requestedValue: RuntimeAttributeValue,
    /** Value observed after applying the patch when available. */
    public val actualValue: RuntimeAttributeValue?,
    /** Apply time in seconds since the Unix epoch. */
    public val appliedAtUnixTime: Double
) {
    init {
        require(appliedAtUnixTime >= 0.0) { "Patch apply time cannot be negative" }
    }

    public companion object {
        /** Typed success-payload contract for applying an attribute patch. */
        public val applyContract: RuntimeMethodContract<RuntimeAttributePatch> by lazy {
            RuntimeMethodContract(RuntimeMethod.applyAttributePatch, serializer())
        }
    }
}

/** Empty parameters for listing active attribute patches. */
@Serializable
public data object RuntimeListAttributePatchesParameters {
    /** Typed request contract for listing active attribute patches. */
    public val contract: RuntimeMethodContract<RuntimeListAttributePatchesParameters> by lazy {
        RuntimeMethodContract(RuntimeMethod.listAttributePatches, serializer())
    }
}

/** Successful list-attribute-patches response payload. */
@Serializable
public data class RuntimeAttributePatchListPayload(
    /** Active temporary patches in Runtime-defined order. */
    public val patches: List<RuntimeAttributePatch>
) {
    public companion object {
        /** Typed success-payload contract for listing attribute patches. */
        public val contract: RuntimeMethodContract<RuntimeAttributePatchListPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.listAttributePatches, serializer())
        }
    }
}

/** Parameters for reverting one active attribute patch. */
@Serializable
public data class RuntimeRevertAttributePatchParameters(
    /** Active patch to revert. */
    public val patchID: RuntimeOpaqueIdentifier
) {
    public companion object {
        /** Typed request contract for reverting an attribute patch. */
        public val contract: RuntimeMethodContract<RuntimeRevertAttributePatchParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.revertAttributePatch, serializer())
        }
    }
}

/** Successful revert-attribute-patch response payload. */
@Serializable
public data class RuntimeRevertAttributePatchPayload(
    /** Identifier of the reverted patch. */
    public val revertedPatchID: RuntimeOpaqueIdentifier,
    /** Value restored by the Runtime when available. */
    public val restoredValue: RuntimeAttributeValue?,
    /** Number of patches remaining after the operation. */
    public val remainingPatchCount: Int
) {
    init {
        require(remainingPatchCount >= 0) { "Remaining patch count cannot be negative" }
    }

    public companion object {
        /** Typed success-payload contract for reverting an attribute patch. */
        public val contract: RuntimeMethodContract<RuntimeRevertAttributePatchPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.revertAttributePatch, serializer())
        }
    }
}

/** Empty parameters for clearing all active attribute patches. */
@Serializable
public data object RuntimeClearAttributePatchesParameters {
    /** Typed request contract for clearing attribute patches. */
    public val contract: RuntimeMethodContract<RuntimeClearAttributePatchesParameters> by lazy {
        RuntimeMethodContract(RuntimeMethod.clearAttributePatches, serializer())
    }
}

/** Successful clear-attribute-patches response payload. */
@Serializable
public data class RuntimeClearAttributePatchesPayload(
    /** Patch identifiers successfully reverted by the clear operation. */
    public val revertedPatchIDs: List<RuntimeOpaqueIdentifier>,
    /** Number of patches remaining after all revert attempts. */
    public val remainingPatchCount: Int
) {
    init {
        require(remainingPatchCount >= 0) { "Remaining patch count cannot be negative" }
        require(revertedPatchIDs.distinct().size == revertedPatchIDs.size) {
            "Reverted patch identifiers cannot contain duplicates"
        }
    }

    public companion object {
        /** Typed success-payload contract for clearing attribute patches. */
        public val contract: RuntimeMethodContract<RuntimeClearAttributePatchesPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.clearAttributePatches, serializer())
        }
    }
}
