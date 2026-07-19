//
//  RuntimeIdentifier.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import Foundation

public struct RuntimeOpaqueIdentifier: Codable, Equatable, Hashable, Sendable {
    /// Non-empty wire value whose internal format is owned by its producer.
    public let rawValue: String

    public init(rawValue: String) throws {
        guard !rawValue.isEmpty, rawValue.count <= 256 else {
            throw RuntimeContractValueError.invalidOpaqueIdentifier(rawValue)
        }
        self.rawValue = rawValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(rawValue: container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}

public struct RuntimeNamespacedIdentifier: Codable, Equatable, Hashable, Sendable {
    /// Dot-separated identifier that declares its owning namespace.
    public let rawValue: String

    public init(rawValue: String) throws {
        guard Self.isValid(rawValue) else {
            throw RuntimeContractValueError.invalidNamespacedIdentifier(rawValue)
        }
        self.rawValue = rawValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(rawValue: container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }

    public static func isValid(_ value: String) -> Bool {
        guard value.count <= 256 else { return false }
        return value.range(
            of: #"^[a-z][a-z0-9-]*(\.[A-Za-z][A-Za-z0-9_-]*)+$"#,
            options: .regularExpression
        ) != nil
    }
}

public enum RuntimeContractValueError: Error, Equatable, Sendable {
    case invalidOpaqueIdentifier(String)
    case invalidNamespacedIdentifier(String)
    case invalidOpenIdentifier(String)
    case unsafeInteger(Int64)
    case nonFiniteNumber(Double)
}
