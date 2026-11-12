package com.example.wavefret.tuner

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetedTuningEvaluatorTest {

    private val evaluator = TargetedTuningEvaluator()
    private val lowE = BassTunings.STANDARD.strings[0]
    private val gString = BassTunings.STANDARD.strings[3]

    @Test
    fun measuresAgainstTheChosenStringOnly() {
        val result = evaluator.centsFrom(lowE, 41.2)

        assertEquals(0.0, result, 0.5)
    }

    @Test
    fun theOctaveHarmonicCountsAsTheChosenString() {
        assertEquals(0.0, evaluator.centsFrom(lowE, 82.4), 0.5)
    }

    @Test
    fun theThirdHarmonicCountsAsTheChosenString() {
        assertEquals(0.0, evaluator.centsFrom(lowE, 123.6), 0.5)
    }

    @Test
    fun aReadingAnOctaveLowIsFoldedUp() {
        assertEquals(0.0, evaluator.centsFrom(gString, 49.0), 0.5)
    }

    @Test
    fun aSharpStringReadsPositive() {
        // 43 Hz is about 74 cents above low E
        assertEquals(74.0, evaluator.centsFrom(lowE, 43.0), 1.0)
    }

    @Test
    fun aFlatHarmonicReadsFlat() {
        // 80 Hz is the octave harmonic of a string about 51 cents flat
        assertEquals(-51.0, evaluator.centsFrom(lowE, 80.0), 1.0)
    }
}
