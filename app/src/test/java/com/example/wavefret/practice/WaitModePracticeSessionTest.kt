package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitModePracticeSessionTest {

    private val openA = TabNote(stringIndex = 1, fret = 0)
    private val openD = TabNote(stringIndex = 2, fret = 0)
    private val openAHz = 55.0
    private val openDHz = 73.42
    private val wrongNoteHz = 61.74

    private fun sessionFor(vararg notes: TabNote): WaitModePracticeSession {
        val exercise = Exercise("Test", BassTunings.STANDARD, tempoBpm = 80, notes = notes.toList())
        return WaitModePracticeSession(exercise, NoteMatcher(), HitConfirmer(requiredConsecutiveMatches = 1))
    }

    @Test
    fun startsWaitingForTheFirstNote() {
        val session = sessionFor(openA, openD)

        val progress = session.currentProgress()

        assertEquals(0, progress.currentNoteIndex)
        assertEquals(2, progress.noteCount)
        assertFalse(progress.isComplete)
    }

    @Test
    fun wrongNoteDoesNotAdvance() {
        val session = sessionFor(openA, openD)

        val progress = session.onPitchDetected(wrongNoteHz, timestampMillis = 0)

        assertEquals(0, progress.currentNoteIndex)
        assertEquals(PitchReading.NOT_MATCHING, progress.lastReading)
    }

    @Test
    fun silenceIsReportedAndDoesNotAdvance() {
        val session = sessionFor(openA, openD)

        val progress = session.onPitchDetected(null, timestampMillis = 0)

        assertEquals(0, progress.currentNoteIndex)
        assertEquals(PitchReading.SILENT, progress.lastReading)
    }

    @Test
    fun correctNoteAdvancesToTheNextNote() {
        val session = sessionFor(openA, openD)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 0)

        assertEquals(1, progress.currentNoteIndex)
        assertEquals(1, progress.hitCount)
        assertEquals(PitchReading.MATCHING, progress.lastReading)
    }

    @Test
    fun playingEveryNoteInOrderCompletesTheExercise() {
        val session = sessionFor(openA, openD)

        session.onPitchDetected(openAHz, timestampMillis = 0)
        val progress = session.onPitchDetected(openDHz, timestampMillis = 100)

        assertTrue(progress.isComplete)
        assertEquals(2, progress.hitCount)
    }

    @Test
    fun sustainedNoteDoesNotSatisfyARepeatOfTheSameNote() {
        val session = sessionFor(openA, openA)

        session.onPitchDetected(openAHz, timestampMillis = 0)
        val progress = session.onPitchDetected(openAHz, timestampMillis = 100)

        assertEquals(1, progress.currentNoteIndex)
    }

    @Test
    fun repeatedNoteCountsAfterTheStringIsReplucked() {
        val session = sessionFor(openA, openA)

        session.onPitchDetected(openAHz, timestampMillis = 0)
        session.onPitchDetected(null, timestampMillis = 100)
        val progress = session.onPitchDetected(openAHz, timestampMillis = 200)

        assertTrue(progress.isComplete)
    }

    @Test
    fun readingsAfterCompletionAreIgnored() {
        val session = sessionFor(openA)
        session.onPitchDetected(openAHz, timestampMillis = 0)

        val progress = session.onPitchDetected(openDHz, timestampMillis = 100)

        assertTrue(progress.isComplete)
        assertEquals(1, progress.hitCount)
    }

    @Test
    fun restartReturnsToTheFirstNote() {
        val session = sessionFor(openA, openD)
        session.onPitchDetected(openAHz, timestampMillis = 0)

        val progress = session.restart()

        assertEquals(0, progress.currentNoteIndex)
        assertEquals(0, progress.hitCount)
    }

    @Test
    fun waitModeNeverCountsMisses() {
        val session = sessionFor(openA)

        val progress = session.onPitchDetected(wrongNoteHz, timestampMillis = 0)

        assertEquals(0, progress.missCount)
    }

    @Test
    fun expectsTheCurrentNotesPitch() {
        val session = sessionFor(openA, openD)

        assertEquals(openAHz, session.expectedFrequencyHz()!!, 0.01)
    }

    @Test
    fun expectsNothingOnceComplete() {
        val session = sessionFor(openA)
        session.onPitchDetected(openAHz, timestampMillis = 0)

        assertEquals(null, session.expectedFrequencyHz())
    }
}
