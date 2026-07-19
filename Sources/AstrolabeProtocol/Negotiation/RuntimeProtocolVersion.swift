//
//  RuntimeProtocolVersion.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public struct RuntimeProtocolVersion: Codable, Comparable, Sendable {
    /// Incompatible contract generation.
    public let major: UInt16

    /// Backward-compatible contract revision.
    public let minor: UInt16

    public init(major: UInt16, minor: UInt16) {
        self.major = major
        self.minor = minor
    }

    public static let v2 = RuntimeProtocolVersion(major: 2, minor: 0)

    public static func < (lhs: RuntimeProtocolVersion, rhs: RuntimeProtocolVersion) -> Bool {
        lhs.major == rhs.major ? lhs.minor < rhs.minor : lhs.major < rhs.major
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.init(
            major: try container.decode(UInt16.self, forKey: .major),
            minor: try container.decode(UInt16.self, forKey: .minor)
        )
    }

    private enum CodingKeys: String, CodingKey {
        case major
        case minor
    }
}

public struct RuntimeProtocolRange: Codable, Equatable, Sendable {
    /// Oldest protocol version accepted by the peer.
    public let minimum: RuntimeProtocolVersion

    /// Newest protocol version accepted by the peer.
    public let maximum: RuntimeProtocolVersion

    public init(
        minimum: RuntimeProtocolVersion,
        maximum: RuntimeProtocolVersion
    ) throws {
        guard minimum.major == maximum.major else {
            throw RuntimeProtocolVersionError.crossMajorRange(minimum: minimum, maximum: maximum)
        }
        guard minimum <= maximum else {
            throw RuntimeProtocolVersionError.invalidRange(minimum: minimum, maximum: maximum)
        }
        self.minimum = minimum
        self.maximum = maximum
    }

    public static let v2 = RuntimeProtocolRange(uncheckedMinimum: .v2, maximum: .v2)

    public func contains(_ version: RuntimeProtocolVersion) -> Bool {
        minimum <= version && version <= maximum
    }

    public func highestCommonVersion(with other: RuntimeProtocolRange) -> RuntimeProtocolVersion? {
        let lowerBound = max(minimum, other.minimum)
        let upperBound = min(maximum, other.maximum)
        return lowerBound <= upperBound ? upperBound : nil
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        try self.init(
            minimum: container.decode(RuntimeProtocolVersion.self, forKey: .minimum),
            maximum: container.decode(RuntimeProtocolVersion.self, forKey: .maximum)
        )
    }

    private init(
        uncheckedMinimum minimum: RuntimeProtocolVersion,
        maximum: RuntimeProtocolVersion
    ) {
        self.minimum = minimum
        self.maximum = maximum
    }

    private enum CodingKeys: String, CodingKey {
        case minimum
        case maximum
    }
}

public enum RuntimeProtocolVersionError: Error, Equatable, Sendable {
    case unsupportedVersion(RuntimeProtocolVersion)
    case invalidRange(minimum: RuntimeProtocolVersion, maximum: RuntimeProtocolVersion)
    case crossMajorRange(minimum: RuntimeProtocolVersion, maximum: RuntimeProtocolVersion)
}
