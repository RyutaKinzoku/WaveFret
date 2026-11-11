package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExerciseTest {

    @Test
    fun exerciseWithoutNotesIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(title = "Empty", tuning = BassTunings.STANDARD, tempoBpm = 80, notes = emptyList())
        }
    }

    @Test
    fun nonPositiveTempoIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(title = "Frozen", tuning = BassTunings.STANDARD, tempoBpm = 0, notes = listOf(TabNote(0, 0)))
        }
    }

    @Test
    fun noteOnAStringTheTuningDoesNotHaveIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(title = "Five", tuning = BassTunings.STANDARD, tempoBpm = 80, notes = listOf(TabNote(4, 0)))
        }
    }

    @Test
    fun noteCountReflectsTheNotes() {
        val exercise = Exercise(
            title = "Two notes",
            tuning = BassTunings.STANDARD,
            tempoBpm = 80,
            notes = listOf(TabNote(0, 0), TabNote(1, 0))
        )

        assertEquals(2, exercise.noteCount)
    }
}
