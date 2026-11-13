package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import org.junit.Assert.assertEquals
import org.junit.Test

class ClefDetectorTest {

    private val spacing = 12
    private val clefDetector = ClefDetector()

    /** A page with an empty 5-line staff from column 20 to 580; lines at rows 100 to 148. */
    private fun blankStaffPage(): BinaryImage {
        val image = BinaryImage.blank(600, 300)
        ScoreImageDrawing.staff(image, top = 100, lineCount = 5, spacing = spacing, left = 20, right = 580)
        return image
    }

    private fun detect(image: BinaryImage): Clef = clefDetector.detect(image, StaffFinder().find(image).single())

    @Test
    fun aClefReachingAboveAndBelowTheStaffIsATrebleClef() {
        val image = blankStaffPage()
        ScoreImageDrawing.trebleClef(image, 26, 100, spacing)

        assertEquals(Clef.TREBLE, detect(image))
    }

    @Test
    fun aClefThatStaysInsideTheStaffIsABassClef() {
        val image = blankStaffPage()
        ScoreImageDrawing.bassClef(image, 26, 100, spacing)

        assertEquals(Clef.BASS, detect(image))
    }

    @Test
    fun aStaffWithoutAClefHasAnUnknownClef() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 120, 149, spacing)

        assertEquals(Clef.UNKNOWN, detect(image))
    }

    @Test
    fun theThinLineJoiningAPianoSystemIsNotAClef() {
        val image = blankStaffPage()
        ScoreImageDrawing.verticalLine(image, 20, 60, 190)

        assertEquals(Clef.UNKNOWN, detect(image))
    }
}
