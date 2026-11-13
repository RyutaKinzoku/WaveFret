package com.example.wavefret.common.textrecognition

/**
 * One character (or a short run of characters) found by text recognition
 * in a photo or rendered page, with its box in image pixels.
 *
 * @property text Recognized text, usually a single character. Type: String
 * @property left Left edge in pixels. Type: Int
 * @property top Top edge in pixels. Type: Int
 * @property right Right edge in pixels. Type: Int
 * @property bottom Bottom edge in pixels. Type: Int
 */
data class RecognizedSymbol(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    /** Box width in pixels, at least 1. Type: Int */
    val width: Int get() = (right - left).coerceAtLeast(1)

    /** Box height in pixels, at least 1. Type: Int */
    val height: Int get() = (bottom - top).coerceAtLeast(1)

    /** Vertical centre in pixels. Type: Double */
    val centerY: Double get() = (top + bottom) / 2.0
}
