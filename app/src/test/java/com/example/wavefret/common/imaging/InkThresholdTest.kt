package com.example.wavefret.common.imaging

import org.junit.Assert.assertTrue
import org.junit.Test

class InkThresholdTest {

    @Test
    fun separatesDarkInkFromLightPaper() {
        val luminance = IntArray(1_000) { if (it % 10 == 0) 30 else 220 }

        val threshold = InkThreshold.of(luminance)

        assertTrue("threshold was $threshold", threshold in 31..220)
    }

    @Test
    fun handlesGreyPaperAndGreyInkOfAPhoto() {
        val luminance = IntArray(1_000) { if (it % 8 == 0) 90 + it % 7 else 160 + it % 11 }

        val threshold = InkThreshold.of(luminance)

        assertTrue("threshold was $threshold", threshold in 97..160)
    }

    @Test
    fun aBlankPageKeepsTheDefaultThreshold() {
        assertTrue(InkThreshold.of(IntArray(100) { 250 }) == BinaryImage.DEFAULT_INK_THRESHOLD)
    }

    @Test
    fun theLenientThresholdAlsoCatchesFaintGreyLines() {
        val luminance = IntArray(1_000) { index ->
            when {
                index % 10 == 0 -> 20
                index % 10 == 1 -> 175
                else -> 250
            }
        }

        val lenientThreshold = InkThreshold.lenient(luminance)

        assertTrue("threshold was $lenientThreshold", lenientThreshold in 176..249)
    }

    @Test
    fun theLenientThresholdStaysBelowGreyPaper() {
        val luminance = IntArray(1_000) { if (it % 8 == 0) 90 else 160 }

        val lenientThreshold = InkThreshold.lenient(luminance)

        assertTrue("threshold was $lenientThreshold", lenientThreshold in 91..160)
    }
}
