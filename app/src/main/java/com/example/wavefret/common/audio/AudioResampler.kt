package com.example.wavefret.common.audio

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Converts decoded audio into the shape the analysers expect: one channel,
 * at a chosen sample rate. Uses linear interpolation, which is enough for
 * bass-range pitch analysis.
 */
class AudioResampler {

    /**
     * @param interleavedSamples Samples with channels interleaved (L R L R…). Type: FloatArray
     * @param channelCount Number of channels. Type: Int
     * @return One averaged channel. Type: FloatArray
     */
    fun toMono(interleavedSamples: FloatArray, channelCount: Int): FloatArray {
        require(channelCount > 0) { "Audio must have at least one channel" }
        if (channelCount == 1) return interleavedSamples
        val frameCount = interleavedSamples.size / channelCount
        return FloatArray(frameCount) { frame ->
            var sum = 0f
            for (channel in 0 until channelCount) sum += interleavedSamples[frame * channelCount + channel]
            sum / channelCount
        }
    }

    /**
     * @param samples Mono samples. Type: FloatArray
     * @param fromRateHz Their sample rate. Type: Int
     * @param toRateHz Wanted sample rate. Type: Int
     * @return The samples at the wanted rate. Type: FloatArray
     */
    fun resample(samples: FloatArray, fromRateHz: Int, toRateHz: Int): FloatArray {
        require(fromRateHz > 0 && toRateHz > 0) { "Sample rates must be positive" }
        if (fromRateHz == toRateHz || samples.isEmpty()) return samples
        val outputLength = (samples.size.toLong() * toRateHz / fromRateHz.toDouble()).roundToInt()
        val step = fromRateHz.toDouble() / toRateHz
        return FloatArray(outputLength) { outputIndex ->
            val sourcePosition = outputIndex * step
            val lowerIndex = floor(sourcePosition).toInt().coerceAtMost(samples.size - 1)
            val upperIndex = (lowerIndex + 1).coerceAtMost(samples.size - 1)
            val fraction = (sourcePosition - lowerIndex).toFloat().coerceIn(0f, 1f)
            samples[lowerIndex] + (samples[upperIndex] - samples[lowerIndex]) * fraction
        }
    }
}
