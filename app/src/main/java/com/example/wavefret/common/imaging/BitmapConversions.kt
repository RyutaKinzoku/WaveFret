package com.example.wavefret.common.imaging

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap

/**
 * Turns a photo or rendered page into black and white, choosing the ink
 * level from the image itself so grey paper and faint ink still work.
 *
 * Android framework glue; the threshold is the tested [InkThreshold].
 *
 * @return The image as ink and paper. Type: BinaryImage
 */
fun Bitmap.toBinaryImage(): BinaryImage {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val luminance = IntArray(pixels.size) { index ->
        val pixel = pixels[index]
        if (Color.alpha(pixel) < OPAQUE_ENOUGH) WHITE_LUMINANCE
        else (Color.red(pixel) * RED_WEIGHT + Color.green(pixel) * GREEN_WEIGHT + Color.blue(pixel) * BLUE_WEIGHT).toInt()
    }
    return BinaryImage.fromLuminance(width, height, luminance, InkThreshold.of(luminance))
}

/**
 * @param region Part of the image to draw. Type: PixelRegion
 * @param scale Whole-number enlargement, so small print is easier to recognize. Type: Int
 * @return The region as a black-on-white bitmap. Type: Bitmap
 */
fun BinaryImage.toBitmap(region: PixelRegion, scale: Int): Bitmap {
    val scaledWidth = region.width * scale
    val scaledHeight = region.height * scale
    val pixels = IntArray(scaledWidth * scaledHeight) { index ->
        val x = region.left + (index % scaledWidth) / scale
        val y = region.top + (index / scaledWidth) / scale
        if (isInk(x, y)) Color.BLACK else Color.WHITE
    }
    return createBitmap(scaledWidth, scaledHeight).apply { setPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight) }
}

private const val OPAQUE_ENOUGH = 128
private const val WHITE_LUMINANCE = 255
private const val RED_WEIGHT = 0.299
private const val GREEN_WEIGHT = 0.587
private const val BLUE_WEIGHT = 0.114
