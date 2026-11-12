package com.example.wavefret.common.audio

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SlidingWindowAudioSourceTest {

    private fun chunks(vararg values: List<Int>) = values.map { chunk -> chunk.map { it.toFloat() }.toFloatArray() }

    @Test
    fun emitsAFullWindowEveryHopOnceEnoughAudioArrived() = runTest {
        val innerSource = FakePcmAudioSource(chunks(listOf(1, 2, 3), listOf(4, 5, 6)), sampleRateHz = 8_000)
        val slidingSource = SlidingWindowAudioSource(innerSource, windowSamples = 4, hopSamples = 2)

        val windows = slidingSource.audioBufferStream().toList().map { window -> window.map { it.toInt() } }

        assertEquals(listOf(listOf(1, 2, 3, 4), listOf(3, 4, 5, 6)), windows)
    }

    @Test
    fun manySmallChunksStillGiveOneWindowPerHop() = runTest {
        val innerSource = FakePcmAudioSource((1..8).map { floatArrayOf(it.toFloat()) }, sampleRateHz = 8_000)
        val slidingSource = SlidingWindowAudioSource(innerSource, windowSamples = 4, hopSamples = 2)

        assertEquals(3, slidingSource.audioBufferStream().toList().size)
    }

    @Test
    fun reportsTheInnerSampleRate() {
        val slidingSource = SlidingWindowAudioSource(FakePcmAudioSource(emptyList(), sampleRateHz = 22_050), 4, 2)

        assertEquals(22_050, slidingSource.sampleRateHz)
    }
}
