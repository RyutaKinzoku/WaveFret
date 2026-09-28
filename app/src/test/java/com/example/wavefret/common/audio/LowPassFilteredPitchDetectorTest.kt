package com.example.wavefret.common.audio

import com.example.wavefret.common.music.centsBetween
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LowPassFilteredPitchDetectorTest {

    /**
     * Records what it was asked to analyze and returns a fixed result, so
     * tests can check what the decorator hands to the detector it wraps.
     *
     * @param scriptedFrequencyHz Result to return from every call. Type: Double?
     */
    private class CapturingPitchDetector(private val scriptedFrequencyHz: Double?) : PitchDetector {
        var receivedSamples: FloatArray? = null
        var receivedSampleRate: Int? = null

        /**
         * @param samples Buffer handed over by the decorator; recorded. Type: FloatArray
         * @param sampleRate Sample rate handed over by the decorator; recorded. Type: Int
         * @return The scripted frequency. Type: Double?
         */
        override fun detectPitch(samples: FloatArray, sampleRate: Int): Double? {
            receivedSamples = samples
            receivedSampleRate = sampleRate
            return scriptedFrequencyHz
        }
    }

    private val lowPassFilter = LowPassFilter(cutoffHz = CUTOFF_HZ)

    private fun bassNoteWithHighFrequencyHiss(): FloatArray {
        return SyntheticSignals.mix(
            SyntheticSignals.sine(frequencyHz = 98.0, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT),
            SyntheticSignals.sine(frequencyHz = 6000.0, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT, amplitude = 0.5)
        )
    }

    @Test
    fun handsLowPassFilteredSamplesToInnerDetector() {
        val innerPitchDetector = CapturingPitchDetector(scriptedFrequencyHz = 98.0)
        val input = bassNoteWithHighFrequencyHiss()

        LowPassFilteredPitchDetector(lowPassFilter, innerPitchDetector).detectPitch(input, SAMPLE_RATE)

        assertArrayEquals(lowPassFilter.filter(input, SAMPLE_RATE), innerPitchDetector.receivedSamples, 0f)
    }

    @Test
    fun handsSampleRateToInnerDetectorUnchanged() {
        val innerPitchDetector = CapturingPitchDetector(scriptedFrequencyHz = 98.0)

        LowPassFilteredPitchDetector(lowPassFilter, innerPitchDetector).detectPitch(bassNoteWithHighFrequencyHiss(), SAMPLE_RATE)

        assertEquals(SAMPLE_RATE, innerPitchDetector.receivedSampleRate)
    }

    @Test
    fun returnsInnerDetectorResult() {
        val innerPitchDetector = CapturingPitchDetector(scriptedFrequencyHz = 73.42)

        val detected = LowPassFilteredPitchDetector(lowPassFilter, innerPitchDetector)
            .detectPitch(bassNoteWithHighFrequencyHiss(), SAMPLE_RATE)

        assertEquals(73.42, detected!!, 0.0)
    }

    @Test
    fun recognizesLowEStringBuriedInBroadbandNoise() {
        // Plain YinPitchDetector returns null for this buffer: noise about as loud as
        // the note, spread across the whole spectrum, makes it look non-periodic.
        // Filtering first lets the low E through. This checks the note is recognized
        // (well inside E, nowhere near A); precision on clean input is covered by
        // YinPitchDetectorTest.
        val noisyLowE = SyntheticSignals.mix(
            SyntheticSignals.sine(frequencyHz = 41.20, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT, amplitude = 0.3),
            SyntheticSignals.whiteNoise(sampleCount = SAMPLE_COUNT, amplitude = 0.25)
        )

        val detected = LowPassFilteredPitchDetector(lowPassFilter, YinPitchDetector()).detectPitch(noisyLowE, SAMPLE_RATE)

        assertNotNull("Expected a pitch near 41.2 Hz but got null", detected)
        val centsOff = abs(centsBetween(referenceHz = 41.20, frequencyHz = detected!!))
        assertTrue("Detected $detected Hz ($centsOff cents off)", centsOff <= RECOGNIZED_NOTE_TOLERANCE_CENTS)
    }

    @Test
    fun returnsNullForBroadbandNoiseAlone() {
        val noiseOnly = SyntheticSignals.whiteNoise(sampleCount = SAMPLE_COUNT, amplitude = 0.5)

        val detected = LowPassFilteredPitchDetector(lowPassFilter, YinPitchDetector()).detectPitch(noiseOnly, SAMPLE_RATE)

        assertNull(detected)
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val SAMPLE_COUNT = 8192
        private const val CUTOFF_HZ = 1000.0
        private const val RECOGNIZED_NOTE_TOLERANCE_CENTS = 25.0
    }
}