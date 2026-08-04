//
//  RuntimeRequiredNullable.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

@propertyWrapper
public struct RuntimeRequiredNullable<Value>: Codable, Equatable, Sendable
where Value: Codable & Equatable & Sendable {
    /// Required wire member whose explicit value may be JSON null.
    public var wrappedValue: Value?

    public init(wrappedValue: Value?) {
        self.wrappedValue = wrappedValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        wrappedValue = container.decodeNil() ? nil : try container.decode(Value.self)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        if let wrappedValue {
            try container.encode(wrappedValue)
        } else {
            try container.encodeNil()
        }
    }
}
