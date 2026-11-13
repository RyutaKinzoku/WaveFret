package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffLineEraserTest {

    private val staffLineEraser = StaffLineEraser()

    private fun pageWithTabStaff(): Pair<BinaryImage, Staff> {
        val image = BinaryImage.blank(300, 150)
        ScoreImageDrawing.staff(image, top = 40, lineCount = 4, spacing = 14, left = 10, right = 290)
        return image to StaffFinder().find(image).single()
    }

    @Test
    fun erasesTheStaffLines() {
        val (image, staff) = pageWithTabStaff()

        val erased = staffLineEraser.erase(image, listOf(staff))

        assertEquals(0, (0 until erased.height).sumOf { erased.inkCountInRow(it) })
    }

    @Test
    fun keepsANumberDrawnOnALine() {
        val (image, staff) = pageWithTabStaff()
        ScoreImageDrawing.clearBox(image, 100, 33, 10, 16)
        ScoreImageDrawing.verticalLine(image, 104, 35, 47)

        val erased = staffLineEraser.erase(image, listOf(staff))

        assertTrue((35..47).all { erased.isInk(104, it) })
        assertFalse(erased.isInk(60, 40))
    }

    @Test
    fun erasesBarLinesThatSpanTheStaff() {
        val (image, staff) = pageWithTabStaff()
        ScoreImageDrawing.verticalLine(image, 150, 40, 83)

        val erased = staffLineEraser.erase(image, listOf(staff))

        assertFalse(erased.isInk(150, 60))
    }

    @Test
    fun leavesTheOriginalImageUntouched() {
        val (image, staff) = pageWithTabStaff()

        staffLineEraser.erase(image, listOf(staff))

        assertTrue(image.isInk(60, 40))
    }

    @Test
    fun keepsANoteheadSittingOnALine() {
        val (image, staff) = pageWithTabStaff()
        ScoreImageDrawing.filledNotehead(image, 150, 41, halfWidth = 8, halfHeight = 6)

        val erased = staffLineEraser.erase(image, listOf(staff))

        assertTrue(erased.isInk(150, 41))
        assertTrue(erased.isInk(145, 40))
    }

    @Test
    fun canKeepBarLines() {
        val (image, staff) = pageWithTabStaff()
        ScoreImageDrawing.verticalLine(image, 150, 40, 83)

        val erased = staffLineEraser.erase(image, listOf(staff), eraseBarLines = false)

        assertTrue(erased.isInk(150, 60))
        assertFalse(erased.isInk(60, 40))
    }
}
