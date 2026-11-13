package com.example.wavefret.common.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryImageTest {

    @Test
    fun darkPixelsBecomeInk() {
        val luminance = intArrayOf(0, 255, 100, 200)

        val image = BinaryImage.fromLuminance(width = 2, height = 2, luminance = luminance)

        assertTrue(image.isInk(0, 0))
        assertFalse(image.isInk(1, 0))
        assertTrue(image.isInk(0, 1))
        assertFalse(image.isInk(1, 1))
    }

    @Test
    fun pixelsOutsideTheImageAreNotInk() {
        val image = BinaryImage.blank(3, 3)

        assertFalse(image.isInk(-1, 0))
        assertFalse(image.isInk(0, 3))
    }

    @Test
    fun copiesAreIndependent() {
        val original = BinaryImage.blank(2, 2)
        val copy = original.copy()

        copy.setInk(1, 1, true)

        assertFalse(original.isInk(1, 1))
        assertTrue(copy.isInk(1, 1))
    }

    @Test
    fun countsInkInARow() {
        val image = BinaryImage.blank(5, 2)
        image.setInk(0, 1, true)
        image.setInk(3, 1, true)

        assertEquals(2, image.inkCountInRow(1))
        assertEquals(0, image.inkCountInRow(0))
    }

    @Test
    fun cropsARegion() {
        val image = BinaryImage.blank(4, 4)
        image.setInk(2, 3, true)

        val cropped = image.crop(left = 1, top = 2, width = 3, height = 2)

        assertEquals(3, cropped.width)
        assertEquals(2, cropped.height)
        assertTrue(cropped.isInk(1, 1))
    }
}
