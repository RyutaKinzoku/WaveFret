package com.example.wavefret.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class TabStaffLayoutTest {

    private val tabStaffLayout = TabStaffLayout(
        noteSpacingPx = 100f,
        playheadXPx = 200f,
        topPaddingPx = 20f,
        stringSpacingPx = 50f
    )

    @Test
    fun noteAtThePlayheadIsDrawnAtThePlayheadLine() {
        assertEquals(200f, tabStaffLayout.noteCenterXPx(noteIndex = 3, playheadPosition = 3.0), 1e-4f)
    }

    @Test
    fun laterNotesAreDrawnToTheRightByOneSpacingPerNote() {
        assertEquals(450f, tabStaffLayout.noteCenterXPx(noteIndex = 5, playheadPosition = 2.5), 1e-4f)
    }

    @Test
    fun earlierNotesScrollOffToTheLeft() {
        assertEquals(-100f, tabStaffLayout.noteCenterXPx(noteIndex = 0, playheadPosition = 3.0), 1e-4f)
    }

    @Test
    fun highestStringIsDrawnOnTopLikeStandardTab() {
        assertEquals(20f, tabStaffLayout.stringLineYPx(stringIndex = 3, stringCount = 4), 1e-4f)
    }

    @Test
    fun lowestStringIsDrawnAtTheBottom() {
        assertEquals(170f, tabStaffLayout.stringLineYPx(stringIndex = 0, stringCount = 4), 1e-4f)
    }

    @Test
    fun visibleNotesAreThoseWhoseCentersFitOnScreenWithAMargin() {
        val visibleNoteIndices = tabStaffLayout.visibleNoteIndices(
            noteCount = 20, playheadPosition = 4.0, viewWidthPx = 600f
        )

        // x = 200 + (i - 4) * 100 must lie within [-100, 700] -> i in 1..9
        assertEquals(1..9, visibleNoteIndices)
    }

    @Test
    fun visibleNotesAreClampedToTheExercise() {
        val visibleNoteIndices = tabStaffLayout.visibleNoteIndices(
            noteCount = 3, playheadPosition = 0.0, viewWidthPx = 2_000f
        )

        assertEquals(0..2, visibleNoteIndices)
    }
}
