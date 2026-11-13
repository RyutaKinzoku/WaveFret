package com.example.wavefret.common.imaging

/**
 * A black-and-white image: each pixel is ink or paper. Used to find
 * printed shapes (lines, noteheads, numbers) in photos and PDF pages
 * without depending on Android's Bitmap, so the logic can be tested.
 *
 * @property width Width in pixels. Type: Int
 * @property height Height in pixels. Type: Int
 */
class BinaryImage private constructor(val width: Int, val height: Int, private val inkPixels: BooleanArray) {

    /**
     * @param x Column. Type: Int
     * @param y Row. Type: Int
     * @return True if the pixel is ink; false outside the image. Type: Boolean
     */
    fun isInk(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        return inkPixels[y * width + x]
    }

    /**
     * @param x Column. Type: Int
     * @param y Row. Type: Int
     * @param isInk True for ink, false for paper. Type: Boolean
     * @return Unit
     */
    fun setInk(x: Int, y: Int, isInk: Boolean) {
        if (x !in 0 until width || y !in 0 until height) return
        inkPixels[y * width + x] = isInk
    }

    /**
     * @param y Row. Type: Int
     * @return Number of ink pixels in the row. Type: Int
     */
    fun inkCountInRow(y: Int): Int = (0 until width).count { isInk(it, y) }

    /**
     * @return An independent copy. Type: BinaryImage
     */
    fun copy(): BinaryImage = BinaryImage(width, height, inkPixels.copyOf())

    /**
     * @param left Left column of the region. Type: Int
     * @param top Top row of the region. Type: Int
     * @param width Region width. Type: Int
     * @param height Region height. Type: Int
     * @return The region as a new image; parts outside the source are paper. Type: BinaryImage
     */
    fun crop(left: Int, top: Int, width: Int, height: Int): BinaryImage {
        val cropped = blank(width, height)
        for (y in 0 until height) for (x in 0 until width) cropped.setInk(x, y, isInk(left + x, top + y))
        return cropped
    }

    companion object {
        /** Luminance (0 black – 255 white) below which a pixel is ink. Type: Int */
        const val DEFAULT_INK_THRESHOLD = 128

        /**
         * @param width Width in pixels. Type: Int
         * @param height Height in pixels. Type: Int
         * @return An image of paper only. Type: BinaryImage
         */
        fun blank(width: Int, height: Int): BinaryImage = BinaryImage(width, height, BooleanArray(width * height))

        /**
         * @param width Width in pixels. Type: Int
         * @param height Height in pixels. Type: Int
         * @param luminance Row-major luminance, 0 black to 255 white. Type: IntArray
         * @param inkThreshold Luminance below which a pixel is ink. Type: Int
         * @return The thresholded image. Type: BinaryImage
         */
        fun fromLuminance(width: Int, height: Int, luminance: IntArray, inkThreshold: Int = DEFAULT_INK_THRESHOLD): BinaryImage {
            return BinaryImage(width, height, BooleanArray(width * height) { luminance[it] < inkThreshold })
        }
    }
}
