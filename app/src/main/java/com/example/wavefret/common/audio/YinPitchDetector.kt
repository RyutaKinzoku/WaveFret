package com.example.wavefret.common.audio

/**
 * Detects the fundamental frequency of an audio buffer using the YIN
 * algorithm (de Cheveigne & Kawahara, 2002). Unlike picking the loudest
 * peak in a frequency spectrum, YIN measures how periodic the waveform
 * is at each candidate lag, so it finds the true fundamental even when a
 * microphone renders a harmonic louder than the fundamental itself — the
 * common case for a bass guitar captured on a phone mic.
 *
 * @property threshold Absolute threshold on the cumulative mean normalized
 *   difference function; only lags whose value dips below it are
 *   considered as the period. Lower values are stricter (fewer false
 *   positives on noise, may miss quiet notes); higher values are more
 *   lenient. Type: Double
 */
class YinPitchDetector(private val threshold: Double = 0.15) : PitchDetector {

    /**
     * @param samples Audio buffer to analyze, as PCM float samples in [-1, 1]. Type: FloatArray
     * @param sampleRate Sample rate the buffer was captured at, in Hz. Type: Int
     * @return Detected fundamental frequency in Hz, or null if no periodic
     *   pitch was found (silence or noise). Type: Double?
     */
    override fun detectPitch(samples: FloatArray, sampleRate: Int): Double? {
        val maxLag = samples.size / 2
        if (maxLag < MINIMUM_USABLE_MAX_LAG) return null

        val differenceFunction = computeDifferenceFunction(samples, maxLag)
        val cmndf = computeCumulativeMeanNormalizedDifference(differenceFunction)
        val candidateLags = findCandidateLags(cmndf)
        if (candidateLags.isEmpty()) return null
        val fundamentalLag = selectFundamentalLag(cmndf, candidateLags)
        val refinedLag = refineLagByParabolicInterpolation(cmndf, fundamentalLag)

        return sampleRate / refinedLag
    }

    /**
     * Computes the squared-difference function d(tau): how much the signal
     * differs from a copy of itself shifted by tau samples, for each tau.
     *
     * @param samples Audio buffer to analyze. Type: FloatArray
     * @param maxLag Largest shift (tau) to evaluate, in samples. Type: Int
     * @return Difference function values, indexed by tau from 0 to maxLag - 1. Type: DoubleArray
     */
    private fun computeDifferenceFunction(samples: FloatArray, maxLag: Int): DoubleArray {
        val result = DoubleArray(maxLag)
        for (lag in 0 until maxLag) {
            var sum = 0.0
            for (index in 0 until maxLag) {
                val delta = samples[index] - samples[index + lag]
                sum += delta * delta
            }
            result[lag] = sum
        }
        return result
    }

    /**
     * Normalizes the difference function by its running mean (YIN step 2),
     * so absolute signal energy doesn't bias which lag looks best. Guards
     * against a zero running mean — pure silence, where every d(tau) is
     * exactly 0 — by treating that lag as non-periodic instead of dividing
     * by zero.
     *
     * @param differenceFunction Raw difference function from computeDifferenceFunction. Type: DoubleArray
     * @return Cumulative mean normalized difference function, same size as input. Type: DoubleArray
     */
    private fun computeCumulativeMeanNormalizedDifference(differenceFunction: DoubleArray): DoubleArray {
        val result = DoubleArray(differenceFunction.size)
        result[0] = 1.0
        var runningSum = 0.0
        for (lag in 1 until differenceFunction.size) {
            runningSum += differenceFunction[lag]
            result[lag] = if (runningSum == 0.0) {
                1.0
            } else {
                differenceFunction[lag] / (runningSum / lag)
            }
        }
        return result
    }

    /**
     * Finds every candidate period, per YIN's absolute threshold step.
     * Each contiguous run of lags whose CMNDF dips below threshold is one
     * candidate, represented by its deepest lag.
     *
     * @param cmndf Cumulative mean normalized difference function. Type: DoubleArray
     * @return Candidate lags in ascending order; empty if nothing dips below threshold. Type: List<Int>
     */
    private fun findCandidateLags(cmndf: DoubleArray): List<Int> {
        val candidateLags = mutableListOf<Int>()
        var lag = MIN_LAG
        while (lag < cmndf.size) {
            if (cmndf[lag] < threshold) {
                var deepestLagInDip = lag
                while (lag < cmndf.size && cmndf[lag] < threshold) {
                    if (cmndf[lag] < cmndf[deepestLagInDip]) deepestLagInDip = lag
                    lag++
                }
                candidateLags.add(deepestLagInDip)
            } else {
                lag++
            }
        }
        return candidateLags
    }

    /**
     * Picks the shortest candidate lag whose dip is nearly as deep as the
     * deepest candidate, guarding against octave errors in both directions:
     *
     * - Too low (a subharmonic): a real note also dips at 2T, 3T, ... about
     *   as deeply as at T, so preferring the shortest near-deepest lag
     *   still picks T.
     * - Too high (a harmonic): when the fundamental is weak and even
     *   harmonics dominate, a shallow dip appears at T/2 before the much
     *   deeper one at T. Taking the first dip alone locked onto T/2, which
     *   is how a low E (41.2 Hz) was read as 82.4 Hz and shown as D.
     *
     * @param cmndf Cumulative mean normalized difference function. Type: DoubleArray
     * @param candidateLags Candidate lags in ascending order, not empty. Type: List<Int>
     * @return The lag judged to be the fundamental period. Type: Int
     */
    private fun selectFundamentalLag(cmndf: DoubleArray, candidateLags: List<Int>): Int {
        val deepestValue = candidateLags.minOf { cmndf[it] }
        val acceptableValue = deepestValue * HARMONIC_REJECTION_RATIO + HARMONIC_REJECTION_MARGIN
        return candidateLags.first { cmndf[it] <= acceptableValue }
    }

    /**
     * Refines an integer lag estimate to sub-sample precision by fitting a
     * parabola through the estimate and its two neighbors, since the true
     * period rarely falls exactly on a sample boundary.
     *
     * @param cmndf Cumulative mean normalized difference function. Type: DoubleArray
     * @param lag Integer lag estimate to refine. Type: Int
     * @return Refined, possibly fractional lag. Type: Double
     */
    private fun refineLagByParabolicInterpolation(cmndf: DoubleArray, lag: Int): Double {
        if (lag <= 0 || lag >= cmndf.size - 1) return lag.toDouble()
        val previous = cmndf[lag - 1]
        val current = cmndf[lag]
        val next = cmndf[lag + 1]
        val denominator = 2 * (2 * current - previous - next)
        if (denominator == 0.0) return lag.toDouble()
        val shift = (next - previous) / denominator
        return lag + shift
    }

    companion object {
        private const val MIN_LAG = 1
        private const val MINIMUM_USABLE_MAX_LAG = 4
        private const val HARMONIC_REJECTION_RATIO = 2.0
        private const val HARMONIC_REJECTION_MARGIN = 0.005
    }
}