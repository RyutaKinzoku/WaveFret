package com.example.wavefret.tuner

import com.example.wavefret.common.music.BassString
import com.example.wavefret.common.music.centsBetween
import kotlin.math.abs

/**
 * Measures how far a reading is from the string being tuned, and only that
 * string. A bass rings strongly at two and three times its pitch and pitch
 * detectors sometimes report those harmonics (or an octave below), so each
 * reading is also tried at 1/2, 1/3 and 2x and the interpretation closest
 * to the chosen string is used. This is what stops the tuner jumping to
 * another string.
 */
class TargetedTuningEvaluator {

    /**
     * @param targetString String being tuned. Type: BassString
     * @param detectedFrequencyHz Pitch reading. Type: Double
     * @return Cents from the string's correct pitch; positive is sharp. Type: Double
     */
    fun centsFrom(targetString: BassString, detectedFrequencyHz: Double): Double {
        return HARMONIC_FOLDS.map { foldFactor -> centsBetween(targetString.frequencyHz, detectedFrequencyHz * foldFactor) }
            .minBy { abs(it) }
    }

    private companion object {
        val HARMONIC_FOLDS = listOf(1.0, 1.0 / 2.0, 1.0 / 3.0, 2.0)
    }
}
