package com.example.wavefret.common.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fourth-order low-pass filter: two cascaded second-order Butterworth
 * sections, with coefficients from the RBJ Audio EQ Cookbook. Used before
 * pitch detection to remove content above the bass range, where there is
 * only broadband noise (mic hiss, quantization, pick and fret clicks) that
 * makes a real note look less periodic than it is.
 *
 * Each call filters its buffer independently, starting from silence, so
 * the class keeps no audio state between calls and is safe to reuse.
 *
 * @property cutoffHz Frequency above which content is attenuated, in Hz. Type: Double
 */
class LowPassFilter(private val cutoffHz: Double) {

    init {
        require(cutoffHz > 0.0) { "cutoffHz must be positive" }
    }

    /**
     * Holds one second-order section's normalized coefficients.
     *
     * @property feedforward0 Weight of the current input sample. Type: Double
     * @property feedforward1 Weight of the previous input sample. Type: Double
     * @property feedforward2 Weight of the input sample two steps back. Type: Double
     * @property feedback1 Weight of the previous output sample. Type: Double
     * @property feedback2 Weight of the output sample two steps back. Type: Double
     */
    private class SectionCoefficients(
        val feedforward0: Double,
        val feedforward1: Double,
        val feedforward2: Double,
        val feedback1: Double,
        val feedback2: Double
    )

    /**
     * @param samples Audio buffer to filter, as PCM float samples. Type: FloatArray
     * @param sampleRate Sample rate the buffer was captured at, in Hz. Type: Int
     * @return A new buffer of the same size with content above cutoffHz removed. Type: FloatArray
     */
    fun filter(samples: FloatArray, sampleRate: Int): FloatArray {
        require(cutoffHz < sampleRate / 2.0) { "cutoffHz must be below the Nyquist frequency" }
        val coefficients = computeButterworthCoefficients(sampleRate)
        var filteredSamples = samples
        repeat(SECTION_COUNT) { filteredSamples = applySection(filteredSamples, coefficients) }
        return filteredSamples
    }

    /**
     * Computes the RBJ cookbook low-pass coefficients for a Butterworth
     * (maximally flat) response, normalized so the output weight is 1.
     *
     * @param sampleRate Sample rate the filter will run at, in Hz. Type: Int
     * @return Coefficients for one second-order section. Type: SectionCoefficients
     */
    private fun computeButterworthCoefficients(sampleRate: Int): SectionCoefficients {
        val angularFrequency = 2.0 * PI * cutoffHz / sampleRate
        val cosine = cos(angularFrequency)
        val alpha = sin(angularFrequency) / (2.0 * BUTTERWORTH_Q)
        val normalization = 1.0 + alpha
        return SectionCoefficients(
            feedforward0 = (1.0 - cosine) / 2.0 / normalization,
            feedforward1 = (1.0 - cosine) / normalization,
            feedforward2 = (1.0 - cosine) / 2.0 / normalization,
            feedback1 = -2.0 * cosine / normalization,
            feedback2 = (1.0 - alpha) / normalization
        )
    }

    /**
     * Runs one second-order section over the buffer (direct form I).
     *
     * @param input Buffer to filter. Type: FloatArray
     * @param coefficients Section coefficients. Type: SectionCoefficients
     * @return The filtered buffer. Type: FloatArray
     */
    private fun applySection(input: FloatArray, coefficients: SectionCoefficients): FloatArray {
        val output = FloatArray(input.size)
        var previousInput = 0.0
        var olderInput = 0.0
        var previousOutput = 0.0
        var olderOutput = 0.0
        for (index in input.indices) {
            val currentInput = input[index].toDouble()
            val currentOutput = coefficients.feedforward0 * currentInput +
                    coefficients.feedforward1 * previousInput +
                    coefficients.feedforward2 * olderInput -
                    coefficients.feedback1 * previousOutput -
                    coefficients.feedback2 * olderOutput
            olderInput = previousInput
            previousInput = currentInput
            olderOutput = previousOutput
            previousOutput = currentOutput
            output[index] = currentOutput.toFloat()
        }
        return output
    }

    companion object {
        private const val SECTION_COUNT = 2
        private val BUTTERWORTH_Q = 1.0 / sqrt(2.0)
    }
}