package com.example.wavefret.practice.playalong

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.HitConfirmer
import com.example.wavefret.practice.NoteMatcher
import com.example.wavefret.practice.PitchReading
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayAlongSessionTest {

    private val openAHz = 55.0
    private val openDHz = 73.42
    private val lesson = Exercise(
        title = "Song",
        tuning = BassTunings.STANDARD,
        tempoBpm = 100,
        notes = listOf(TabNote(1, 0), TabNote(2, 0)),
        noteOnsetsMillis = listOf(2_000L, 3_000L)
    )

    private fun session() = PlayAlongSession(lesson, NoteMatcher(), HitConfirmer(requiredConsecutiveMatches = 1))

    @Test
    fun songPlaysUntilTheFirstNote() {
        val session = session()

        session.onMediaPosition(1_000)

        assertTrue(session.shouldMediaPlay)
        assertFalse(session.isWaitingForNote)
    }

    @Test
    fun songPausesAtANoteUntilItIsPlayed() {
        val session = session()

        session.onMediaPosition(1_960)

        assertFalse(session.shouldMediaPlay)
        assertTrue(session.isWaitingForNote)
    }

    @Test
    fun wrongNotesKeepTheSongPausedForAsManyTriesAsNeeded() {
        val session = session()
        session.onMediaPosition(2_000)

        repeat(20) { session.onPitchDetected(openDHz, timestampMillis = 0) }

        assertFalse(session.shouldMediaPlay)
        assertEquals(PitchReading.NOT_MATCHING, session.currentProgress().lastReading)
    }

    @Test
    fun theRightNoteResumesTheSongAndMovesOn() {
        val session = session()
        session.onMediaPosition(2_000)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 0)

        assertTrue(session.shouldMediaPlay)
        assertEquals(1, progress.currentNoteIndex)
    }

    @Test
    fun notesPlayedWhileTheSongIsRunningDoNotCount() {
        // The phone speaker may be playing the original bass line.
        val session = session()
        session.onMediaPosition(1_000)

        val progress = session.onPitchDetected(openAHz, timestampMillis = 0)

        assertEquals(0, progress.currentNoteIndex)
    }

    @Test
    fun playheadFollowsTheSongBetweenNotes() {
        val session = session()

        session.onMediaPosition(1_000)

        assertEquals(-0.5, session.currentProgress().playheadPosition, 1e-9)
    }

    @Test
    fun afterTheLastNoteTheSongPlaysOnAndTheLessonIsComplete() {
        val session = session()
        session.onMediaPosition(2_000); session.onPitchDetected(openAHz, 0)
        session.onMediaPosition(3_000); session.onPitchDetected(openDHz, 0)

        session.onMediaPosition(3_500)

        assertTrue(session.shouldMediaPlay)
        assertTrue(session.currentProgress().isComplete)
        assertEquals(2, session.currentProgress().hitCount)
    }

    @Test
    fun seekingBackRestartsFromTheNoteAtThatPoint() {
        val session = session()
        session.onMediaPosition(2_000); session.onPitchDetected(openAHz, 0)
        session.onMediaPosition(3_000); session.onPitchDetected(openDHz, 0)

        session.onSeekedTo(1_500)

        assertEquals(0, session.currentProgress().currentNoteIndex)
        assertTrue(session.shouldMediaPlay)
    }

    @Test
    fun lessonsWithoutTimingsCannotBePlayedAlong() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            PlayAlongSession(lesson.copy(noteOnsetsMillis = null), NoteMatcher(), HitConfirmer())
        }
    }

    @Test
    fun expectsTheNoteItIsWaitingFor() {
        val session = session()
        session.onMediaPosition(2_000)

        assertEquals(openAHz, session.expectedFrequencyHz()!!, 0.01)
    }
}
