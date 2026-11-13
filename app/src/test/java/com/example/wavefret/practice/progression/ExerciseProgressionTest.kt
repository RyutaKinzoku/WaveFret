package com.example.wavefret.practice.progression

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseProgressionTest {

    private val exerciseProgression = ExerciseProgression(DifficultyScorer(), masteryScorePercent = 90)

    private val easy = Exercise("Easy", BassTunings.STANDARD, 60, listOf(TabNote(0, 0), TabNote(0, 0)))
    private val medium = Exercise("Medium", BassTunings.STANDARD, 100, listOf(TabNote(0, 3), TabNote(2, 5), TabNote(1, 2)))
    private val hard = Exercise("Hard", BassTunings.STANDARD, 160, listOf(TabNote(0, 1), TabNote(3, 12), TabNote(0, 3)))

    private fun result(title: String, mode: PracticeMode, scorePercent: Int) =
        PracticeResult(title, mode, scorePercent, hitCount = scorePercent, missCount = 100 - scorePercent, completedAtMillis = 0)

    @Test
    fun ordersExercisesFromEasiestToHardest() {
        val ordered = exerciseProgression.orderedByDifficulty(listOf(hard, easy, medium))

        assertEquals(listOf("Easy", "Medium", "Hard"), ordered.map { it.exercise.title })
    }

    @Test
    fun exerciseIsMasteredOnlyByAGoodEnoughTempoModeScore() {
        assertTrue(exerciseProgression.isMastered("Easy", listOf(result("Easy", PracticeMode.TEMPO, 95))))
        assertFalse(exerciseProgression.isMastered("Easy", listOf(result("Easy", PracticeMode.TEMPO, 80))))
        assertFalse(exerciseProgression.isMastered("Easy", listOf(result("Easy", PracticeMode.WAIT, 100))))
    }

    @Test
    fun recommendsTheEasiestExerciseWhenNothingIsMastered() {
        assertEquals("Easy", exerciseProgression.recommendNext(listOf(hard, medium, easy), emptyList())?.title)
    }

    @Test
    fun exerciseIsFinishedOnceCompletedInAnyMode() {
        assertTrue(exerciseProgression.isFinished("Easy", listOf(result("Easy", PracticeMode.WAIT, 40))))
        assertTrue(exerciseProgression.isFinished("Easy", listOf(result("Easy", PracticeMode.TEMPO, 10))))
        assertFalse(exerciseProgression.isFinished("Easy", listOf(result("Medium", PracticeMode.WAIT, 100))))
    }

    @Test
    fun recommendsTheEasiestExerciseNotYetFinished() {
        val results = listOf(result("Easy", PracticeMode.WAIT, 60))

        assertEquals("Medium", exerciseProgression.recommendNext(listOf(hard, medium, easy), results)?.title)
    }

    @Test
    fun neverRecommendsAFinishedExerciseAgain() {
        val results = listOf(result("Easy", PracticeMode.WAIT, 60), result("Medium", PracticeMode.WAIT, 70))

        assertEquals("Hard", exerciseProgression.recommendNext(listOf(hard, medium, easy), results)?.title)
    }

    @Test
    fun recommendsNothingOnceEverythingIsFinished() {
        val results = listOf("Easy", "Medium", "Hard").map { result(it, PracticeMode.WAIT, 50) }

        assertNull(exerciseProgression.recommendNext(listOf(hard, medium, easy), results))
    }

    @Test
    fun bestScoreIsTheHighestScoreInEitherMode() {
        val results = listOf(result("Easy", PracticeMode.WAIT, 100), result("Easy", PracticeMode.TEMPO, 70))

        assertEquals(100, exerciseProgression.bestScorePercent("Easy", results))
        assertNull(exerciseProgression.bestScorePercent("Hard", results))
    }
}
