//
//  RuntimeColor.kt
//  astrolabe-protocol
//
//  Created by 轩辕十四 on 2026/7/20.
//

package dev.astrolabe.protocol

import kotlinx.serialization.Serializable

/** Color components expressed in a declared color space. */
@Serializable
public data class RuntimeColor(
    /** Color-space identifier used by the components. */
    public val colorSpace: String,
    /** Red component in the inclusive range from zero to one. */
    public val red: Double,
    /** Green component in the inclusive range from zero to one. */
    public val green: Double,
    /** Blue component in the inclusive range from zero to one. */
    public val blue: Double,
    /** Alpha component in the inclusive range from zero to one. */
    public val alpha: Double
) {
    init {
        require(colorSpace.isNotEmpty()) { "Color space cannot be empty" }
        require(listOf(red, green, blue, alpha).all { it in 0.0..1.0 }) {
            "Color components must be between zero and one"
        }
    }
}
