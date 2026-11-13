package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonPlaybackScheduleTest {

    private val schedule = LessonPlaybackSchedule()

    private fun lesson(noteCount: Int, tempoBpm: Int = 120, onsetsMillis: List<Long>? = null) =
        Exercise("Lesson", BassTunings.STANDARD, tempoBpm, List(noteCount) { TabNote(0, it) }, onsetsMillis)

    @Test
    fun withoutTimingsEachNoteLastsOneBeat() {
        val notes = schedule.schedule(lesson(3, tempoBpm = 120))

        assertEquals(listOf(0L, 500L, 1_000L), notes.map { it.startMillis })
        assertEquals(listOf(500L, 500L, 500L), notes.map { it.durationMillis })
    }

    @Test
    fun withTimingsNotesKeepTheSongsRhythmFromTheFirstNote() {
        val notes = schedule.schedule(lesson(3, tempoBpm = 120, onsetsMillis = listOf(1_000L, 1_250L, 1_750L)))

        assertEquals(listOf(0L, 250L, 750L), notes.map { it.startMillis })
        assertEquals(listOf(250L, 500L, 500L), notes.map { it.durationMillis })
    }

    @Test
    fun longSilencesInTheSongAreShortened() {
        val notes = schedule.schedule(lesson(2, onsetsMillis = listOf(0L, 10_000L)))

        assertEquals(listOf(0L, 2_000L), notes.map { it.startMillis })
    }

    @Test
    fun notesStartingTogetherAreStillEachHeard() {
        val notes = schedule.schedule(lesson(2, onsetsMillis = listOf(0L, 0L)))

        assertEquals(listOf(80L, 500L), notes.map { it.durationMillis })
    }

    @Test
    fun eachNoteSoundsAtItsStringAndFret() {
        val notes = schedule.schedule(lesson(2))

        assertEquals(41.2, notes[0].frequencyHz, 0.05)
        assertEquals(43.65, notes[1].frequencyHz, 0.05)
        assertEquals(listOf(0, 1), notes.map { it.noteIndex })
    }

    @Test
    fun playbackCanStartFromAnyNote() {
        val notes = schedule.schedule(lesson(4, onsetsMillis = listOf(0L, 300L, 700L, 1_200L)), fromNoteIndex = 2)

        assertEquals(listOf(2, 3), notes.map { it.noteIndex })
        assertEquals(listOf(0L, 500L), notes.map { it.startMillis })
    }
}
