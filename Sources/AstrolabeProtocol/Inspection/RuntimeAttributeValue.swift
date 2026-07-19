//
//  RuntimeAttributeValue.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

public enum RuntimeAttributeValue: Codable, Equatable, Sendable {
    case null
    case boolean(Bool)
    case integer(Int64)
    case number(Double)
    case string(String)
    case stringList([String])
    case measurement(RuntimeMeasurement)
    case point(RuntimeCoordinatePoint)
    case size(RuntimeMeasuredSize)
    case vector(RuntimeVector)
    case rect(RuntimeCoordinateRect)
    case insets(RuntimeInsets)
    case color(RuntimeColor)
    case textRuns([RuntimeTextRun])
    case layoutRelations([RuntimeLayoutRelation])
    case array([RuntimeJSONValue])
    case object([String: RuntimeJSONValue])
    case extensionValue(type: RuntimeNamespacedIdentifier, value: RuntimeJSONValue)

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let type = try container.decode(String.self, forKey: .type)

        switch type {
        case "null":
            guard try container.decodeNil(forKey: .value) else {
                throw DecodingError.dataCorruptedError(
                    forKey: .value,
                    in: container,
                    debugDescription: "A null attribute value must contain JSON null"
                )
            }
            self = .null
        case "boolean":
            self = .boolean(try container.decode(Bool.self, forKey: .value))
        case "integer":
            let value = try container.decode(Int64.self, forKey: .value)
            guard RuntimeJSONNumber.isSafeInteger(value) else {
                throw RuntimeContractValueError.unsafeInteger(value)
            }
            self = .integer(value)
        case "number":
            let value = try container.decode(Double.self, forKey: .value)
            guard value.isFinite else {
                throw RuntimeContractValueError.nonFiniteNumber(value)
            }
            self = .number(value)
        case "string":
            self = .string(try container.decode(String.self, forKey: .value))
        case "stringList":
            self = .stringList(try container.decode([String].self, forKey: .value))
        case "measurement":
            self = .measurement(try container.decode(RuntimeMeasurement.self, forKey: .value))
        case "point":
            self = .point(try container.decode(RuntimeCoordinatePoint.self, forKey: .value))
        case "size":
            self = .size(try container.decode(RuntimeMeasuredSize.self, forKey: .value))
        case "vector":
            self = .vector(try container.decode(RuntimeVector.self, forKey: .value))
        case "rect":
            self = .rect(try container.decode(RuntimeCoordinateRect.self, forKey: .value))
        case "insets":
            self = .insets(try container.decode(RuntimeInsets.self, forKey: .value))
        case "color":
            self = .color(try container.decode(RuntimeColor.self, forKey: .value))
        case "textRuns":
            self = .textRuns(try container.decode([RuntimeTextRun].self, forKey: .value))
        case "layoutRelations":
            self = .layoutRelations(
                try container.decode([RuntimeLayoutRelation].self, forKey: .value)
            )
        case "array":
            self = .array(try container.decode([RuntimeJSONValue].self, forKey: .value))
        case "object":
            self = .object(
                try container.decode([String: RuntimeJSONValue].self, forKey: .value)
            )
        default:
            self = .extensionValue(
                type: try RuntimeNamespacedIdentifier(rawValue: type),
                value: try container.decode(RuntimeJSONValue.self, forKey: .value)
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        switch self {
        case .null:
            try container.encode("null", forKey: .type)
            try container.encodeNil(forKey: .value)
        case let .boolean(value):
            try encode("boolean", value, into: &container)
        case let .integer(value):
            guard RuntimeJSONNumber.isSafeInteger(value) else {
                throw RuntimeContractValueError.unsafeInteger(value)
            }
            try encode("integer", value, into: &container)
        case let .number(value):
            guard value.isFinite else {
                throw RuntimeContractValueError.nonFiniteNumber(value)
            }
            try encode("number", value, into: &container)
        case let .string(value):
            try encode("string", value, into: &container)
        case let .stringList(value):
            try encode("stringList", value, into: &container)
        case let .measurement(value):
            try encode("measurement", value, into: &container)
        case let .point(value):
            try encode("point", value, into: &container)
        case let .size(value):
            try encode("size", value, into: &container)
        case let .vector(value):
            try encode("vector", value, into: &container)
        case let .rect(value):
            try encode("rect", value, into: &container)
        case let .insets(value):
            try encode("insets", value, into: &container)
        case let .color(value):
            try encode("color", value, into: &container)
        case let .textRuns(value):
            try encode("textRuns", value, into: &container)
        case let .layoutRelations(value):
            try encode("layoutRelations", value, into: &container)
        case let .array(value):
            try encode("array", value, into: &container)
        case let .object(value):
            try encode("object", value, into: &container)
        case let .extensionValue(type, value):
            try encode(type.rawValue, value, into: &container)
        }
    }

    private func encode<Value: Encodable>(
        _ type: String,
        _ value: Value,
        into container: inout KeyedEncodingContainer<CodingKeys>
    ) throws {
        try container.encode(type, forKey: .type)
        try container.encode(value, forKey: .value)
    }

    private enum CodingKeys: String, CodingKey {
        case type
        case value
    }
}
