package com.example.wavefret.common.textrecognition

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.imaging.toBitmap
import kotlin.math.roundToInt

/**
 * Reads the characters in one region of a black-and-white page with ML
 * Kit, enlarging small regions first so small tab numbers are read, and
 * returns their boxes in page pixels.
 *
 * Android framework glue, verified on-device.
 *
 * @property symbolRecognizer ML Kit text recognition. Type: MlKitSymbolRecognizer
 * @property targetHeightPixels Regions shorter than this are enlarged. Type: Int
 */
class MlKitImageSymbolRecognizer(
    private val symbolRecognizer: MlKitSymbolRecognizer,
    private val targetHeightPixels: Int = DEFAULT_TARGET_HEIGHT_PIXELS
) : ImageSymbolRecognizer {

    /**
     * @param image Whole page image. Type: BinaryImage
     * @param region Part of the page to read. Type: PixelRegion
     * @return Characters found, with boxes in page pixels. Type: List<RecognizedSymbol>
     */
    override suspend fun recognize(image: BinaryImage, region: PixelRegion): List<RecognizedSymbol> {
        val scale = (targetHeightPixels.toDouble() / region.height).roundToInt().coerceIn(1, MAXIMUM_SCALE)
        val regionBitmap = image.toBitmap(region, scale)
        return try {
            symbolRecognizer.recognize(regionBitmap).map { symbol ->
                RecognizedSymbol(
                    text = symbol.text,
                    left = region.left + symbol.left / scale,
                    top = region.top + symbol.top / scale,
                    right = region.left + symbol.right / scale,
                    bottom = region.top + symbol.bottom / scale
                )
            }
        } finally {
            regionBitmap.recycle()
        }
    }

    private companion object {
        const val DEFAULT_TARGET_HEIGHT_PIXELS = 320
        const val MAXIMUM_SCALE = 4
    }
}
