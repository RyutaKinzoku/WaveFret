package com.example.wavefret.tuner

import com.example.wavefret.common.music.centsBetween
import kotlin.math.abs

/**
 * Compares a detected pitch against a BassTuning and reports how close it
 * is to the nearest string: which string, how many cents off, and whether
 * that's flat, in tune, or sharp.
 *
 * @property tuning The set of strings to compare against. Type: BassTuning
 * @property inTuneToleranceCents How many cents flat or sharp still counts
 *   as in tune. Type: Double
 */
class TuningEvaluator(
    private val tuning: BassTuning,
    private val inTuneToleranceCents: Double = 5.0
) {

    /**
     * @param frequencyHz Detected pitch to evaluate, in Hz. Type: Double
     * @return The closest string, how far off it is in cents, and the
     *   resulting flat/in-tune/sharp status. Type: TuningResult
     */
    fun evaluate(frequencyHz: Double): TuningResult {
        val closestString = requireNotNull(
            tuning.strings.minByOrNull { abs(centsBetween(it.frequencyHz, frequencyHz)) }
        ) { "BassTuning must contain at least one string" }

        val cents = centsBetween(closestString.frequencyHz, frequencyHz)
        val status = when {
            cents < -inTuneToleranceCents -> TuningStatus.FLAT
            cents > inTuneToleranceCents -> TuningStatus.SHARP
            else -> TuningStatus.IN_TUNE
        }
        return TuningResult(closestString, cents, status)
    }
}