package com.example.wavefret.common.imaging

/**
 * The brightness of every pixel of a picture (a video frame, a photo),
 * before it is split into ink and paper.
 *
 * @property width Width in pixels. Type: Int
 * @property height Height in pixels. Type: Int
 * @property luminance Brightness of each pixel, row by row, 0 black to 255 white. Type: IntArray
 */
class LuminanceImage(val width: Int, val height: Int, val luminance: IntArray) {

    init {
        require(luminance.size == width * height) { "There must be one luminance value per pixel" }
    }

    /** True if more than half the picture is darker than mid-grey, as with a white tab on a dark background. Type: Boolean */
    val isMostlyDark: Boolean get() = luminance.count { it < MID_GREY } * 2 > luminance.size

    /**
     * @param inkIsLight True when the writing is lighter than its background (white on black). Type: Boolean
     * @return The picture as ink and paper, with the ink level chosen close to the background's tone. Type: BinaryImage
     */
    fun toBinaryImage(inkIsLight: Boolean): BinaryImage {
        val darkness = if (inkIsLight) IntArray(luminance.size) { WHITE - luminance[it] } else luminance
        return BinaryImage.fromLuminance(width, height, darkness, InkThreshold.lenient(darkness))
    }

    private companion object {
        const val MID_GREY = 128
        const val WHITE = 255
    }
}
