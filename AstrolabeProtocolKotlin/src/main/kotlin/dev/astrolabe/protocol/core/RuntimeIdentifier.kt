//
//  RuntimeIdentifier.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Non-empty wire identifier whose internal format is owned by its producer. */
@JvmInline
@Serializable
public value class RuntimeOpaqueIdentifier(
    /** Raw identifier value. */
    public val rawValue: String
) {
    init {
        require(rawValue.isNotEmpty() && rawValue.length <= MAXIMUM_IDENTIFIER_LENGTH) {
            "Opaque identifier must contain between 1 and 256 characters"
        }
    }
}

/** Dot-separated identifier that declares its owning namespace. */
@JvmInline
@Serializable
public value class RuntimeNamespacedIdentifier(
    /** Raw namespaced identifier value. */
    public val rawValue: String
) {
    init {
        require(isValid(rawValue)) { "Namespaced identifier has an invalid format" }
    }

    public companion object {
        /** Returns whether a value satisfies the shared namespaced identifier contract. */
        public fun isValid(value: String): Boolean =
            value.length <= MAXIMUM_IDENTIFIER_LENGTH && NAMESPACED_IDENTIFIER_PATTERN.matches(value)
    }
}

/** Namespaced extension members preserved without platform interpretation. */
@JvmInline
@Serializable
public value class RuntimeExtensionMap(
    /** String-keyed extension values. */
    public val values: Map<String, JsonElement>
) {
    init {
        require(values.keys.all(RuntimeNamespacedIdentifier::isValid)) {
            "Runtime extension keys must be namespaced"
        }
    }
}

private const val MAXIMUM_IDENTIFIER_LENGTH: Int = 256
private val NAMESPACED_IDENTIFIER_PATTERN: Regex =
    Regex("^[a-z][a-z0-9-]*(\\.[A-Za-z][A-Za-z0-9_-]*)+$")
