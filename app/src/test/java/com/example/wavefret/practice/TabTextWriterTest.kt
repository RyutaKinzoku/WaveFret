package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Test

class TabTextWriterTest {

    private val exercise = Exercise(
        title = "Round trip",
        tuning = BassTunings.STANDARD,
        tempoBpm = 95,
        notes = listOf(TabNote(0, 3), TabNote(1, 0), TabNote(2, 12), TabNote(3, 5))
    )

    @Test
    fun writesTitleTempoAndNotes() {
        val tabText = TabTextWriter(notesPerLine = 8).write(exercise)

        assertEquals("title: Round trip\ntempo: 95\nE3 A0 D12 G5\n", tabText)
    }

    @Test
    fun wrapsNotesOntoSeveralLines() {
        val tabText = TabTextWriter(notesPerLine = 2).write(exercise)

        assertEquals("title: Round trip\ntempo: 95\nE3 A0\nD12 G5\n", tabText)
    }

    @Test
    fun writtenTextParsesBackToTheSameExercise() {
        val tabText = TabTextWriter().write(exercise)

        assertEquals(exercise, TabTextParser(BassTunings.STANDARD).parse(tabText))
    }

    @Test
    fun timedExerciseRoundTripsWithItsTimings() {
        val timedExercise = exercise.copy(noteOnsetsMillis = listOf(0L, 500L, 1_000L, 1_500L))

        assertEquals(timedExercise, TabTextParser(BassTunings.STANDARD).parse(TabTextWriter().write(timedExercise)))
    }
}
