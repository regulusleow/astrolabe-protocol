//
//  RuntimeMessageCodec.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/16.
//

import Foundation

public struct RuntimeMessageCodec: Sendable {
    public init() {}

    public func encode<Message: Encodable>(_ message: Message) throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let payload = try encoder.encode(message)
        try RuntimeJSONDocumentValidator().validate(payload)
        return payload
    }

    public func decode<Message: Decodable>(
        _ type: Message.Type,
        from payload: Data
    ) throws -> Message {
        try RuntimeJSONDocumentValidator().validate(payload)
        return try JSONDecoder().decode(type, from: payload)
    }
}

public enum RuntimeMessageCodecError: Error, Equatable, Sendable {
    case emptyPayload
    case byteOrderMarkNotAllowed
    case invalidUTF8
    case rootMustBeObject
    case malformedJSON
    case duplicateObjectKey(String)
    case trailingData
    case unsafeInteger(String)
    case nonFiniteNumber(String)
}

private struct RuntimeJSONDocumentValidator {
    func validate(_ data: Data) throws {
        guard !data.isEmpty else {
            throw RuntimeMessageCodecError.emptyPayload
        }
        guard !data.starts(with: [0xEF, 0xBB, 0xBF]) else {
            throw RuntimeMessageCodecError.byteOrderMarkNotAllowed
        }
        guard String(data: data, encoding: .utf8) != nil else {
            throw RuntimeMessageCodecError.invalidUTF8
        }

        var parser = RuntimeJSONParser(bytes: Array(data))
        try parser.parseRootObject()
    }
}

private struct RuntimeJSONParser {
    private let bytes: [UInt8]
    private var index = 0
    private var stack = [ContainerFrame]()

    init(bytes: [UInt8]) {
        self.bytes = bytes
    }

    mutating func parseRootObject() throws {
        skipWhitespace()
        guard consumeIfPresent(CharacterByte.leftBrace) else {
            throw RuntimeMessageCodecError.rootMustBeObject
        }
        stack.append(ContainerFrame.object())

        while let state = stack.last?.state {
            switch state {
            case .objectKeyOrEnd:
                try parseObjectKey(allowEnd: true)
            case .objectKey:
                try parseObjectKey(allowEnd: false)
            case .objectColon:
                skipWhitespace()
                try consume(CharacterByte.colon)
                stack[stack.count - 1].state = .objectValue
            case .objectValue:
                try parseValue()
            case .objectCommaOrEnd:
                try parseObjectSeparator()
            case .arrayValueOrEnd:
                try parseArrayValue(allowEnd: true)
            case .arrayValue:
                try parseArrayValue(allowEnd: false)
            case .arrayCommaOrEnd:
                try parseArraySeparator()
            }
        }

        skipWhitespace()
        guard index == bytes.count else {
            throw RuntimeMessageCodecError.trailingData
        }
    }

    private mutating func parseObjectKey(allowEnd: Bool) throws {
        skipWhitespace()
        if allowEnd, consumeIfPresent(CharacterByte.rightBrace) {
            stack.removeLast()
            return
        }
        guard peek() == CharacterByte.quote else {
            throw RuntimeMessageCodecError.malformedJSON
        }
        let key = try parseString()
        guard stack[stack.count - 1].keys.insert(key).inserted else {
            throw RuntimeMessageCodecError.duplicateObjectKey(key)
        }
        stack[stack.count - 1].state = .objectColon
    }

    private mutating func parseObjectSeparator() throws {
        skipWhitespace()
        if consumeIfPresent(CharacterByte.rightBrace) {
            stack.removeLast()
        } else {
            try consume(CharacterByte.comma)
            stack[stack.count - 1].state = .objectKey
        }
    }

    private mutating func parseArrayValue(allowEnd: Bool) throws {
        skipWhitespace()
        if allowEnd, consumeIfPresent(CharacterByte.rightBracket) {
            stack.removeLast()
            return
        }
        try parseValue()
    }

    private mutating func parseArraySeparator() throws {
        skipWhitespace()
        if consumeIfPresent(CharacterByte.rightBracket) {
            stack.removeLast()
        } else {
            try consume(CharacterByte.comma)
            stack[stack.count - 1].state = .arrayValue
        }
    }

    private mutating func parseValue() throws {
        skipWhitespace()
        guard let byte = peek() else {
            throw RuntimeMessageCodecError.malformedJSON
        }
        markCurrentValueConsumed()

        switch byte {
        case CharacterByte.leftBrace:
            index += 1
            stack.append(ContainerFrame.object())
        case CharacterByte.leftBracket:
            index += 1
            stack.append(ContainerFrame.array())
        case CharacterByte.quote:
            _ = try parseString()
        case CharacterByte.minus, CharacterByte.zero ... CharacterByte.nine:
            try parseNumber()
        case CharacterByte.t:
            try consumeLiteral("true")
        case CharacterByte.f:
            try consumeLiteral("false")
        case CharacterByte.n:
            try consumeLiteral("null")
        default:
            throw RuntimeMessageCodecError.malformedJSON
        }
    }

