//
//  RuntimeAttributedTextRun.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Zero-based text range. */
@Serializable
public data class RuntimeTextRange(
    /** Zero-based text location. */
    public val location: Long,
    /** Number of text units covered by the run. */
    public val length: Long
) {
    init {
        require(location >= 0 && length >= 0) { "Text range values cannot be negative" }
        require(location <= MAXIMUM_SAFE_TEXT_INDEX && length <= MAXIMUM_SAFE_TEXT_INDEX) {
            "Text range values exceed the JSON safe integer range"
        }
    }
}

/** Styled text segment captured by the Runtime. */
@Serializable
public data class RuntimeTextRun(
    /** Range covered by this run. */
    public val range: RuntimeTextRange,
    /** Text covered by this run. */
    public val text: String,
    /** Platform font name when available. */
    public val fontName: String?,
    /** Platform font-family name when available. */
    public val fontFamilyName: String?,
    /** Font size and scaling unit when available. */
    public val fontSize: RuntimeMeasurement?,
    /** Foreground color when available. */
    public val color: RuntimeColor?,
    /** Namespaced platform-specific text-run facts. */
    public val extensions: RuntimeExtensionMap
)

private const val MAXIMUM_SAFE_TEXT_INDEX: Long = 9_007_199_254_740_991
