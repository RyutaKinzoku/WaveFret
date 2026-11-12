package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class DecimatingPitchDetectorTest {

    private val detector = DecimatingPitchDetector(YinPitchDetector(), decimationFactor = 4)

    @Test
    fun findsLowEFromAQuarterOfTheSamples() {
        val lowE = SyntheticSignals.bassLikeTone(41.2, 44_100, 4096)

        assertEquals(41.2, detector.detectPitch(lowE, 44_100)!!, 0.3)
    }

    @Test
    fun findsTheGString() {
        val gString = SyntheticSignals.bassLikeTone(98.0, 44_100, 4096)

        assertEquals(98.0, detector.detectPitch(gString, 44_100)!!, 0.5)
    }

    @Test
    fun silenceHasNoPitch() {
        assertEquals(null, detector.detectPitch(FloatArray(4096), 44_100))
    }
}
