package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StaffFinderTest {

    private val staffFinder = StaffFinder()

    @Test
    fun findsAFourLineTabStaff() {
        val image = BinaryImage.blank(400, 200)
        val rows = ScoreImageDrawing.staff(image, top = 50, lineCount = 4, spacing = 14, left = 20, right = 380)

        val staffs = staffFinder.find(image)

        assertEquals(1, staffs.size)
        assertEquals(4, staffs.single().lineYs.size)
        staffs.single().lineYs.zip(rows).forEach { (found, drawn) -> assertTrue(abs(found - (drawn + 0.5)) < 1.0) }
        assertEquals(14.0, staffs.single().lineSpacing, 0.5)
    }

    @Test
    fun separatesANotationStaffFromTheTabStaffBelowIt() {
        val image = BinaryImage.blank(400, 300)
        ScoreImageDrawing.staff(image, top = 30, lineCount = 5, spacing = 10, left = 20, right = 380)
        ScoreImageDrawing.staff(image, top = 120, lineCount = 4, spacing = 14, left = 20, right = 380)

        val staffs = staffFinder.find(image)

        assertEquals(listOf(5, 4), staffs.map { it.lineYs.size })
    }

    @Test
    fun linesBrokenByTabNumbersAreStillFound() {
        val image = BinaryImage.blank(400, 200)
        val rows = ScoreImageDrawing.staff(image, top = 50, lineCount = 4, spacing = 14, left = 20, right = 380)
        listOf(60, 120, 180, 240, 300).forEach { x -> ScoreImageDrawing.clearBox(image, x, rows[3] - 6, 12, 14) }

        assertEquals(listOf(4), staffFinder.find(image).map { it.lineYs.size })
    }

    @Test
    fun barLinesAndTextAreNotStaffLines() {
        val image = BinaryImage.blank(400, 200)
        ScoreImageDrawing.staff(image, top = 50, lineCount = 4, spacing = 14, left = 20, right = 380)
        ScoreImageDrawing.verticalLine(image, 200, 50, 92)
        ScoreImageDrawing.horizontalLine(image, 150, 20, 60)

        assertEquals(listOf(4), staffFinder.find(image).map { it.lineYs.size })
    }

    @Test
    fun reportsTheStaffsHorizontalExtent() {
        val image = BinaryImage.blank(400, 200)
        ScoreImageDrawing.staff(image, top = 50, lineCount = 4, spacing = 14, left = 30, right = 370)

        val staff = staffFinder.find(image).single()

        assertEquals(30, staff.left, 2)
        assertEquals(370, staff.right, 2)
    }

    @Test
    fun aBlankPageHasNoStaffs() {
        assertEquals(0, staffFinder.find(BinaryImage.blank(100, 100)).size)
    }

    private fun assertEquals(expected: Int, actual: Int, tolerance: Int) {
        assertTrue("expected $expected ±$tolerance but was $actual", abs(expected - actual) <= tolerance)
    }
}
