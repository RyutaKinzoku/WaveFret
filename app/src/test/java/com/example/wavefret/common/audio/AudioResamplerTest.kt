package com.example.wavefret.common.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioResamplerTest {

    private val audioResampler = AudioResampler()

    @Test
    fun stereoIsAveragedToMono() {
        val mono = audioResampler.toMono(floatArrayOf(1f, 0f, 0.5f, 0.5f, -1f, 1f), channelCount = 2)

        assertArrayEquals(floatArrayOf(0.5f, 0.5f, 0f), mono, 1e-6f)
    }

    @Test
    fun monoIsReturnedUnchanged() {
        assertArrayEquals(floatArrayOf(0.1f, 0.2f), audioResampler.toMono(floatArrayOf(0.1f, 0.2f), 1), 1e-6f)
    }

    @Test
    fun halvingTheRateKeepsEveryOtherSample() {
        val resampled = audioResampler.resample(floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f), fromRateHz = 44_100, toRateHz = 22_050)

        assertArrayEquals(floatArrayOf(0f, 2f, 4f), resampled, 1e-6f)
    }

    @Test
    fun upsamplingInterpolatesBetweenSamples() {
        val resampled = audioResampler.resample(floatArrayOf(0f, 1f), fromRateHz = 1, toRateHz = 2)

        assertArrayEquals(floatArrayOf(0f, 0.5f, 1f, 1f), resampled, 1e-6f)
    }

    @Test
    fun resamplingASinePreservesItsPitch() {
        val sine = SyntheticSignals.sine(110.0, 44_100, 44_100)

        val resampled = audioResampler.resample(sine, 44_100, 22_050)

        assertEquals(22_050, resampled.size)
        assertEquals(110.0, YinPitchDetector().detectPitch(resampled.copyOfRange(0, 4096), 22_050)!!, 1.0)
    }
}
