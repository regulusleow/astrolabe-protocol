//
//  RuntimeApplication.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Platform-neutral application identity. */
@Serializable
public data class RuntimeApplication(
    /** Platform-neutral application identifier. */
    public val identifier: String,
    /** User-visible application name. */
    public val displayName: String,
    /** Application release version when available. */
    public val version: String?,
    /** Application build version when available. */
    public val buildVersion: String?
)

/** Inspected process instance. */
@Serializable
public data class RuntimeTarget(
    /** Opaque process-instance identifier. */
    public val identifier: RuntimeOpaqueIdentifier,
    /** Platform process identifier represented as an opaque string. */
    public val processIdentifier: String?,
    /** Open target-kind identifier. */
    public val kind: String,
    /** Whether this is the Runtime's primary inspection target. */
    public val primary: Boolean
)

/** Effective interface layout direction. */
@Serializable
public enum class RuntimeLayoutDirection {
    leftToRight,
    rightToLeft,
    unknown
}

/** Platform, device, locale, and display facts. */
@Serializable
public data class RuntimeEnvironment(
    /** Platform family reported by the Runtime. */
    public val platform: String,
    /** Operating-system release version. */
    public val operatingSystemVersion: String,
    /** Open device-category identifier. */
    public val deviceCategory: String,
    /** User-visible device name when available. */
    public val deviceName: String?,
    /** Hardware or virtual-device model when available. */
    public val deviceModel: String?,
    /** Whether the target runs on a virtual device. */
    public val virtualDevice: Boolean,
    /** Active locale identifier when available. */
    public val locale: String?,
    /** Effective interface layout direction. */
    public val layoutDirection: RuntimeLayoutDirection,
    /** Display facts required for coordinate conversion. */
    public val display: RuntimeDisplayInfo,
    /** Optional namespaced platform facts. */
    public val extensions: RuntimeExtensionMap? = null
)

/** Successful application-info response payload. */
@Serializable
public data class RuntimeApplicationInfoPayload(
    /** Application identity and release metadata. */
    public val application: RuntimeApplication,
    /** Inspected process instance. */
    public val target: RuntimeTarget,
    /** Platform, device, locale, and display facts. */
    public val environment: RuntimeEnvironment,
    /** Optional namespaced application facts. */
    public val extensions: RuntimeExtensionMap? = null
) {
    public companion object {
        /** Typed success-payload contract for the application-info method. */
        public val contract: RuntimeMethodContract<RuntimeApplicationInfoPayload> by lazy {
            RuntimeMethodContract(RuntimeMethod.applicationInfo, serializer())
        }
    }
}

/** Empty parameters for the application-info method. */
@Serializable
public class RuntimeApplicationInfoParameters {
    public companion object {
        /** Typed request contract for the application-info method. */
        public val contract: RuntimeMethodContract<RuntimeApplicationInfoParameters> by lazy {
            RuntimeMethodContract(RuntimeMethod.applicationInfo, serializer())
        }
    }

    override fun equals(other: Any?): Boolean = other is RuntimeApplicationInfoParameters

    override fun hashCode(): Int = javaClass.hashCode()
}
