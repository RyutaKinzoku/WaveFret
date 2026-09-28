package com.example.wavefret.common.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates synthetic PCM waveforms for testing pitch-detection logic
 * without a real microphone. Test fixture only — lives in the test source
 * set, never referenced from main.
 */
object SyntheticSignals {

    private const val FUNDAMENTAL_AMPLITUDE = 0.15
    private const val SECOND_HARMONIC_AMPLITUDE = 0.5
    private const val THIRD_HARMONIC_AMPLITUDE = 0.3
    private const val EVEN_HEAVY_FUNDAMENTAL_AMPLITUDE = 0.1
    private const val EVEN_HEAVY_SECOND_HARMONIC_AMPLITUDE = 0.6
    private const val EVEN_HEAVY_THIRD_HARMONIC_AMPLITUDE = 0.1
    private const val EVEN_HEAVY_FOURTH_HARMONIC_AMPLITUDE = 0.2

    /**
     * Generates a tone whose even harmonics dominate while the fundamental
     * and odd harmonics are weak — how an acoustic bass's low E reaches a
     * phone mic once its short-lived fundamental has faded. The waveform
     * repeats almost exactly every half period, which is the trap that
     * made the tuner read a low E (41.2 Hz) as 82.4 Hz, shown as D.
     *
     * @param fundamentalHz Fundamental frequency of the note, in Hz. Type: Double
     * @param sampleRate Samples per second used to generate the wave. Type: Int
     * @param sampleCount Number of samples to generate. Type: Int
     * @return A composite waveform as PCM float samples. Type: FloatArray
     */
    fun evenHarmonicHeavyTone(
        fundamentalHz: Double,
        sampleRate: Int,
        sampleCount: Int
    ): FloatArray {
        val harmonicAmplitudes = listOf(
            EVEN_HEAVY_FUNDAMENTAL_AMPLITUDE,
            EVEN_HEAVY_SECOND_HARMONIC_AMPLITUDE,
            EVEN_HEAVY_THIRD_HARMONIC_AMPLITUDE,
            EVEN_HEAVY_FOURTH_HARMONIC_AMPLITUDE
        )
        val harmonics = harmonicAmplitudes.mapIndexed { harmonicIndex, amplitude ->
            sine(fundamentalHz * (harmonicIndex + 1), sampleRate, sampleCount, amplitude)
        }
        return FloatArray(sampleCount) { index -> harmonics.sumOf { it[index].toDouble() }.toFloat() }
    }

    /**
     * Adds two equally sized signals sample by sample, e.g. a tone plus
     * background noise.
     *
     * @param firstSignal First signal to add. Type: FloatArray
     * @param secondSignal Second signal to add, same size as the first. Type: FloatArray
     * @return The sample-by-sample sum. Type: FloatArray
     */
    fun mix(firstSignal: FloatArray, secondSignal: FloatArray): FloatArray {
        require(firstSignal.size == secondSignal.size) { "Signals to mix must have the same size" }
        return FloatArray(firstSignal.size) { index -> firstSignal[index] + secondSignal[index] }
    }

    /**
     * @param frequencyHz Frequency of the sine wave, in Hz. Type: Double
     * @param sampleRate Samples per second used to generate the wave. Type: Int
     * @param sampleCount Number of samples to generate. Type: Int
     * @param amplitude Peak amplitude of the wave, from 0.0 to 1.0. Type: Double
     * @return A pure sine wave as PCM float samples. Type: FloatArray
     */
    fun sine(
        frequencyHz: Double,
        sampleRate: Int,
        sampleCount: Int,
        amplitude: Double = 1.0
    ): FloatArray {
        return FloatArray(sampleCount) { index ->
            (amplitude * sin(2.0 * PI * frequencyHz * index / sampleRate)).toFloat()
        }
    }

    /**
     * Generates a tone shaped like what a phone microphone actually
     * captures from a bass guitar: a weak fundamental with a strong 2nd
     * and 3rd harmonic, since phone mics roll off the low frequencies a
     * bass fundamental lives at. Used to test that pitch detection finds
     * the true fundamental instead of locking onto the loudest harmonic.
     *
     * @param fundamentalHz Fundamental frequency of the note, in Hz. Type: Double
     * @param sampleRate Samples per second used to generate the wave. Type: Int
     * @param sampleCount Number of samples to generate. Type: Int
     * @return A composite waveform as PCM float samples. Type: FloatArray
     */
    fun bassLikeTone(
        fundamentalHz: Double,
        sampleRate: Int,
        sampleCount: Int
    ): FloatArray {
        val fundamental = sine(fundamentalHz, sampleRate, sampleCount, amplitude = FUNDAMENTAL_AMPLITUDE)
        val secondHarmonic = sine(fundamentalHz * 2, sampleRate, sampleCount, amplitude = SECOND_HARMONIC_AMPLITUDE)
        val thirdHarmonic = sine(fundamentalHz * 3, sampleRate, sampleCount, amplitude = THIRD_HARMONIC_AMPLITUDE)
        return FloatArray(sampleCount) { index ->
            fundamental[index] + secondHarmonic[index] + thirdHarmonic[index]
        }
    }

    /**
     * @param sampleCount Number of samples to generate. Type: Int
     * @return A buffer of all-zero samples, representing silence. Type: FloatArray
     */
    fun silence(sampleCount: Int): FloatArray = FloatArray(sampleCount)

    /**
     * Generates deterministic pseudo-random noise, useful for testing that
     * pitch detection correctly reports "no pitch" for non-periodic input.
     *
     * @param sampleCount Number of samples to generate. Type: Int
     * @param amplitude Peak amplitude of the noise, from 0.0 to 1.0. Type: Double
     * @param seed Random seed, fixed by default so tests are reproducible. Type: Long
     * @return A buffer of random samples in [-amplitude, amplitude]. Type: FloatArray
     */
    fun whiteNoise(sampleCount: Int, amplitude: Double = 1.0, seed: Long = 42L): FloatArray {
        val random = Random(seed)
        return FloatArray(sampleCount) { random.nextDouble(-amplitude, amplitude).toFloat() }
    }
}