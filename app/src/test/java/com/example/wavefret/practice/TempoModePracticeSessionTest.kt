package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** At 60 BPM with a one-beat count-in started at t=0, note i is due at (i + 1) seconds. */
class TempoModePracticeSessionTest {

    private val openA = TabNote(stringIndex = 1, fret = 0)
    private val openD = TabNote(stringIndex = 2, fret = 0)
    private val openAHz = 55.0
    private val openDHz = 73.42

    private fun startedSessionFor(vararg notes: TabNote): TempoModePracticeSession {
        val exercise = Exercise("Test", BassTunings.STANDARD, tempoBpm = 60, notes = notes.toList())
        val session = TempoModePracticeSession(
            exercise = exercise,
            noteMatcher = NoteMatcher(),
            hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 1),
            countInBeats = 1
        )
        session.start(startTimeMillis = 0)
        return session
    }

    @Test
    fun readingsBeforeStartAreIgnored() {
        val exercise = Exercise("Test", BassTunings.STANDARD, tempoBpm = 60, notes = listOf(openA))
        val session = TempoModePracticeSession(exercise, NoteMatcher(), HitConfirmer(1), countInBeats = 1)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 1_000)

        assertEquals(0, progress.hitCount)
        assertEquals(false, session.isStarted)
    }

    @Test
    fun correctNoteOnItsBeatIsAHit() {
        val session = startedSessionFor(openA, openD)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 1_000)

        assertEquals(1, progress.hitCount)
        assertEquals(1, progress.currentNoteIndex)
    }

    @Test
    fun correctNoteSlightlyEarlyIsStillAHit() {
        val session = startedSessionFor(openA, openD)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 700)

        assertEquals(1, progress.hitCount)
    }

    @Test
    fun correctNoteFarTooEarlyIsNotAHit() {
        val session = startedSessionFor(openA, openD)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 200)

        assertEquals(0, progress.hitCount)
        assertEquals(0, progress.currentNoteIndex)
    }

    @Test
    fun noteNotPlayedBeforeItsWindowClosesIsAMiss() {
        val session = startedSessionFor(openA, openD)

        val progress = session.onPitchDetected(null, timestampMillis = 1_600)

        assertEquals(1, progress.missCount)
        assertEquals(1, progress.currentNoteIndex)
    }

    @Test
    fun longSilenceMissesEveryPassedNote() {
        val session = startedSessionFor(openA, openD)

        val progress = session.onPitchDetected(null, timestampMillis = 5_000)

        assertEquals(2, progress.missCount)
        assertTrue(progress.isComplete)
    }

    @Test
    fun legatoMoveToTheNextNoteOnItsBeatIsAHit() {
        val session = startedSessionFor(openA, openD)

        session.onPitchDetected(openAHz, timestampMillis = 1_000)
        val progress = session.onPitchDetected(openDHz, timestampMillis = 2_000)

        assertEquals(2, progress.hitCount)
        assertTrue(progress.isComplete)
    }

    @Test
    fun playheadFollowsTheBeatAfterTheCountIn() {
        val session = startedSessionFor(openA, openD)

        val progress = session.onPitchDetected(null, timestampMillis = 1_500)

        assertEquals(0.5, progress.playheadPosition, 1e-9)
    }

    @Test
    fun restartClearsScoreAndRequiresANewStart() {
        val session = startedSessionFor(openA, openD)
        session.onPitchDetected(openAHz, timestampMillis = 1_000)

        val progress = session.restart()

        assertEquals(0, progress.hitCount)
        assertEquals(false, session.isStarted)
    }

    @Test
    fun missedNotesAreRecordedByIndex() {
        val session = startedSessionFor(openA, openD)

        session.onPitchDetected(null, timestampMillis = 1_600)
        val progress = session.onPitchDetected(openDHz, timestampMillis = 2_000)

        assertEquals(setOf(0), progress.missedNoteIndices)
    }

    @Test
    fun expectsTheNoteWhoseBeatIsCurrent() {
        val session = startedSessionFor(openA, openD)
        session.onPitchDetected(null, timestampMillis = 1_600)

        assertEquals(openDHz, session.expectedFrequencyHz()!!, 0.01)
    }
}
