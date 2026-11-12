package com.example.wavefret.common.audio

import kotlin.math.sqrt

/**
 * A noise gate that learns the room: it tracks the background noise level
 * and lets through anything clearly above it, so softly plucked notes are
 * heard in a quiet room while a noisy room's hum is still ignored.
 *
 * The noise floor drops straight to any quieter buffer and creeps up only
 * slowly (a fraction of a percent per buffer), so a long sustained note is
 * not mistaken for background noise.
 *
 * @property minimumRms Level below which a buffer is always silent. Type: Double
 * @property signalToNoiseRatio How many times louder than the noise floor a signal must be. Type: Double
 * @property noiseFloorRisePerBuffer Factor the noise floor may rise by per louder buffer. Type: Double
 */
class AdaptiveSilenceGate(
    private val minimumRms: Double = DEFAULT_MINIMUM_RMS,
    private val signalToNoiseRatio: Double = DEFAULT_SIGNAL_TO_NOISE_RATIO,
    private val noiseFloorRisePerBuffer: Double = DEFAULT_NOISE_FLOOR_RISE_PER_BUFFER
) : SignalGate {

    private var noiseFloorRms = minimumRms

    /**
     * @param samples Audio buffer in -1..1. Type: FloatArray
     * @return True if the buffer is clearly louder than the background. Type: Boolean
     */
    override fun hasSignal(samples: FloatArray): Boolean {
        val bufferRms = rootMeanSquare(samples)
        noiseFloorRms = if (bufferRms < noiseFloorRms) bufferRms else minOf(bufferRms, noiseFloorRms * noiseFloorRisePerBuffer)
        return bufferRms >= maxOf(minimumRms, noiseFloorRms * signalToNoiseRatio)
    }

    /**
     * @param samples Audio buffer. Type: FloatArray
     * @return Its root-mean-square level. Type: Double
     */
    private fun rootMeanSquare(samples: FloatArray): Double {
        if (samples.isEmpty()) return 0.0
        var sumOfSquares = 0.0
        for (sample in samples) sumOfSquares += sample * sample.toDouble()
        return sqrt(sumOfSquares / samples.size)
    }

    private companion object {
        const val DEFAULT_MINIMUM_RMS = 0.002
        const val DEFAULT_SIGNAL_TO_NOISE_RATIO = 3.0
        const val DEFAULT_NOISE_FLOOR_RISE_PER_BUFFER = 1.004
    }
}
