package com.example.wavefret.practice

import androidx.annotation.StringRes
import com.example.wavefret.R
import com.example.wavefret.practice.progression.DifficultyLevel

/**
 * @param difficultyLevel Level to name. Type: DifficultyLevel
 * @return String resource with the level's display name. Type: Int
 */
@StringRes
fun labelResourceFor(difficultyLevel: DifficultyLevel): Int = when (difficultyLevel) {
    DifficultyLevel.BEGINNER -> R.string.difficulty_beginner
    DifficultyLevel.INTERMEDIATE -> R.string.difficulty_intermediate
    DifficultyLevel.ADVANCED -> R.string.difficulty_advanced
}
