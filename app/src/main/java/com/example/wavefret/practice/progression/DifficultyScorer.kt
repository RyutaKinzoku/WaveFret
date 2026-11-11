package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Ranks exercises with a transparent heuristic rather than a learned model:
 * tempo, how often the hand changes position, how far it crosses strings,
 * how far up the neck it goes, and how far it jumps between frets. Each
 * factor is normalized to 0..1 and weighted into a 0..100 score.
 *
 * Exercises carry one note per beat, so tempo doubles as note density;
 * rhythm is not encoded in the tab model, so it does not contribute yet.
 */
class DifficultyScorer {

    /**
     * @param exercise Exercise to rate. Type: Exercise
     * @return Its difficulty, overall and by factor. Type: DifficultyScore
     */
    fun score(exercise: Exercise): DifficultyScore {
        val transitions = exercise.notes.zipWithNext()
        val transitionCount = transitions.size.coerceAtLeast(1)
        val stringGapCount = (exercise.tuning.strings.size - 1).coerceAtLeast(1)

        val positionShiftCount = countPositionShifts(exercise.notes)
        val stringCrossingDistance = transitions.sumOf { (from, to) -> abs(to.stringIndex - from.stringIndex) }
        val averageFretJump = transitions
            .filter { (from, to) -> from.fret > 0 && to.fret > 0 }
            .map { (from, to) -> abs(to.fret - from.fret) }
            .average().takeUnless { it.isNaN() } ?: 0.0
        val highestFret = exercise.notes.maxOf { it.fret }

        val weightedDifficulty =
            TEMPO_WEIGHT * normalized(exercise.tempoBpm - EASIEST_TEMPO_BPM.toDouble(), TEMPO_RANGE_BPM) +
                POSITION_SHIFT_WEIGHT * normalized(positionShiftCount.toDouble(), transitionCount.toDouble()) +
                STRING_CROSSING_WEIGHT * normalized(stringCrossingDistance.toDouble(), transitionCount * stringGapCount / 2.0) +
                NECK_HEIGHT_WEIGHT * normalized(highestFret.toDouble(), FRETS_PER_OCTAVE) +
                FRET_JUMP_WEIGHT * normalized(averageFretJump, LARGE_FRET_JUMP)

        return DifficultyScore(
            overallScore = weightedDifficulty.roundToInt().coerceIn(0, MAXIMUM_SCORE),
            positionShiftCount = positionShiftCount,
            stringCrossingDistance = stringCrossingDistance
        )
    }

    /**
     * Follows a four-fret hand window along the fretted notes and counts how
     * often a note falls outside it. Open strings need no hand position.
     *
     * @param notes Notes in playing order. Type: List<TabNote>
     * @return Number of position shifts. Type: Int
     */
    private fun countPositionShifts(notes: List<TabNote>): Int {
        var handWindowLowestFret: Int? = null
        var positionShiftCount = 0
        for (fret in notes.map { it.fret }.filter { it > 0 }) {
            val windowLowestFret = handWindowLowestFret
            if (windowLowestFret == null) {
                handWindowLowestFret = fret
                continue
            }
            val windowHighestFret = windowLowestFret + HAND_SPAN_FRETS - 1
            if (fret in windowLowestFret..windowHighestFret) continue
            positionShiftCount++
            handWindowLowestFret = if (fret > windowHighestFret) fret - (HAND_SPAN_FRETS - 1) else fret
        }
        return positionShiftCount
    }

    /**
     * @param value Raw factor value. Type: Double
     * @param valueAtMaximum Value considered maximally hard. Type: Double
     * @return The value scaled into 0..1. Type: Double
     */
    private fun normalized(value: Double, valueAtMaximum: Double): Double {
        return (value / valueAtMaximum).coerceIn(0.0, 1.0)
    }

    private companion object {
        const val HAND_SPAN_FRETS = 4
        const val EASIEST_TEMPO_BPM = 50
        const val TEMPO_RANGE_BPM = 150.0
        const val FRETS_PER_OCTAVE = 12.0
        const val LARGE_FRET_JUMP = 7.0
        const val MAXIMUM_SCORE = 100
        const val TEMPO_WEIGHT = 35.0
        const val POSITION_SHIFT_WEIGHT = 25.0
        const val STRING_CROSSING_WEIGHT = 20.0
        const val NECK_HEIGHT_WEIGHT = 10.0
        const val FRET_JUMP_WEIGHT = 10.0
    }
}
