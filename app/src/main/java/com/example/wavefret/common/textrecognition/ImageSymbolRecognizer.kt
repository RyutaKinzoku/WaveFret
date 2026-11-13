package com.example.wavefret.common.textrecognition

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion

/**
 * Recognizes the characters printed in one region of an image.
 */
fun interface ImageSymbolRecognizer {

    /**
     * @param image Whole page image. Type: BinaryImage
     * @param region Part of the page to read. Type: PixelRegion
     * @return Characters found, with boxes in page pixels. Type: List<RecognizedSymbol>
     */
    suspend fun recognize(image: BinaryImage, region: PixelRegion): List<RecognizedSymbol>
}
