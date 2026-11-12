package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningSessionTest {

    @Test
    fun withNoStringChosenItStartsFromTheLowestString() {
        val session = TuningSession(stringCount = 4)

        assertEquals(0, session.targetStringIndex)
        assertTrue(session.isAutomatic)
    }

    @Test
    fun automaticModeMovesUpToTheNextUntunedString() {
        val session = TuningSession(4)

        session.markTuned(0)

        assertEquals(1, session.targetStringIndex)
        assertEquals(setOf(0), session.tunedStringIndices)
    }

    @Test
    fun aChosenStringIsTunedEvenIfLowerOnesAreNot() {
        val session = TuningSession(4)

        session.select(3)

        assertEquals(3, session.targetStringIndex)
        assertFalse(session.isAutomatic)
    }

    @Test
    fun afterTheChosenStringIsTunedItGoesBackToAutomatic() {
        val session = TuningSession(4)
        session.select(3)

        session.markTuned(3)

        assertTrue(session.isAutomatic)
        assertEquals(0, session.targetStringIndex)
    }

    @Test
    fun tunedStringsAreSkippedInAutomaticMode() {
        val session = TuningSession(4)
        session.select(1); session.markTuned(1)
        session.markTuned(0)

        assertEquals(2, session.targetStringIndex)
    }

    @Test
    fun choosingATunedStringUnmarksItAndTunesIt() {
        val session = TuningSession(4)
        session.markTuned(0)

        session.select(0)

        assertEquals(emptySet<Int>(), session.tunedStringIndices)
        assertEquals(0, session.targetStringIndex)
    }

    @Test
    fun choosingTheChosenStringAgainGoesBackToAutomatic() {
        val session = TuningSession(4)
        session.select(2)

        session.select(2)

        assertTrue(session.isAutomatic)
    }

    @Test
    fun unmarkingMakesAStringOutOfTuneAgain() {
        val session = TuningSession(4)
        session.markTuned(0); session.markTuned(1)

        session.unmark(0)

        assertEquals(setOf(1), session.tunedStringIndices)
        assertEquals(0, session.targetStringIndex)
    }

    @Test
    fun whenEveryStringIsTunedThereIsNothingLeftToTune() {
        val session = TuningSession(4)

        (0..3).forEach(session::markTuned)

        assertNull(session.targetStringIndex)
        assertTrue(session.isAllTuned)
    }

    @Test
    fun resetMakesEveryStringOutOfTuneAndAutomatic() {
        val session = TuningSession(4)
        session.markTuned(0); session.select(2)

        session.resetAll()

        assertEquals(emptySet<Int>(), session.tunedStringIndices)
        assertTrue(session.isAutomatic)
    }
}
