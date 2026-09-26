package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SyntheticSignalsTest {

    @Test
    fun sineReturnsRequestedSampleCount() {
        val samples = SyntheticSignals.sine(frequencyHz = 220.0, sampleRate = 44100, sampleCount = 1000)
        assertEquals(1000, samples.size)
    }

    @Test
    fun sineReachesRequestedAmplitudeAtQuarterPeriod() {
        // 441 Hz at a 44100 Hz sample rate gives an exact 100-sample period,
        // so sample 25 (a quarter period in) lands exactly on the wave's peak.
        val samples = SyntheticSignals.sine(frequencyHz = 441.0, sampleRate = 44100, sampleCount = 26, amplitude = 1.0)
        assertEquals(1.0f, samples[25], 0.0001f)
    }

    @Test
    fun sineScalesWithRequestedAmplitude() {
        val samples = SyntheticSignals.sine(frequencyHz = 441.0, sampleRate = 44100, sampleCount = 26, amplitude = 0.5)
        assertEquals(0.5f, samples[25], 0.0001f)
    }

    @Test
    fun bassLikeToneReturnsRequestedSampleCount() {
        val samples = SyntheticSignals.bassLikeTone(fundamentalHz = 41.2, sampleRate = 44100, sampleCount = 4096)
        assertEquals(4096, samples.size)
    }

    @Test
    fun bassLikeTonePeakAmplitudeNeverClips() {
        val samples = SyntheticSignals.bassLikeTone(fundamentalHz = 41.2, sampleRate = 44100, sampleCount = 4096)
        val peak = samples.maxOf { abs(it) }
        assertTrue(peak <= 1.0f)
    }

    @Test
    fun bassLikeToneHasHarmonicContentDifferentFromAPureSine() {
        val bassLike = SyntheticSignals.bassLikeTone(fundamentalHz = 110.0, sampleRate = 44100, sampleCount = 2000)
        val pureSine = SyntheticSignals.sine(frequencyHz = 110.0, sampleRate = 44100, sampleCount = 2000)
        assertNotEquals(pureSine.toList(), bassLike.toList())
    }

    @Test
    fun silenceReturnsAllZeroSamples() {
        val samples = SyntheticSignals.silence(500)
        assertTrue(samples.all { it == 0f })
    }

    @Test
    fun whiteNoiseReturnsRequestedSampleCount() {
        val samples = SyntheticSignals.whiteNoise(sampleCount = 500)
        assertEquals(500, samples.size)
    }

    @Test
    fun whiteNoiseStaysWithinRequestedAmplitude() {
        val samples = SyntheticSignals.whiteNoise(sampleCount = 500, amplitude = 0.5)
        assertTrue(samples.all { abs(it) <= 0.5f })
    }
}