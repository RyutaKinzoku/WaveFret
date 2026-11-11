package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * Turns difficulty ratings and practice history into a learning path:
 * exercises ordered easiest first, which ones are mastered, and what to
 * play next. Mastery requires playing in time, so only tempo-mode runs
 * count towards it.
 *
 * @property difficultyScorer Rates each exercise. Type: DifficultyScorer
 * @property masteryScorePercent Tempo-mode score needed to master an exercise. Type: Int
 */
class ExerciseProgression(
    private val difficultyScorer: DifficultyScorer,
    private val masteryScorePercent: Int = DEFAULT_MASTERY_SCORE_PERCENT
) {

    /**
     * @param exercises Exercises to order. Type: List<Exercise>
     * @return The exercises with their ratings, easiest first (stable for ties). Type: List<RankedExercise>
     */
    fun orderedByDifficulty(exercises: List<Exercise>): List<RankedExercise> {
        return exercises
            .map { RankedExercise(it, difficultyScorer.score(it)) }
            .sortedBy { it.difficultyScore.overallScore }
    }

    /**
     * @param exerciseTitle Exercise to check. Type: String
     * @param results Practice history. Type: List<PracticeResult>
     * @return True if a tempo-mode run reached the mastery score. Type: Boolean
     */
    fun isMastered(exerciseTitle: String, results: List<PracticeResult>): Boolean {
        return results.any {
            it.exerciseTitle == exerciseTitle && it.practiceMode == PracticeMode.TEMPO &&
                it.scorePercent >= masteryScorePercent
        }
    }

    /**
     * @param exercises Exercises available. Type: List<Exercise>
     * @param results Practice history. Type: List<PracticeResult>
     * @return The easiest exercise not yet mastered, or null if all are. Type: Exercise?
     */
    fun recommendNext(exercises: List<Exercise>, results: List<PracticeResult>): Exercise? {
        return orderedByDifficulty(exercises)
            .firstOrNull { !isMastered(it.exercise.title, results) }
            ?.exercise
    }

    /**
     * @param exerciseTitle Exercise to check. Type: String
     * @param results Practice history. Type: List<PracticeResult>
     * @return Highest score in any mode, or null if never completed. Type: Int?
     */
    fun bestScorePercent(exerciseTitle: String, results: List<PracticeResult>): Int? {
        return results.filter { it.exerciseTitle == exerciseTitle }.maxOfOrNull { it.scorePercent }
    }

    private companion object {
        const val DEFAULT_MASTERY_SCORE_PERCENT = 90
    }
}
