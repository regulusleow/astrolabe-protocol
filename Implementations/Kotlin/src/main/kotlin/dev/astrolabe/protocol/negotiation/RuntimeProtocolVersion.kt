//
//  RuntimeProtocolVersion.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Wire protocol version carried by each request and response. */
@Serializable
public data class RuntimeProtocolVersion(
    /** Protocol major version. */
    public val major: Int,
    /** Protocol minor version. */
    public val minor: Int
) : Comparable<RuntimeProtocolVersion> {
    init {
        require(major in 0..MAXIMUM_VERSION_COMPONENT) { "Protocol major version is outside UInt16 range" }
        require(minor in 0..MAXIMUM_VERSION_COMPONENT) { "Protocol minor version is outside UInt16 range" }
    }

    /** Whether this value is the only protocol version implemented by this package. */
    public val isSupported: Boolean
        get() = this == V2

    override fun compareTo(other: RuntimeProtocolVersion): Int =
        compareValuesBy(this, other, RuntimeProtocolVersion::major, RuntimeProtocolVersion::minor)

    public companion object {
        /** Astrolabe Wire Protocol 2.0. */
        public val V2: RuntimeProtocolVersion = RuntimeProtocolVersion(major = 2, minor = 0)
    }
}

/** Inclusive protocol version interval supported by one peer. */
@Serializable
public data class RuntimeProtocolRange(
    /** Oldest protocol version accepted by the peer. */
    public val minimum: RuntimeProtocolVersion,
    /** Newest protocol version accepted by the peer. */
    public val maximum: RuntimeProtocolVersion
) {
    init {
        require(minimum.major == maximum.major) { "Protocol range cannot span major versions" }
        require(minimum <= maximum) { "Protocol range minimum cannot exceed maximum" }
    }

    /** Returns whether this range includes the supplied version. */
    public operator fun contains(version: RuntimeProtocolVersion): Boolean = version in minimum..maximum

    /** Returns the newest version accepted by both ranges, or null when they do not overlap. */
    public fun highestCommonVersion(other: RuntimeProtocolRange): RuntimeProtocolVersion? {
        val lowerBound = maxOf(minimum, other.minimum)
        val upperBound = minOf(maximum, other.maximum)
        return upperBound.takeIf { lowerBound <= it }
    }

    public companion object {
        /** Range containing Astrolabe Wire Protocol 2.0 only. */
        public val V2: RuntimeProtocolRange = RuntimeProtocolRange(
            minimum = RuntimeProtocolVersion.V2,
            maximum = RuntimeProtocolVersion.V2
        )
    }
}

private const val MAXIMUM_VERSION_COMPONENT: Int = 65_535
