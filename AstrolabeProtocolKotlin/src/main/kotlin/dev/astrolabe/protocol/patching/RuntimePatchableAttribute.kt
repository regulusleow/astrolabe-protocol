//
//  RuntimePatchableAttribute.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Common or namespaced attribute-value type accepted by a patch. */
@JvmInline
@Serializable
public value class RuntimePatchValueType(
    /** Raw value-type identifier. */
    public val rawValue: String
) {
    init {
        require(rawValue in COMMON_PATCH_VALUE_TYPES || RuntimeNamespacedIdentifier.isValid(rawValue)) {
            "Patch value type must be common or namespaced"
        }
    }
}

/** Optional value-level constraints for one patchable attribute. */
@Serializable
public data class RuntimePatchValueConstraints(
    /** Inclusive or exclusive numeric lower bound when present. */
    public val minimum: Double?,
    /** Inclusive or exclusive numeric upper bound when present. */
    public val maximum: Double?,
    /** Whether the minimum bound is exclusive. */
    public val minimumExclusive: Boolean,
    /** Whether the maximum bound is exclusive. */
    public val maximumExclusive: Boolean,
    /** Accepted producer-defined string formats. */
    public val acceptedFormats: List<String>,
    /** Explicit values accepted by the Runtime. */
    public val allowedValues: List<RuntimeAttributeValue>
) {
    init {
        require(minimum == null || maximum == null || minimum <= maximum) {
            "Patch value minimum cannot exceed maximum"
        }
        require(acceptedFormats.all(String::isNotEmpty)) { "Accepted patch formats cannot be empty" }
        require(acceptedFormats.distinct().size == acceptedFormats.size) {
            "Accepted patch formats cannot contain duplicates"
        }
    }
}

/** Runtime-owned description of one patchable attribute family. */
@Serializable
public data class RuntimePatchableAttribute(
    /** Namespaced attribute path or placeholder pattern. */
    public val attributePattern: String,
    /** Value type accepted by the attribute. */
    public val valueType: RuntimePatchValueType,
    /** Semantic node roles eligible for the patch. */
    public val targetRoles: List<String>,
    /** Optional value-level constraints. */
    public val valueConstraints: RuntimePatchValueConstraints?,
    /** Namespaced platform-specific applicability facts. */
    public val extensions: RuntimeExtensionMap
) {
    init {
        require(
            attributePattern.length <= MAXIMUM_PATCH_ATTRIBUTE_PATTERN_LENGTH &&
                PATCH_ATTRIBUTE_PATTERN.matches(attributePattern)
        ) {
            "Patch attribute pattern must be namespaced"
        }
        require(targetRoles.all { it.isNotEmpty() && it.length <= MAXIMUM_PATCH_ROLE_LENGTH }) {
            "Patch target roles must contain between 1 and 128 characters"
        }
        require(targetRoles.distinct().size == targetRoles.size) {
            "Patch target roles cannot contain duplicates"
        }
    }
}

/** Empty parameters for discovering patchable attributes. */
@Serializable
public data object RuntimePatchableAttributesParameters {
    /** Typed request contract for the patchable-attributes method. */
    public val contract: RuntimeMethodContract<RuntimePatchableAttributesParameters> by lazy {
        RuntimeMethodContract(RuntimeMethod.patchableAttributes, serializer())
    }
}

/** Successful patchable-attributes response payload. */
@Serializable
public data class RuntimePatchableAttributesPayload(
    /** Runtime-owned catalog of patchable attributes. */
    public val attributes: List<RuntimePatchableAttribute>
) {
    public companion object {
        /** Typed success-payload contract for the patchable-attributes method. */
        public val contract: RuntimeMethodContract<RuntimePatchableAttributesPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.patchableAttributes, serializer())
        }
    }
}

private val COMMON_PATCH_VALUE_TYPES: Set<String> = setOf(
    "null",
    "boolean",
    "integer",
    "number",
    "string",
    "stringList",
    "measurement",
    "point",
    "size",
    "vector",
    "rect",
    "insets",
    "color",
    "textRuns",
    "layoutRelations",
    "array",
    "object"
)
private val PATCH_ATTRIBUTE_PATTERN: Regex =
    Regex("^[a-z][a-z0-9-]*(\\.(?:[A-Za-z][A-Za-z0-9_-]*|<[A-Za-z][A-Za-z0-9_-]*>))+$")
private const val MAXIMUM_PATCH_ATTRIBUTE_PATTERN_LENGTH: Int = 256
private const val MAXIMUM_PATCH_ROLE_LENGTH: Int = 128
