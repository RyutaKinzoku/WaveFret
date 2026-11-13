package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.LessonMediaSource
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonSynchronizerTest {

    private val lessonSynchronizer = LessonSynchronizer(FretboardPositionFinder(BassTunings.STANDARD))

    /** Open E, G on the E string, open A, open D: MIDI 28, 31, 33, 38. */
    private val tabLesson = Exercise(
        "Riff", BassTunings.STANDARD, 100, listOf(TabNote(0, 0), TabNote(0, 3), TabNote(1, 0), TabNote(2, 0))
    )
    private val video = LessonMediaSource.YouTubeVideo("dQw4w9WgXcQ")

    private fun heard(vararg notes: Pair<Int, Long>) = notes.map { (midiNumber, onsetMillis) -> NoteEvent(midiNumber, onsetMillis, 200) }

    @Test
    fun eachTabNoteTakesTheTimeItWasHeard() {
        val synced = lessonSynchronizer.synchronize(tabLesson, heard(28 to 1_000, 31 to 1_500, 33 to 2_000, 38 to 2_500), video)

        assertEquals(listOf(1_000L, 1_500L, 2_000L, 2_500L), synced.noteOnsetsMillis)
        assertEquals(video, synced.mediaSource)
        assertEquals(tabLesson.notes, synced.notes)
    }

    @Test
    fun extraNotesHeardFromOtherInstrumentsAreSkipped() {
        val synced = lessonSynchronizer.synchronize(
            tabLesson, heard(28 to 1_000, 45 to 1_200, 31 to 1_500, 33 to 2_000, 50 to 2_200, 38 to 2_500), video
        )

        assertEquals(listOf(1_000L, 1_500L, 2_000L, 2_500L), synced.noteOnsetsMillis)
    }

    @Test
    fun notesHeardAnOctaveOffStillMatch() {
        val synced = lessonSynchronizer.synchronize(tabLesson, heard(40 to 1_000, 31 to 1_500, 45 to 2_000, 38 to 2_500), video)

        assertEquals(listOf(1_000L, 1_500L, 2_000L, 2_500L), synced.noteOnsetsMillis)
    }

    @Test
    fun notesThatWereNotHeardGetATimeBetweenTheirNeighbours() {
        val synced = lessonSynchronizer.synchronize(tabLesson, heard(28 to 1_000, 33 to 2_000, 38 to 2_500), video)

        assertEquals(listOf(1_000L, 1_500L, 2_000L, 2_500L), synced.noteOnsetsMillis)
    }

    @Test
    fun timingsNeverGoBackwards() {
        val synced = lessonSynchronizer.synchronize(tabLesson, heard(38 to 500, 28 to 1_000, 31 to 1_500, 33 to 2_000), video)

        val onsets = synced.noteOnsetsMillis!!
        assertTrue(onsets.zipWithNext().all { (earlier, later) -> later >= earlier })
        assertEquals(1_000L, onsets[0])
    }

    @Test
    fun aLessonCannotBeSyncedWithNothingHeard() {
        assertThrows(IllegalArgumentException::class.java) { lessonSynchronizer.synchronize(tabLesson, emptyList(), video) }
    }

    @Test
    fun aLessonWithNoMatchingNotesCannotBeSynced() {
        assertThrows(IllegalArgumentException::class.java) {
            lessonSynchronizer.synchronize(tabLesson, heard(60 to 100, 61 to 200, 62 to 300, 63 to 400), video)
        }
    }
}
