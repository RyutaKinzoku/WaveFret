package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * What the end-of-run card shows: the score, whether it is a personal best
 * or a new mastery worth celebrating, and what to play next.
 *
 * @property result The run that just finished. Type: PracticeResult
 * @property rating Encouraging verdict for the score. Type: PerformanceRating
 * @property isNewBest True if no earlier run of this exercise scored as high. Type: Boolean
 * @property isJustMastered True if this run mastered the exercise for the first time. Type: Boolean
 * @property nextExercise Easiest other exercise not yet mastered, or null. Type: Exercise?
 */
data class PracticeSummary(
    val result: PracticeResult,
    val rating: PerformanceRating,
    val isNewBest: Boolean,
    val isJustMastered: Boolean,
    val nextExercise: Exercise?
)
