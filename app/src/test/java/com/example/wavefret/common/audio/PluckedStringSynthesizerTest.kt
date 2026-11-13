package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PluckedStringSynthesizerTest {

    private val sampleRate = 44_100
    private val synthesizer = PluckedStringSynthesizer()

    private fun pitchOf(samples: FloatArray): Double {
        val window = samples.copyOfRange(sampleRate / 10, sampleRate / 10 + 8_192)
        val pitch = YinPitchDetector().detectPitch(window, sampleRate)
        assertNotNull(pitch)
        return pitch!!
    }

    @Test
    fun aNoteHasTheRequestedLength() {
        assertEquals(22_050, synthesizer.pluck(98.0, 22_050, sampleRate).size)
    }

    @Test
    fun theLowEStringSoundsAtItsPitch() {
        val pitch = pitchOf(synthesizer.pluck(41.2, sampleRate, sampleRate))

        assertEquals(41.2, pitch, 41.2 * 0.01)
    }

    @Test
    fun aHigherNoteSoundsAtItsPitch() {
        val pitch = pitchOf(synthesizer.pluck(196.0, sampleRate, sampleRate))

        assertEquals(196.0, pitch, 196.0 * 0.01)
    }

    @Test
    fun aNoteIsAudibleAndNeverClips() {
        val samples = synthesizer.pluck(55.0, sampleRate, sampleRate)

        assertTrue(samples.maxOf { abs(it) } in 0.3f..1.0f)
    }

    @Test
    fun aNoteEndsInSilenceSoTheNextOneDoesNotClick() {
        val samples = synthesizer.pluck(55.0, sampleRate / 4, sampleRate)

        assertTrue(abs(samples.last()) < 0.001f)
    }
}
