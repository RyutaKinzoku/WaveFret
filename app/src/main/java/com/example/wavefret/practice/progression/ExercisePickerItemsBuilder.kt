package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * Builds the exercise picker's rows: easiest first, each with its level,
 * best score and mastery, and the next exercise to learn flagged.
 *
 * @property exerciseProgression Supplies ordering, mastery and recommendation. Type: ExerciseProgression
 */
class ExercisePickerItemsBuilder(private val exerciseProgression: ExerciseProgression) {

    /**
     * @param exercises Exercises available. Type: List<Exercise>
     * @param results Practice history. Type: List<PracticeResult>
     * @return Picker rows, easiest first. Type: List<ExercisePickerItem>
     */
    fun build(exercises: List<Exercise>, results: List<PracticeResult>): List<ExercisePickerItem> {
        val recommendedExercise = exerciseProgression.recommendNext(exercises, results)
        return exerciseProgression.orderedByDifficulty(exercises).map { rankedExercise ->
            val title = rankedExercise.exercise.title
            ExercisePickerItem(
                exercise = rankedExercise.exercise,
                difficultyLevel = rankedExercise.difficultyScore.level,
                bestScorePercent = exerciseProgression.bestScorePercent(title, results),
                isMastered = exerciseProgression.isMastered(title, results),
                isRecommended = rankedExercise.exercise == recommendedExercise
            )
        }
    }
}
