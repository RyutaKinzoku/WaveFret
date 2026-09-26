package com.example.wavefret.tuner

import kotlin.math.sqrt

/**
 * Filters out near-silent audio buffers before they reach pitch detection,
 * so the tuner doesn't display jittery noise-floor "pitches" while the
 * player isn't actually playing.
 *
 * @property rmsThreshold Minimum root-mean-square level a buffer must have
 *   to be considered actual signal, rather than silence/noise floor. Type: Double
 */
class SilenceGate(private val rmsThreshold: Double = 0.01) {

    /**
     * @param samples Audio buffer to check, as PCM float samples in [-1, 1]. Type: FloatArray
     * @return True if the buffer's RMS level is at or above rmsThreshold. Type: Boolean
     */
    fun hasSignal(samples: FloatArray): Boolean {
        return rootMeanSquare(samples) >= rmsThreshold
    }

    /**
     * @param samples Audio buffer to measure. Type: FloatArray
     * @return The buffer's root-mean-square amplitude. Type: Double
     */
    private fun rootMeanSquare(samples: FloatArray): Double {
        if (samples.isEmpty()) return 0.0
        val sumOfSquares = samples.sumOf { it.toDouble() * it.toDouble() }
        return sqrt(sumOfSquares / samples.size)
    }
}