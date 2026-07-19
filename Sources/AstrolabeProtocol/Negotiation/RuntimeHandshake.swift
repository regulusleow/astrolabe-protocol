//
//  RuntimeHandshake.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeClientDescriptor: Codable, Equatable, Sendable {
    /// Client product name.
    public let name: String

    /// Client release version.
    public let version: String

    public init(name: String, version: String) {
        self.name = name
        self.version = version
    }
}

public struct RuntimeHandshakeParameters: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Host initiating the protocol session.
    public let client: RuntimeClientDescriptor

    /// Protocol versions the Host can decode.
    public let supportedProtocolRange: RuntimeProtocolRange

    public init(client: RuntimeClientDescriptor, supportedProtocolRange: RuntimeProtocolRange) {
        self.client = client
        self.supportedProtocolRange = supportedProtocolRange
    }

    public static let runtimeMethod = RuntimeMethod.handshake
}

public struct RuntimeDescriptor: Codable, Equatable, Sendable {
    /// Namespaced runtime implementation identifier.
    public let identifier: RuntimeNamespacedIdentifier

    /// Runtime SDK release version.
    public let version: String

    /// Opaque identifier for this runtime process instance.
    public let instanceID: RuntimeOpaqueIdentifier

    public init(
        identifier: RuntimeNamespacedIdentifier,
        version: String,
        instanceID: RuntimeOpaqueIdentifier
    ) {
        self.identifier = identifier
        self.version = version
        self.instanceID = instanceID
    }
}

public struct RuntimeHandshakePayload: Codable, Equatable, Sendable, RuntimeMethodBound {
    /// Runtime implementation participating in the session.
    public let runtime: RuntimeDescriptor

    /// Platform identifier reported by the runtime.
    public let platform: String

    /// Protocol version selected for this session.
    public let negotiatedProtocolVersion: RuntimeProtocolVersion

    /// Runtime operations available to the Host.
    public let capabilities: [RuntimeCapability]

    /// Optional namespaced session facts.
    public let extensions: RuntimeExtensionMap?

    public init(
        runtime: RuntimeDescriptor,
        platform: String,
        negotiatedProtocolVersion: RuntimeProtocolVersion,
        capabilities: [RuntimeCapability],
        extensions: RuntimeExtensionMap? = nil
    ) {
        self.runtime = runtime
        self.platform = platform
        self.negotiatedProtocolVersion = negotiatedProtocolVersion
        self.capabilities = capabilities
        self.extensions = extensions
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        runtime = try container.decode(RuntimeDescriptor.self, forKey: .runtime)
        platform = try container.decode(String.self, forKey: .platform)
        negotiatedProtocolVersion = try container.decode(
            RuntimeProtocolVersion.self,
            forKey: .negotiatedProtocolVersion
        )
        guard negotiatedProtocolVersion == .v2 else {
            throw RuntimeProtocolVersionError.unsupportedVersion(negotiatedProtocolVersion)
        }
        capabilities = try container.decode([RuntimeCapability].self, forKey: .capabilities)
        extensions = try container.decodeIfPresent(RuntimeExtensionMap.self, forKey: .extensions)
    }

    public static let runtimeMethod = RuntimeMethod.handshake

    private enum CodingKeys: String, CodingKey {
        case runtime
        case platform
        case negotiatedProtocolVersion
        case capabilities
        case extensions
    }
}
