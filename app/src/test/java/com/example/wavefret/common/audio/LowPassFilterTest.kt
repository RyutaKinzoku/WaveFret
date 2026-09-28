package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LowPassFilterTest {

    private val lowPassFilter = LowPassFilter(cutoffHz = CUTOFF_HZ)

    private fun peakAfterSettling(samples: FloatArray): Float {
        return samples.drop(SETTLING_SAMPLE_COUNT).maxOf { abs(it) }
    }

    @Test
    fun filterPreservesSampleCount() {
        val samples = SyntheticSignals.sine(frequencyHz = 100.0, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        assertEquals(SAMPLE_COUNT, lowPassFilter.filter(samples, SAMPLE_RATE).size)
    }

    @Test
    fun filterPassesBassFrequenciesAlmostUnchanged() {
        val samples = SyntheticSignals.sine(frequencyHz = 100.0, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val filtered = lowPassFilter.filter(samples, SAMPLE_RATE)
        assertEquals(1.0f, peakAfterSettling(filtered), 0.02f)
    }

    @Test
    fun filterRemovesFrequenciesWellAboveCutoff() {
        val samples = SyntheticSignals.sine(frequencyHz = 5000.0, sampleRate = SAMPLE_RATE, sampleCount = SAMPLE_COUNT)
        val filtered = lowPassFilter.filter(samples, SAMPLE_RATE)
        assertTrue(peakAfterSettling(filtered) < 0.01f)
    }

    @Test
    fun filterKeepsSilenceSilent() {
        val filtered = lowPassFilter.filter(SyntheticSignals.silence(SAMPLE_COUNT), SAMPLE_RATE)
        assertTrue(filtered.all { it == 0f })
    }

    @Test
    fun constructorRejectsNonPositiveCutoff() {
        assertThrows(IllegalArgumentException::class.java) { LowPassFilter(cutoffHz = 0.0) }
    }

    @Test
    fun filterRejectsCutoffAtOrAboveNyquist() {
        val tooHighCutoffFilter = LowPassFilter(cutoffHz = 22050.0)
        assertThrows(IllegalArgumentException::class.java) {
            tooHighCutoffFilter.filter(SyntheticSignals.silence(SAMPLE_COUNT), SAMPLE_RATE)
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val SAMPLE_COUNT = 8192
        private const val CUTOFF_HZ = 1000.0
        private const val SETTLING_SAMPLE_COUNT = 1000
    }
}