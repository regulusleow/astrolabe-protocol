//
//  RuntimeApplication.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeApplication: Codable, Equatable, Sendable {
    /// Platform-neutral application identifier.
    public let identifier: String

    /// User-visible application name.
    public let displayName: String

    /// Application release version when available.
    @RuntimeRequiredNullable public private(set) var version: String?

    /// Application build version when available.
    @RuntimeRequiredNullable public private(set) var buildVersion: String?

    public init(identifier: String, displayName: String, version: String?, buildVersion: String?) {
        self.identifier = identifier
        self.displayName = displayName
        self.version = version
        self.buildVersion = buildVersion
    }
}

public struct RuntimeTarget: Codable, Equatable, Sendable {
    /// Opaque process-instance identifier.
    public let identifier: RuntimeOpaqueIdentifier

    /// Platform process identifier represented as an opaque string.
    @RuntimeRequiredNullable public private(set) var processIdentifier: String?

    /// Open target-kind identifier.
    public let kind: String

    /// Whether this is the Runtime's primary inspection target.
    public let primary: Bool

    public init(
        identifier: RuntimeOpaqueIdentifier,
        processIdentifier: String?,
        kind: String,
        primary: Bool
    ) {
        self.identifier = identifier
        self.processIdentifier = processIdentifier
        self.kind = kind
        self.primary = primary
    }
}

public enum RuntimeLayoutDirection: String, Codable, Sendable {
    case leftToRight
    case rightToLeft
    case unknown
}

public struct RuntimeEnvironment: Codable, Equatable, Sendable {
    /// Platform family reported by the Runtime.
    public let platform: String

    /// Operating-system release version.
    public let operatingSystemVersion: String

    /// Open device-category identifier.
    public let deviceCategory: String

    /// User-visible device name when available.
    @RuntimeRequiredNullable public private(set) var deviceName: String?

    /// Hardware or simulator model when available.
    @RuntimeRequiredNullable public private(set) var deviceModel: String?

    /// Whether the target runs on a virtual device.
    public let virtualDevice: Bool

    /// Active locale identifier when available.
    @RuntimeRequiredNullable public private(set) var locale: String?

    /// Effective interface layout direction.
    public let layoutDirection: RuntimeLayoutDirection

    /// Display facts required for coordinate conversion.
    public let display: RuntimeDisplayInfo

    /// Optional namespaced platform facts.
    public let extensions: RuntimeExtensionMap?

    public init(
        platform: String,
        operatingSystemVersion: String,
        deviceCategory: String,
        deviceName: String?,
        deviceModel: String?,
        virtualDevice: Bool,
        locale: String?,
        layoutDirection: RuntimeLayoutDirection,
        display: RuntimeDisplayInfo,
        extensions: RuntimeExtensionMap? = nil
    ) {
        self.platform = platform
        self.operatingSystemVersion = operatingSystemVersion
        self.deviceCategory = deviceCategory
        self.deviceName = deviceName
        self.deviceModel = deviceModel
        self.virtualDevice = virtualDevice
        self.locale = locale
        self.layoutDirection = layoutDirection
        self.display = display
        self.extensions = extensions
    }
}

public struct RuntimeApplicationInfoPayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Application identity and release metadata.
    public let application: RuntimeApplication

    /// Inspected process instance.
    public let target: RuntimeTarget

    /// Platform, device, locale, and display facts.
    public let environment: RuntimeEnvironment

    /// Optional namespaced application facts.
    public let extensions: RuntimeExtensionMap?

    public init(
        application: RuntimeApplication,
        target: RuntimeTarget,
        environment: RuntimeEnvironment,
        extensions: RuntimeExtensionMap? = nil
    ) {
        self.application = application
        self.target = target
        self.environment = environment
        self.extensions = extensions
    }

    public static let runtimeMethod = RuntimeMethod.applicationInfo
}
