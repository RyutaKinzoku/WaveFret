package com.example.wavefret.practice.progression

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSummaryBuilderTest {

    private val builder = PracticeSummaryBuilder(ExerciseProgression(DifficultyScorer(), masteryScorePercent = 90))
    private val easy = Exercise("Easy", BassTunings.STANDARD, 60, listOf(TabNote(0, 0), TabNote(0, 0)))
    private val hard = Exercise("Hard", BassTunings.STANDARD, 160, listOf(TabNote(0, 1), TabNote(3, 12), TabNote(0, 3)))

    private fun result(title: String, mode: PracticeMode, score: Int) =
        PracticeResult(title, mode, score, hitCount = 0, missCount = 0, completedAtMillis = 0)

    @Test
    fun firstRunIsAlwaysANewBest() {
        val summary = builder.build(result("Easy", PracticeMode.WAIT, 40), emptyList(), listOf(easy, hard))

        assertTrue(summary.isNewBest)
    }

    @Test
    fun lowerScoreThanBeforeIsNotANewBest() {
        val summary = builder.build(
            result("Easy", PracticeMode.WAIT, 40), listOf(result("Easy", PracticeMode.WAIT, 60)), listOf(easy, hard)
        )

        assertFalse(summary.isNewBest)
    }

    @Test
    fun masteringRunIsFlaggedAsJustMasteredOnlyTheFirstTime() {
        val firstMastery = builder.build(result("Easy", PracticeMode.TEMPO, 95), emptyList(), listOf(easy, hard))
        val repeatMastery = builder.build(
            result("Easy", PracticeMode.TEMPO, 95), listOf(result("Easy", PracticeMode.TEMPO, 91)), listOf(easy, hard)
        )

        assertTrue(firstMastery.isJustMastered)
        assertFalse(repeatMastery.isJustMastered)
    }

    @Test
    fun suggestsTheNextUnfinishedExerciseOtherThanThisOne() {
        val summary = builder.build(result("Easy", PracticeMode.TEMPO, 95), emptyList(), listOf(easy, hard))

        assertEquals("Hard", summary.nextExercise?.title)
    }

    @Test
    fun afterTheSecondExerciseSuggestsTheThirdNotTheFirst() {
        val medium = Exercise("Medium", BassTunings.STANDARD, 100, listOf(TabNote(0, 3), TabNote(2, 5), TabNote(1, 2)))
        val summary = builder.build(
            result("Medium", PracticeMode.WAIT, 80), listOf(result("Easy", PracticeMode.WAIT, 70)), listOf(easy, medium, hard)
        )

        assertEquals("Hard", summary.nextExercise?.title)
    }

    @Test
    fun suggestsNothingWhenEverythingElseIsFinished() {
        val summary = builder.build(
            result("Easy", PracticeMode.TEMPO, 95), listOf(result("Hard", PracticeMode.WAIT, 40)), listOf(easy, hard)
        )

        assertNull(summary.nextExercise)
    }

    @Test
    fun ratingFollowsTheScore() {
        val exercises = listOf(easy, hard)
        assertEquals(PerformanceRating.PERFECT, builder.build(result("Easy", PracticeMode.WAIT, 100), emptyList(), exercises).rating)
        assertEquals(PerformanceRating.GREAT, builder.build(result("Easy", PracticeMode.WAIT, 85), emptyList(), exercises).rating)
        assertEquals(PerformanceRating.GOOD, builder.build(result("Easy", PracticeMode.WAIT, 60), emptyList(), exercises).rating)
        assertEquals(PerformanceRating.KEEP_PRACTICING, builder.build(result("Easy", PracticeMode.WAIT, 30), emptyList(), exercises).rating)
    }
}
