package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInExercisesTest {

    private val builtInExercises = BuiltInExercises(TabTextParser(BassTunings.STANDARD))

    @Test
    fun shipsSeveralExercises() {
        assertTrue(builtInExercises.all().size >= 3)
    }

    @Test
    fun everyExerciseHasAUniqueTitle() {
        val titles = builtInExercises.all().map { it.title }

        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun firstExerciseIsTheOpenStringWarmUp() {
        val firstExercise = builtInExercises.all().first()

        assertTrue(firstExercise.notes.all { it.fret == 0 })
    }

    @Test
    fun everyExerciseStaysWithinTheFirstTwelveFrets() {
        val allNotes = builtInExercises.all().flatMap { it.notes }

        assertTrue(allNotes.all { it.fret in 0..12 })
    }
}
