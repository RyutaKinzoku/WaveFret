package com.example.wavefret.tuner

import com.example.wavefret.common.audio.SyntheticSignals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SilenceGateTest {

    private val silenceGate = SilenceGate(rmsThreshold = 0.01)

    @Test
    fun hasSignalReturnsFalseForSilence() {
        val samples = SyntheticSignals.silence(1000)
        assertFalse(silenceGate.hasSignal(samples))
    }

    @Test
    fun hasSignalReturnsFalseForVeryQuietNoise() {
        val samples = SyntheticSignals.whiteNoise(sampleCount = 1000, amplitude = 0.001)
        assertFalse(silenceGate.hasSignal(samples))
    }

    @Test
    fun hasSignalReturnsTrueForANormalPlayingLevelTone() {
        val samples = SyntheticSignals.sine(frequencyHz = 110.0, sampleRate = 44100, sampleCount = 1000, amplitude = 0.3)
        assertTrue(silenceGate.hasSignal(samples))
    }

    @Test
    fun hasSignalReturnsFalseRightAtTheBoundaryBelowThreshold() {
        // A sine of amplitude A has RMS = A / sqrt(2), so an amplitude
        // chosen from that relationship lands reliably just under threshold.
        val amplitudeJustBelowThreshold = 0.01 * sqrt(2.0) * 0.9
        val samples = SyntheticSignals.sine(
            frequencyHz = 110.0, sampleRate = 44100, sampleCount = 4096, amplitude = amplitudeJustBelowThreshold
        )
        assertFalse(silenceGate.hasSignal(samples))
    }

    @Test
    fun hasSignalReturnsTrueRightAtTheBoundaryAboveThreshold() {
        val amplitudeJustAboveThreshold = 0.01 * sqrt(2.0) * 1.1
        val samples = SyntheticSignals.sine(
            frequencyHz = 110.0, sampleRate = 44100, sampleCount = 4096, amplitude = amplitudeJustAboveThreshold
        )
        assertTrue(silenceGate.hasSignal(samples))
    }
}