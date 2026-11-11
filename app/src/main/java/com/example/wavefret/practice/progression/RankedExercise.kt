package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * An exercise together with its difficulty rating.
 *
 * @property exercise The exercise. Type: Exercise
 * @property difficultyScore How hard it is. Type: DifficultyScore
 */
data class RankedExercise(
    val exercise: Exercise,
    val difficultyScore: DifficultyScore
)
