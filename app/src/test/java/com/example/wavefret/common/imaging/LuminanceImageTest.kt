package com.example.wavefret.common.imaging

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuminanceImageTest {

    @Test
    fun darkInkOnLightPaperIsInk() {
        val image = LuminanceImage(4, 1, intArrayOf(20, 230, 230, 230)).toBinaryImage(inkIsLight = false)

        assertTrue(image.isInk(0, 0))
        assertFalse(image.isInk(1, 0))
    }

    @Test
    fun lightInkOnADarkBackgroundIsInkWhenInkIsLight() {
        val image = LuminanceImage(4, 1, intArrayOf(230, 20, 20, 20)).toBinaryImage(inkIsLight = true)

        assertTrue(image.isInk(0, 0))
        assertFalse(image.isInk(1, 0))
    }

    @Test
    fun aFrameIsDarkWhenMostOfItIsDark() {
        assertTrue(LuminanceImage(4, 1, intArrayOf(230, 20, 20, 20)).isMostlyDark)
        assertFalse(LuminanceImage(4, 1, intArrayOf(20, 230, 230, 230)).isMostlyDark)
    }
}
