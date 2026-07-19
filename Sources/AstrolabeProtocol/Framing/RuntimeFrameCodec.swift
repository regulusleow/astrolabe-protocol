//
//  RuntimeFrameCodec.swift
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/10.
//

import Foundation

public struct RuntimeFrameCodec: Sendable {
    /// Default upper bound for one framed JSON payload.
    public static let defaultMaximumPayloadSize = 16 * 1024 * 1024

    /// Maximum number of payload bytes accepted in a single frame.
    public let maximumPayloadSize: Int

    public init() {
        maximumPayloadSize = Self.defaultMaximumPayloadSize
    }

    public init(maximumPayloadSize: Int) throws {
        guard maximumPayloadSize > 0,
              maximumPayloadSize <= Int(UInt32.max) else {
            throw RuntimeFrameError.invalidMaximumPayloadSize(maximumPayloadSize)
        }
        self.maximumPayloadSize = maximumPayloadSize
    }

    public func encode(payload: Data) throws -> Data {
        guard !payload.isEmpty else {
            throw RuntimeFrameError.emptyPayload
        }
        guard payload.count <= maximumPayloadSize else {
            throw RuntimeFrameError.payloadTooLarge(
                actual: payload.count,
                maximum: maximumPayloadSize
            )
        }

        let length = UInt32(payload.count)
        var frame = Data(capacity: MemoryLayout<UInt32>.size + payload.count)
        frame.append(UInt8((length >> 24) & 0xFF))
        frame.append(UInt8((length >> 16) & 0xFF))
        frame.append(UInt8((length >> 8) & 0xFF))
        frame.append(UInt8(length & 0xFF))
        frame.append(payload)
        return frame
    }

    public func makeStreamDecoder() -> RuntimeFrameStreamDecoder {
        RuntimeFrameStreamDecoder(maximumPayloadSize: maximumPayloadSize)
    }
}

public struct RuntimeFrameStreamDecoder: Sendable {
    /// Number of bytes retained while waiting for a complete frame.
    public var pendingByteCount: Int {
        buffer.count
    }

    private let maximumPayloadSize: Int
    private var buffer = Data()

    fileprivate init(maximumPayloadSize: Int) {
        self.maximumPayloadSize = maximumPayloadSize
    }

    public mutating func append(_ data: Data) throws -> [Data] {
        buffer.append(data)
        var payloads = [Data]()

        while buffer.count >= MemoryLayout<UInt32>.size {
            let payloadLength = Int(readPayloadLength())
            guard payloadLength > 0 else {
                throw RuntimeFrameError.emptyPayload
            }
            guard payloadLength <= maximumPayloadSize else {
                throw RuntimeFrameError.payloadTooLarge(
                    actual: payloadLength,
                    maximum: maximumPayloadSize
                )
            }

            let frameLength = MemoryLayout<UInt32>.size + payloadLength
            guard buffer.count >= frameLength else {
                break
            }

            payloads.append(
                buffer.subdata(in: MemoryLayout<UInt32>.size ..< frameLength)
            )
            buffer.removeSubrange(0 ..< frameLength)
        }

        return payloads
    }

    public mutating func reset() {
        buffer.removeAll(keepingCapacity: true)
    }

    private func readPayloadLength() -> UInt32 {
        buffer.prefix(MemoryLayout<UInt32>.size).reduce(0) { partialResult, byte in
            (partialResult << 8) | UInt32(byte)
        }
    }
}

public enum RuntimeFrameError: Error, Equatable, Sendable {
    case emptyPayload
    case invalidMaximumPayloadSize(Int)
    case payloadTooLarge(actual: Int, maximum: Int)
}
