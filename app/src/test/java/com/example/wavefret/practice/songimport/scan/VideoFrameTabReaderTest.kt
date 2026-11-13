package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.LuminanceImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.textrecognition.ImageSymbolRecognizer
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.practice.TabNote
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFrameTabReaderTest {

    /** Fake text recognition: sees a printed digit only where the image it is given shows it as ink. */
    private class PrintedDigits(private val digits: List<RecognizedSymbol>) : ImageSymbolRecognizer {
        override suspend fun recognize(image: BinaryImage, region: PixelRegion): List<RecognizedSymbol> =
            digits.filter { digit -> image.isInk((digit.left + digit.right) / 2, (digit.top + digit.bottom) / 2) }
    }

    private val tabFrame = BinaryImage.blank(500, 200)
    private val printedDigits: List<RecognizedSymbol>

    init {
        val rows = ScoreImageDrawing.staff(tabFrame, top = 60, lineCount = 4, spacing = 14, left = 20, right = 480)
        printedDigits = listOf(100 to rows[3], 150 to rows[2]).mapIndexed { index, (centreX, row) ->
            ScoreImageDrawing.filledRectangle(tabFrame, centreX - 4, row - 5, centreX + 4, row + 6)
            RecognizedSymbol(if (index == 0) "3" else "5", centreX - 4, row - 5, centreX + 4, row + 6)
        }
    }

    private fun luminanceOf(image: BinaryImage, inkLevel: Int, paperLevel: Int) =
        LuminanceImage(image.width, image.height, IntArray(image.width * image.height) { index ->
            if (image.isInk(index % image.width, index / image.width)) inkLevel else paperLevel
        })

    private fun reader() = VideoFrameTabReader(ScoreImageReader(BassTunings.STANDARD, PrintedDigits(printedDigits)))

    @Test
    fun readsADarkTabOnALightBackground() = runTest {
        val columns = reader().read(luminanceOf(tabFrame, inkLevel = 20, paperLevel = 235))

        assertEquals(listOf(listOf(TabNote(0, 3)), listOf(TabNote(1, 5))), columns)
    }

    @Test
    fun readsALightTabOnADarkBackground() = runTest {
        val columns = reader().read(luminanceOf(tabFrame, inkLevel = 235, paperLevel = 20))

        assertEquals(listOf(listOf(TabNote(0, 3)), listOf(TabNote(1, 5))), columns)
    }

    @Test
    fun aFrameWithoutATabReadsNothing() = runTest {
        val columns = reader().read(luminanceOf(BinaryImage.blank(500, 200), inkLevel = 20, paperLevel = 20))

        assertTrue(columns.isEmpty())
    }
}
