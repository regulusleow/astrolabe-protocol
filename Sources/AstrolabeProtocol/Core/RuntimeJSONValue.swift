//
//  RuntimeJSONValue.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public indirect enum RuntimeJSONValue: Codable, Equatable, Sendable {
    case null
    case boolean(Bool)
    case integer(Int64)
    case number(Double)
    case string(String)
    case array([RuntimeJSONValue])
    case object([String: RuntimeJSONValue])

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .boolean(value)
        } else if let value = try? container.decode(Int64.self) {
            guard RuntimeJSONNumber.isSafeInteger(value) else {
                throw RuntimeContractValueError.unsafeInteger(value)
            }
            self = .integer(value)
        } else if let value = try? container.decode(Double.self) {
            guard value.isFinite else {
                throw RuntimeContractValueError.nonFiniteNumber(value)
            }
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([RuntimeJSONValue].self) {
            self = .array(value)
        } else {
            self = .object(try container.decode([String: RuntimeJSONValue].self))
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .null:
            try container.encodeNil()
        case let .boolean(value):
            try container.encode(value)
        case let .integer(value):
            guard RuntimeJSONNumber.isSafeInteger(value) else {
                throw RuntimeContractValueError.unsafeInteger(value)
            }
            try container.encode(value)
        case let .number(value):
            guard value.isFinite else {
                throw RuntimeContractValueError.nonFiniteNumber(value)
            }
            try container.encode(value)
        case let .string(value):
            try container.encode(value)
        case let .array(value):
            try container.encode(value)
        case let .object(value):
            try container.encode(value)
        }
    }
}

public struct RuntimeJSONObject: Codable, Equatable, Sendable {
    /// String-keyed values contained by this JSON object.
    public let values: [String: RuntimeJSONValue]

    public init(values: [String: RuntimeJSONValue] = [:]) {
        self.values = values
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        values = try container.decode([String: RuntimeJSONValue].self)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(values)
    }
}

public struct RuntimeExtensionMap: Codable, Equatable, Sendable {
    /// Namespaced extension members preserved without platform interpretation.
    public let values: [String: RuntimeJSONValue]

    public init(values: [String: RuntimeJSONValue] = [:]) throws {
        try Self.validateKeys(values.keys)
        self.values = values
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let values = try container.decode([String: RuntimeJSONValue].self)
        try Self.validateKeys(values.keys)
        self.values = values
    }

    public func encode(to encoder: Encoder) throws {
        try Self.validateKeys(values.keys)
        var container = encoder.singleValueContainer()
        try container.encode(values)
    }

    private static func validateKeys(_ keys: Dictionary<String, RuntimeJSONValue>.Keys) throws {
        for key in keys where !RuntimeNamespacedIdentifier.isValid(key) {
            throw RuntimeContractValueError.invalidNamespacedIdentifier(key)
        }
    }
}

public enum RuntimeJSONNumber {
    public static let maximumSafeInteger: Int64 = 9_007_199_254_740_991
    public static let minimumSafeInteger: Int64 = -9_007_199_254_740_991

    public static func isSafeInteger(_ value: Int64) -> Bool {
        minimumSafeInteger ... maximumSafeInteger ~= value
    }
}
