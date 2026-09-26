package com.example.wavefret.common.audio

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln

class YinPitchDetectorTest {

    private fun centsBetween(expectedHz: Double, actualHz: Double): Double {
        return 1200.0 * ln(actualHz / expectedHz) / ln(2.0)
    }

    private fun assertWithinCents(expectedHz: Double, actualHz: Double?, toleranceCents: Double = 3.0) {
        assertNotNull("Expected a detected pitch near $expectedHz Hz but got null", actualHz)
        val centsOff = abs(centsBetween(expectedHz, actualHz!!))
        assertTrue(
            "Expected $expectedHz Hz within $toleranceCents cents, but detected $actualHz Hz ($centsOff cents off)",
            centsOff <= toleranceCents
        )
    }

    @Test
    fun detectPitchFindsFrequencyOfAPureSineWave() {
        val samples = SyntheticSignals.sine(frequencyHz = 110.0, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertWithinCents(110.0, detected)
    }

    @Test
    fun detectPitchFindsLowEStringFrequency() {
        val samples = SyntheticSignals.sine(frequencyHz = 41.20, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertWithinCents(41.20, detected)
    }

    @Test
    fun detectPitchFindsAStringFrequency() {
        val samples = SyntheticSignals.sine(frequencyHz = 55.00, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertWithinCents(55.00, detected)
    }

    @Test
    fun detectPitchFindsDStringFrequency() {
        val samples = SyntheticSignals.sine(frequencyHz = 73.42, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertWithinCents(73.42, detected)
    }

    @Test
    fun detectPitchFindsGStringFrequency() {
        val samples = SyntheticSignals.sine(frequencyHz = 98.00, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertWithinCents(98.00, detected)
    }

    @Test
    fun detectPitchFindsFundamentalNotHarmonicForABassLikeTone() {
        val samples = SyntheticSignals.bassLikeTone(fundamentalHz = 41.20, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertWithinCents(41.20, detected)
    }

    @Test
    fun detectPitchReturnsNullForSilence() {
        val samples = SyntheticSignals.silence(SAMPLE_COUNT)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertNull(detected)
    }

    @Test
    fun detectPitchReturnsNullForWhiteNoise() {
        val samples = SyntheticSignals.whiteNoise(sampleCount = SAMPLE_COUNT, amplitude = 0.5)
        val detected = YinPitchDetector().detectPitch(samples, SAMPLE_RATE)
        assertNull(detected)
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val SAMPLE_COUNT = 4096
    }
}