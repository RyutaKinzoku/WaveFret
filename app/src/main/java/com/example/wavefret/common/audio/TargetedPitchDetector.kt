package com.example.wavefret.common.audio

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/**
 * Checks whether the note the lesson expects is sounding, instead of
 * asking "which note is this?" from scratch. Because the period to look
 * for is known, a much weaker, noisier periodicity is accepted, so softly
 * plucked notes are heard. Only periods within a narrow band around the
 * expected note are considered, and a signal whose real fundamental is an
 * octave higher is refused, so other notes aren't mistaken for the expected one.
 *
 * @property lookAroundCents How far from the expected pitch a note may be. Type: Double
 * @property confidenceThreshold Highest YIN aperiodicity accepted (YIN's own default is 0.15). Type: Double
 * @property octaveRejectionRatio The note counts as an octave up when half its period is nearly as
 *   periodic as the full period: within this ratio (plus a small margin). Type: Double
 */
class TargetedPitchDetector(
    private val lookAroundCents: Double = DEFAULT_LOOK_AROUND_CENTS,
    private val confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD,
    private val octaveRejectionRatio: Double = DEFAULT_OCTAVE_REJECTION_RATIO
) {

    private val lowPassFilter = LowPassFilter(cutoffHz = BASS_CUTOFF_HZ)

    /**
     * @param samples Audio buffer in -1..1. Type: FloatArray
     * @param sampleRate Its sample rate. Type: Int
     * @param expectedFrequencyHz Pitch the lesson expects now. Type: Double
     * @return The heard pitch near the expected one, or null if it isn't sounding. Type: Double?
     */
    fun detectNear(samples: FloatArray, sampleRate: Int, expectedFrequencyHz: Double): Double? {
        val shortestLag = floor(sampleRate / (expectedFrequencyHz * centsRatio(lookAroundCents))).toInt()
        val longestLag = ceil(sampleRate / (expectedFrequencyHz * centsRatio(-lookAroundCents))).toInt()
        if (shortestLag < 2 || samples.size < 2 * (longestLag + 1)) return null

        val filtered = lowPassFilter.filter(samples, sampleRate)
        val aperiodicity = cumulativeMeanNormalizedDifference(filtered, longestLag + 1)
        val bestLag = (shortestLag..longestLag).minBy { aperiodicity[it] }
        if (aperiodicity[bestLag] > confidenceThreshold) return null

        val halfLagRange = (bestLag / 2 - 1)..(bestLag / 2 + 1)
        // A real octave-up note repeats just as well at half the period; a real bass note with a strong
        // second harmonic repeats clearly better at its full period.
        val octaveRejectionLimit = aperiodicity[bestLag] * octaveRejectionRatio + OCTAVE_REJECTION_MARGIN
        if (halfLagRange.first >= 1 && halfLagRange.minOf { aperiodicity[it] } < octaveRejectionLimit) return null

        return sampleRate / refinedLag(aperiodicity, bestLag)
    }

    /**
     * @param samples Filtered audio. Type: FloatArray
     * @param lagCount Number of lags to compute (0 until lagCount). Type: Int
     * @return YIN's cumulative mean normalised difference per lag. Type: DoubleArray
     */
    private fun cumulativeMeanNormalizedDifference(samples: FloatArray, lagCount: Int): DoubleArray {
        val comparedLength = samples.size - lagCount
        val aperiodicity = DoubleArray(lagCount)
        aperiodicity[0] = 1.0
        var runningSum = 0.0
        for (lag in 1 until lagCount) {
            var difference = 0.0
            for (index in 0 until comparedLength) {
                val delta = samples[index] - samples[index + lag]
                difference += delta * delta
            }
            runningSum += difference
            aperiodicity[lag] = if (runningSum == 0.0) 1.0 else difference / (runningSum / lag)
        }
        return aperiodicity
    }

    /**
     * @param aperiodicity YIN curve. Type: DoubleArray
     * @param lag Best whole-sample lag. Type: Int
     * @return Lag refined between samples by parabolic interpolation. Type: Double
     */
    private fun refinedLag(aperiodicity: DoubleArray, lag: Int): Double {
        if (lag <= 0 || lag >= aperiodicity.size - 1) return lag.toDouble()
        val denominator = 2 * (2 * aperiodicity[lag] - aperiodicity[lag - 1] - aperiodicity[lag + 1])
        if (denominator == 0.0) return lag.toDouble()
        return lag + (aperiodicity[lag + 1] - aperiodicity[lag - 1]) / denominator
    }

    /**
     * @param cents Interval. Type: Double
     * @return Frequency ratio of that interval. Type: Double
     */
    private fun centsRatio(cents: Double): Double = 2.0.pow(cents / CENTS_PER_OCTAVE)

    private companion object {
        const val DEFAULT_LOOK_AROUND_CENTS = 60.0
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.4
        const val DEFAULT_OCTAVE_REJECTION_RATIO = 1.5
        const val OCTAVE_REJECTION_MARGIN = 0.02
        const val BASS_CUTOFF_HZ = 1000.0
        const val CENTS_PER_OCTAVE = 1200.0
    }
}
