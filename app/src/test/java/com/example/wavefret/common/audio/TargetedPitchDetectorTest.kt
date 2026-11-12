package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TargetedPitchDetectorTest {

    private val sampleRate = 44_100
    private val detector = TargetedPitchDetector()

    private fun softNoisyNote(frequencyHz: Double): FloatArray {
        val tone = SyntheticSignals.bassLikeTone(frequencyHz, sampleRate, 4096)
        val noise = SyntheticSignals.whiteNoise(4096, amplitude = 0.012, seed = 3L)
        return FloatArray(4096) { index -> tone[index] * 0.02f + noise[index] }
    }

    @Test
    fun hearsASoftNoisyNoteWhenItIsTheExpectedOne() {
        val heardHz = detector.detectNear(softNoisyNote(55.0), sampleRate, expectedFrequencyHz = 55.0)

        assertNotNull(heardHz)
        assertEquals(55.0, heardHz!!, 0.8)
    }

    @Test
    fun aSlightlyOutOfTuneNoteIsStillReportedWithItsRealPitch() {
        assertEquals(56.0, detector.detectNear(softNoisyNote(56.0), sampleRate, 55.0)!!, 0.8)
    }

    @Test
    fun aDifferentNoteIsNotMistakenForTheExpectedOne() {
        assertNull(detector.detectNear(softNoisyNote(73.42), sampleRate, expectedFrequencyHz = 55.0))
    }

    @Test
    fun theOctaveAboveIsNotMistakenForTheExpectedNote() {
        val octaveAbove = SyntheticSignals.sine(110.0, sampleRate, 4096)

        assertNull(detector.detectNear(octaveAbove, sampleRate, expectedFrequencyHz = 55.0))
    }

    @Test
    fun noiseAloneIsNotANote() {
        assertNull(detector.detectNear(SyntheticSignals.whiteNoise(4096, 0.02, 9L), sampleRate, 55.0))
    }

    @Test
    fun aRealBassToneWithAStrongSecondHarmonicIsStillHeard() {
        val lowE = SyntheticSignals.bassLikeTone(41.2, sampleRate, 4096)

        assertEquals(41.2, detector.detectNear(lowE, sampleRate, expectedFrequencyHz = 41.2)!!, 0.5)
    }

    @Test
    fun aToneDominatedByItsOctaveHarmonicIsStillHeardAsItsFundamental() {
        val harmonicHeavyA = SyntheticSignals.evenHarmonicHeavyTone(55.0, sampleRate, 4096)

        assertEquals(55.0, detector.detectNear(harmonicHeavyA, sampleRate, expectedFrequencyHz = 55.0)!!, 0.6)
    }
}
