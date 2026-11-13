package com.example.wavefret.common.imaging

/**
 * A rectangle of an image, edges included.
 *
 * @property left Leftmost column. Type: Int
 * @property top Top row. Type: Int
 * @property right Rightmost column. Type: Int
 * @property bottom Bottom row. Type: Int
 */
data class PixelRegion(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    /** Width in pixels. Type: Int */
    val width: Int get() = right - left + 1

    /** Height in pixels. Type: Int */
    val height: Int get() = bottom - top + 1

    /**
     * @param image Image the region must fit in. Type: BinaryImage
     * @return The part of the region inside the image. Type: PixelRegion
     */
    fun clampedTo(image: BinaryImage): PixelRegion = PixelRegion(
        left = left.coerceIn(0, image.width - 1),
        top = top.coerceIn(0, image.height - 1),
        right = right.coerceIn(0, image.width - 1),
        bottom = bottom.coerceIn(0, image.height - 1)
    )
}
