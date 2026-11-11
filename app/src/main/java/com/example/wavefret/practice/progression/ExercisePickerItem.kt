package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * One row of the exercise picker, independent of Android Views.
 *
 * @property exercise The exercise. Type: Exercise
 * @property difficultyLevel Its difficulty band. Type: DifficultyLevel
 * @property bestScorePercent Best score so far, or null if never completed. Type: Int?
 * @property isMastered True once mastered in tempo mode. Type: Boolean
 * @property isRecommended True for the single exercise suggested next. Type: Boolean
 */
data class ExercisePickerItem(
    val exercise: Exercise,
    val difficultyLevel: DifficultyLevel,
    val bestScorePercent: Int?,
    val isMastered: Boolean,
    val isRecommended: Boolean
)
