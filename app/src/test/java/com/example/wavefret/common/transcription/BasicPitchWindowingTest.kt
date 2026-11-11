package com.example.wavefret.common.transcription

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BasicPitchWindowingTest {

    private val windowing = BasicPitchWindowing()

    @Test
    fun everyWindowHasTheModelInputLength() {
        val windows = windowing.windows(FloatArray(100_000) { 0.1f })

        assertEquals(true, windows.all { it.size == BasicPitchWindowing.WINDOW_SAMPLES })
    }

    @Test
    fun audioIsPaddedByHalfAnOverlapAtTheStart() {
        val windows = windowing.windows(FloatArray(10) { 1f })

        assertEquals(0f, windows.first()[BasicPitchWindowing.OVERLAP_SAMPLES / 2 - 1])
        assertEquals(1f, windows.first()[BasicPitchWindowing.OVERLAP_SAMPLES / 2])
    }

    @Test
    fun windowsAdvanceByTheHopSize() {
        val audio = FloatArray(100_000) { index -> index.toFloat() }

        val windows = windowing.windows(audio)

        val hop = BasicPitchWindowing.WINDOW_SAMPLES - BasicPitchWindowing.OVERLAP_SAMPLES
        assertEquals(windows[0][hop], windows[1][0])
    }

    @Test
    fun enoughWindowsToCoverTheWholeAudio() {
        val windows = windowing.windows(FloatArray(100_000))
        val hop = BasicPitchWindowing.WINDOW_SAMPLES - BasicPitchWindowing.OVERLAP_SAMPLES

        assertEquals(true, windows.size * hop >= 100_000 + BasicPitchWindowing.OVERLAP_SAMPLES / 2)
    }

    @Test
    fun stitchingDropsHalfTheOverlappingFramesOnEachSideAndTrimsToTheAudio() {
        // Each window's frame f holds value (window * 1000 + f) in every bin.
        val framesPerWindow = BasicPitchWindowing.FRAMES_PER_WINDOW
        val windowOutputs = (0 until 3).map { window ->
            Array(framesPerWindow) { frame -> FloatArray(2) { (window * 1000 + frame).toFloat() } }
        }
        val audioSamples = 50_000

        val stitched = windowing.stitch(windowOutputs, audioSamples)

        val trimFrames = BasicPitchWindowing.OVERLAP_FRAMES / 2
        assertArrayEquals(floatArrayOf(trimFrames.toFloat(), trimFrames.toFloat()), stitched.first(), 0f)
        assertArrayEquals(floatArrayOf(1000f + trimFrames, 1000f + trimFrames), stitched[framesPerWindow - 2 * trimFrames], 0f)
        assertEquals(windowing.frameCountFor(audioSamples), stitched.size)
    }

    @Test
    fun frameRateMatchesTheModelHop() {
        assertEquals(22_050.0 / 256.0, BasicPitchWindowing.FRAMES_PER_SECOND, 1e-9)
    }
}
