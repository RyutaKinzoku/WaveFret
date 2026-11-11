package com.example.wavefret.practice.progression

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerItemsBuilderTest {

    private val exercisePickerItemsBuilder = ExercisePickerItemsBuilder(ExerciseProgression(DifficultyScorer(), 90))

    private val easy = Exercise("Easy", BassTunings.STANDARD, 60, listOf(TabNote(0, 0), TabNote(0, 0)))
    private val hard = Exercise("Hard", BassTunings.STANDARD, 160, listOf(TabNote(0, 1), TabNote(3, 12), TabNote(0, 3)))
    private val masteredEasy = PracticeResult("Easy", PracticeMode.TEMPO, 95, 19, 1, completedAtMillis = 0)

    @Test
    fun itemsAreOrderedEasiestFirstWithTheirLevel() {
        val pickerItems = exercisePickerItemsBuilder.build(listOf(hard, easy), emptyList())

        assertEquals(listOf("Easy", "Hard"), pickerItems.map { it.exercise.title })
        assertEquals(DifficultyLevel.BEGINNER, pickerItems.first().difficultyLevel)
    }

    @Test
    fun itemsCarryMasteryAndBestScore() {
        val pickerItems = exercisePickerItemsBuilder.build(listOf(hard, easy), listOf(masteredEasy))

        assertEquals(true, pickerItems[0].isMastered)
        assertEquals(95, pickerItems[0].bestScorePercent)
        assertEquals(false, pickerItems[1].isMastered)
        assertEquals(null, pickerItems[1].bestScorePercent)
    }

    @Test
    fun onlyTheNextExerciseToLearnIsRecommended() {
        val pickerItems = exercisePickerItemsBuilder.build(listOf(hard, easy), listOf(masteredEasy))

        assertEquals(listOf(false, true), pickerItems.map { it.isRecommended })
    }
}
