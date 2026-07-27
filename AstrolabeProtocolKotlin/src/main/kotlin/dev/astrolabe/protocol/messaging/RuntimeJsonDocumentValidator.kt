//
//  RuntimeJsonDocumentValidator.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Validates wire-level JSON constraints before model decoding. */
internal class RuntimeJsonDocumentValidator(
    private val json: Json
) {
    fun validate(data: ByteArray) {
        if (data.isEmpty()) {
            invalid("JSON document cannot be empty")
        }
        if (data.size >= BYTE_ORDER_MARK.size &&
            data.copyOfRange(0, BYTE_ORDER_MARK.size).contentEquals(BYTE_ORDER_MARK)
        ) {
            invalid("JSON document cannot contain a byte-order mark")
        }
        try {
            data.decodeToString(throwOnInvalidSequence = true)
        } catch (error: CharacterCodingException) {
            throw RuntimeMessageException.InvalidDocument("JSON document is not valid UTF-8", error)
        }

        RuntimeJsonParser(data, json).parseRootObject()
    }

    private companion object {
        private val BYTE_ORDER_MARK = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}

private class RuntimeJsonParser(
    private val bytes: ByteArray,
    private val json: Json
) {
    private var index: Int = 0
    private val stack = mutableListOf<ContainerFrame>()

    fun parseRootObject() {
        skipWhitespace()
        if (!consumeIfPresent(CharacterByte.LEFT_BRACE)) {
            invalid("Wire message root must be an object")
        }
        stack += ContainerFrame.objectFrame()

        while (stack.isNotEmpty()) {
            when (stack.last().state) {
                ContainerState.OBJECT_KEY_OR_END -> parseObjectKey(allowEnd = true)
                ContainerState.OBJECT_KEY -> parseObjectKey(allowEnd = false)
                ContainerState.OBJECT_COLON -> parseObjectColon()
                ContainerState.OBJECT_VALUE -> parseValue()
                ContainerState.OBJECT_COMMA_OR_END -> parseObjectSeparator()
                ContainerState.ARRAY_VALUE_OR_END -> parseArrayValue(allowEnd = true)
                ContainerState.ARRAY_VALUE -> parseArrayValue(allowEnd = false)
                ContainerState.ARRAY_COMMA_OR_END -> parseArraySeparator()
            }
        }

        skipWhitespace()
        if (index != bytes.size) {
            invalid("JSON document contains trailing data")
        }
    }

    private fun parseObjectKey(allowEnd: Boolean) {
        skipWhitespace()
        if (allowEnd && consumeIfPresent(CharacterByte.RIGHT_BRACE)) {
            stack.removeLast()
            return
        }
        if (peek() != CharacterByte.QUOTE) {
            invalid("JSON object key must be a string")
        }

        val key = parseString()
        if (!stack.last().keys.add(key)) {
            invalid("JSON object contains duplicate key: $key")
        }
        stack.last().state = ContainerState.OBJECT_COLON
    }

    private fun parseObjectColon() {
        skipWhitespace()
        consume(CharacterByte.COLON)
        stack.last().state = ContainerState.OBJECT_VALUE
    }

    private fun parseObjectSeparator() {
        skipWhitespace()
        if (consumeIfPresent(CharacterByte.RIGHT_BRACE)) {
            stack.removeLast()
        } else {
            consume(CharacterByte.COMMA)
            stack.last().state = ContainerState.OBJECT_KEY
        }
    }

    private fun parseArrayValue(allowEnd: Boolean) {
        skipWhitespace()
        if (allowEnd && consumeIfPresent(CharacterByte.RIGHT_BRACKET)) {
            stack.removeLast()
            return
        }
        parseValue()
    }

    private fun parseArraySeparator() {
        skipWhitespace()
        if (consumeIfPresent(CharacterByte.RIGHT_BRACKET)) {
            stack.removeLast()
        } else {
            consume(CharacterByte.COMMA)
            stack.last().state = ContainerState.ARRAY_VALUE
        }
    }

    private fun parseValue() {
        skipWhitespace()
        val byte = peek() ?: invalid("JSON value is incomplete")
        markCurrentValueConsumed()

        when (byte) {
            CharacterByte.LEFT_BRACE -> {
                index += 1
                stack += ContainerFrame.objectFrame()
            }
            CharacterByte.LEFT_BRACKET -> {
                index += 1
                stack += ContainerFrame.arrayFrame()
            }
            CharacterByte.QUOTE -> parseString()
            CharacterByte.MINUS, in CharacterByte.ZERO..CharacterByte.NINE -> parseNumber()
            CharacterByte.T -> consumeLiteral("true")
            CharacterByte.F -> consumeLiteral("false")
            CharacterByte.N -> consumeLiteral("null")
            else -> invalid("JSON value is malformed")
        }
    }

    private fun markCurrentValueConsumed() {
        val frame = stack.last()
        frame.state = when (frame.state) {
            ContainerState.OBJECT_VALUE -> ContainerState.OBJECT_COMMA_OR_END
            ContainerState.ARRAY_VALUE_OR_END,
            ContainerState.ARRAY_VALUE -> ContainerState.ARRAY_COMMA_OR_END
            else -> frame.state
        }
    }

    private fun parseString(): String {
        val start = index
        consume(CharacterByte.QUOTE)
        var escaped = false

        while (index < bytes.size) {
            val byte = bytes[index]
            index += 1
            if (escaped) {
                escaped = false
                continue
            }
            when {
                byte == CharacterByte.BACKSLASH -> escaped = true
                byte == CharacterByte.QUOTE -> {
                    val encoded = bytes.copyOfRange(start, index).decodeToString()
                    return try {
                        json.decodeFromString(encoded)
                    } catch (error: SerializationException) {
                        throw RuntimeMessageException.InvalidDocument(
                            "JSON string is malformed",
                            error
                        )
                    }
                }
                byte.toInt() in 0 until CharacterByte.SPACE.toInt() ->
                    invalid("JSON string contains a control byte")
            }
        }
        invalid("JSON string is unterminated")
    }

    private fun parseNumber() {
        val start = index
        while (peek()?.let(::isNumberByte) == true) {
            index += 1
        }
        val token = bytes.copyOfRange(start, index).decodeToString()
        if (!NUMBER_PATTERN.matches(token)) {
            invalid("JSON number is malformed")
        }

        if (token.contains('.') || token.contains('e') || token.contains('E')) {
            val value = token.toDoubleOrNull()
            if (value == null || !value.isFinite()) {
                invalid("JSON number must be finite")
            }
        } else {
            val value = token.toLongOrNull()
            if (value == null || value !in MINIMUM_SAFE_INTEGER..MAXIMUM_SAFE_INTEGER) {
                invalid("JSON integer exceeds the safe range")
            }
        }
    }

    private fun consumeLiteral(literal: String) {
        val expected = literal.encodeToByteArray()
        if (index + expected.size > bytes.size ||
            !bytes.copyOfRange(index, index + expected.size).contentEquals(expected)
        ) {
            invalid("JSON literal is malformed")
        }
        index += expected.size
    }

    private fun consume(expected: Byte) {
        if (!consumeIfPresent(expected)) {
            invalid("JSON document is malformed")
        }
    }

    private fun consumeIfPresent(expected: Byte): Boolean {
        if (peek() != expected) {
            return false
        }
        index += 1
        return true
    }

    private fun skipWhitespace() {
        while (peek() in CharacterByte.WHITESPACE) {
            index += 1
        }
    }

    private fun peek(): Byte? = bytes.getOrNull(index)

    private fun isNumberByte(byte: Byte): Boolean =
        byte == CharacterByte.MINUS ||
            byte == CharacterByte.PLUS ||
            byte == CharacterByte.PERIOD ||
            byte == CharacterByte.E ||
            byte == CharacterByte.UPPERCASE_E ||
            byte in CharacterByte.ZERO..CharacterByte.NINE

    private fun invalid(message: String): Nothing =
        throw RuntimeMessageException.InvalidDocument("$message at byte offset $index")

    private companion object {
        private const val MAXIMUM_SAFE_INTEGER: Long = 9_007_199_254_740_991
        private const val MINIMUM_SAFE_INTEGER: Long = -9_007_199_254_740_991
        private val NUMBER_PATTERN = Regex("^-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?$")
    }
}

private class ContainerFrame(
    var state: ContainerState,
    val keys: MutableSet<String>
) {
    companion object {
        fun objectFrame(): ContainerFrame =
            ContainerFrame(ContainerState.OBJECT_KEY_OR_END, mutableSetOf())

        fun arrayFrame(): ContainerFrame =
            ContainerFrame(ContainerState.ARRAY_VALUE_OR_END, mutableSetOf())
    }
}

private enum class ContainerState {
    OBJECT_KEY_OR_END,
    OBJECT_KEY,
    OBJECT_COLON,
    OBJECT_VALUE,
    OBJECT_COMMA_OR_END,
    ARRAY_VALUE_OR_END,
    ARRAY_VALUE,
    ARRAY_COMMA_OR_END
}

private object CharacterByte {
    const val QUOTE: Byte = 0x22
    const val PLUS: Byte = 0x2B
    const val COMMA: Byte = 0x2C
    const val MINUS: Byte = 0x2D
    const val PERIOD: Byte = 0x2E
    const val ZERO: Byte = 0x30
    const val NINE: Byte = 0x39
    const val COLON: Byte = 0x3A
    const val UPPERCASE_E: Byte = 0x45
    const val LEFT_BRACKET: Byte = 0x5B
    const val BACKSLASH: Byte = 0x5C
    const val RIGHT_BRACKET: Byte = 0x5D
    const val E: Byte = 0x65
    const val F: Byte = 0x66
    const val N: Byte = 0x6E
    const val T: Byte = 0x74
    const val LEFT_BRACE: Byte = 0x7B
    const val RIGHT_BRACE: Byte = 0x7D
    const val SPACE: Byte = 0x20
    val WHITESPACE: Set<Byte> = setOf(0x20, 0x09, 0x0A, 0x0D)
}

private fun invalid(message: String): Nothing =
    throw RuntimeMessageException.InvalidDocument(message)
