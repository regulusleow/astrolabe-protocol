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
) {
    init {
        require(identifier.isNotEmpty() && identifier.length <= 512) {
            "Application identifier must contain between 1 and 512 characters"
        }
        require(displayName.isNotEmpty() && displayName.length <= 256) {
            "Application display name must contain between 1 and 256 characters"
        }
        require(version == null || version.length <= 64) { "Application version is too long" }
        require(buildVersion == null || buildVersion.length <= 64) { "Application build version is too long" }
    }
}

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
) {
    init {
        require(processIdentifier == null || processIdentifier.length <= 128) {
            "Process identifier is too long"
        }
        require(kind.isNotEmpty() && kind.length <= 64) {
            "Target kind must contain between 1 and 64 characters"
        }
    }
}

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
) {
    init {
        require(platform.isNotEmpty() && platform.length <= 64) {
            "Platform must contain between 1 and 64 characters"
        }
        require(operatingSystemVersion.isNotEmpty() && operatingSystemVersion.length <= 64) {
            "Operating-system version must contain between 1 and 64 characters"
        }
        require(deviceCategory.isNotEmpty() && deviceCategory.length <= 64) {
            "Device category must contain between 1 and 64 characters"
        }
        require(deviceName == null || deviceName.length <= 256) { "Device name is too long" }
        require(deviceModel == null || deviceModel.length <= 256) { "Device model is too long" }
        require(locale == null || locale.length <= 64) { "Locale identifier is too long" }
    }
}

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
public data object RuntimeApplicationInfoParameters {
    /** Typed request contract for the application-info method. */
    public val contract: RuntimeMethodContract<RuntimeApplicationInfoParameters> by lazy {
        RuntimeMethodContract(RuntimeMethod.applicationInfo, serializer())
    }
}