    private mutating func markCurrentValueConsumed() {
        let position = stack.count - 1
        switch stack[position].state {
        case .objectValue:
            stack[position].state = .objectCommaOrEnd
        case .arrayValueOrEnd, .arrayValue:
            stack[position].state = .arrayCommaOrEnd
        default:
            break
        }
    }

    private mutating func parseString() throws -> String {
        let start = index
        try consume(CharacterByte.quote)
        var escaped = false

        while let byte = peek() {
            index += 1
            if escaped {
                escaped = false
                continue
            }
            if byte == CharacterByte.backslash {
                escaped = true
            } else if byte == CharacterByte.quote {
                let slice = Data(bytes[start ..< index])
                do {
                    return try JSONDecoder().decode(String.self, from: slice)
                } catch {
                    throw RuntimeMessageCodecError.malformedJSON
                }
            } else if byte < CharacterByte.space {
                throw RuntimeMessageCodecError.malformedJSON
            }
        }
        throw RuntimeMessageCodecError.malformedJSON
    }

    private mutating func parseNumber() throws {
        let start = index
        while let byte = peek(), isNumberByte(byte) {
            index += 1
        }
        let token = String(decoding: bytes[start ..< index], as: UTF8.self)
        guard isValidNumberToken(token) else {
            throw RuntimeMessageCodecError.malformedJSON
        }

        if token.contains(".") || token.contains("e") || token.contains("E") {
            guard let value = Double(token), value.isFinite else {
                throw RuntimeMessageCodecError.nonFiniteNumber(token)
            }
        } else {
            guard let value = Int64(token), RuntimeJSONNumber.isSafeInteger(value) else {
                throw RuntimeMessageCodecError.unsafeInteger(token)
            }
        }
    }

    private mutating func consumeLiteral(_ literal: String) throws {
        let literalBytes = Array(literal.utf8)
        guard index + literalBytes.count <= bytes.count,
              Array(bytes[index ..< index + literalBytes.count]) == literalBytes else {
            throw RuntimeMessageCodecError.malformedJSON
        }
        index += literalBytes.count
    }

    private mutating func consume(_ expected: UInt8) throws {
        guard consumeIfPresent(expected) else {
            throw RuntimeMessageCodecError.malformedJSON
        }
    }

    private mutating func consumeIfPresent(_ expected: UInt8) -> Bool {
        guard peek() == expected else { return false }
        index += 1
        return true
    }

    private mutating func skipWhitespace() {
        while let byte = peek(), CharacterByte.whitespace.contains(byte) {
            index += 1
        }
    }

    private func peek() -> UInt8? {
        index < bytes.count ? bytes[index] : nil
    }

    private func isNumberByte(_ byte: UInt8) -> Bool {
        byte == CharacterByte.minus || byte == CharacterByte.plus ||
            byte == CharacterByte.period || byte == CharacterByte.e || byte == CharacterByte.uppercaseE ||
            CharacterByte.zero ... CharacterByte.nine ~= byte
    }

    private func isValidNumberToken(_ token: String) -> Bool {
        token.range(
            of: #"^-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?$"#,
            options: .regularExpression
        ) != nil
    }
}

private struct ContainerFrame {
    var state: ContainerState
    var keys: Set<String>

    static func object() -> Self {
        Self(state: .objectKeyOrEnd, keys: [])
    }

    static func array() -> Self {
        Self(state: .arrayValueOrEnd, keys: [])
    }
}

private enum ContainerState {
    case objectKeyOrEnd
    case objectKey
    case objectColon
    case objectValue
    case objectCommaOrEnd
    case arrayValueOrEnd
    case arrayValue
    case arrayCommaOrEnd
}

private enum CharacterByte {
    static let quote: UInt8 = 0x22
    static let plus: UInt8 = 0x2B
    static let comma: UInt8 = 0x2C
    static let minus: UInt8 = 0x2D
    static let period: UInt8 = 0x2E
    static let zero: UInt8 = 0x30
    static let nine: UInt8 = 0x39
    static let colon: UInt8 = 0x3A
    static let uppercaseE: UInt8 = 0x45
    static let leftBracket: UInt8 = 0x5B
    static let backslash: UInt8 = 0x5C
    static let rightBracket: UInt8 = 0x5D
    static let e: UInt8 = 0x65
    static let f: UInt8 = 0x66
    static let n: UInt8 = 0x6E
    static let t: UInt8 = 0x74
    static let leftBrace: UInt8 = 0x7B
    static let rightBrace: UInt8 = 0x7D
    static let space: UInt8 = 0x20
    static let whitespace: Set<UInt8> = [0x20, 0x09, 0x0A, 0x0D]
}
