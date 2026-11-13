package com.example.wavefret.common.audio

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Synthesizes a plucked bass string (the Karplus–Strong method): a short
 * burst of soft noise repeats once per period, and each repetition is a
 * little quieter and duller than the last, which sounds like a string
 * that is plucked and then rings out. The dulling filter is symmetric, so
 * the period stays a whole number of samples and the pitch is exact. The noise has a fixed seed,
 * so the same note always sounds the same.
 *
 * @property seed Seed of the pluck's noise. Type: Long
 */
class PluckedStringSynthesizer(private val seed: Long = DEFAULT_SEED) {

    /**
     * @param frequencyHz Pitch of the note, in Hz. Type: Double
     * @param sampleCount Length of the note in samples; its last few milliseconds fade to silence. Type: Int
     * @param sampleRate Samples per second. Type: Int
     * @return The note as PCM float samples between -1 and 1. Type: FloatArray
     */
    fun pluck(frequencyHz: Double, sampleCount: Int, sampleRate: Int): FloatArray {
        val period = (sampleRate / frequencyHz).roundToInt().coerceAtLeast(MINIMUM_PERIOD)
        val random = Random(seed)
        val pluck = DoubleArray(period) { random.nextDouble(-1.0, 1.0) }
        repeat(SOFTENING_PASSES) { soften(pluck) }
        val peak = pluck.maxOf { abs(it) }.takeIf { it > 0.0 } ?: 1.0

        val string = DoubleArray(sampleCount)
        for (index in 0 until sampleCount) {
            string[index] = if (index < period) {
                pluck[index] / peak
            } else {
                val earlier = if (index - period - 1 >= 0) string[index - period - 1] else string[index - 1]
                DECAY_PER_PASS * (earlier / 4 + string[index - period] / 2 + string[index - period + 1] / 4)
            }
        }
        val output = FloatArray(sampleCount) { index -> (string[index] * OUTPUT_GAIN).toFloat() }
        fadeOut(output, (sampleRate * RELEASE_SECONDS).toInt())
        return output
    }

    /**
     * Averages each value with its neighbour, so the pluck is round like a
     * finger on a bass rather than sharp like a pick on a guitar.
     *
     * @param string One period of the pluck, softened in place. Type: DoubleArray
     * @return Unit
     */
    private fun soften(string: DoubleArray) {
        val first = string[0]
        for (index in 0 until string.size - 1) string[index] = (string[index] + string[index + 1]) / 2
        string[string.size - 1] = (string[string.size - 1] + first) / 2
    }

    /**
     * @param samples Note to fade in place. Type: FloatArray
     * @param releaseSampleCount Samples over which the end fades to silence. Type: Int
     * @return Unit
     */
    private fun fadeOut(samples: FloatArray, releaseSampleCount: Int) {
        val fadeLength = minOf(releaseSampleCount, samples.size)
        if (fadeLength <= 1) {
            samples.fill(0f, samples.size - fadeLength)
            return
        }
        val fadeStart = samples.size - fadeLength
        for (index in fadeStart until samples.size) {
            samples[index] *= (samples.size - 1 - index).toFloat() / (fadeLength - 1)
        }
    }

    private companion object {
        const val DEFAULT_SEED = 7L
        const val MINIMUM_PERIOD = 2
        const val SOFTENING_PASSES = 3
        const val OUTPUT_GAIN = 0.8
        const val DECAY_PER_PASS = 0.998
        const val RELEASE_SECONDS = 0.012
    }
}
