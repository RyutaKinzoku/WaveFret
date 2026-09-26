package com.example.wavefret.common.audio

import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates synthetic PCM waveforms for testing pitch-detection logic
 * without a real microphone. Test fixture only — lives in the test source
 * set, never referenced from main.
 */
object SyntheticSignals {

    private const val FUNDAMENTAL_AMPLITUDE = 0.15
    private const val SECOND_HARMONIC_AMPLITUDE = 0.5
    private const val THIRD_HARMONIC_AMPLITUDE = 0.3

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
}