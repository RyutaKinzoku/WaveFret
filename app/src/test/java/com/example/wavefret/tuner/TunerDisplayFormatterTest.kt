package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class TunerDisplayFormatterTest {

    private val formatter = TunerDisplayFormatter()

    @Test
    fun inTuneReadsZeroCentsWithTheNeedleUpright() {
        assertEquals("0¢", formatter.centsLabel(0.2))
        assertEquals(0f, formatter.needleRotationDegrees(0.0), 0.01f)
    }

    @Test
    fun sharpReadingsHaveAPlusSignAndAreRounded() {
        assertEquals("+8¢", formatter.centsLabel(7.6))
    }

    @Test
    fun flatReadingsHaveAMinusSign() {
        assertEquals("-12¢", formatter.centsLabel(-12.4))
    }

    @Test
    fun needleTurnsInProportionWithinTheScale() {
        assertEquals(-22.5f, formatter.needleRotationDegrees(-25.0), 0.01f)
    }

    @Test
    fun needleStopsAtTheEndOfTheScaleButTheLabelShowsTheRealValue() {
        assertEquals(45f, formatter.needleRotationDegrees(73.0), 0.01f)
        assertEquals("+73¢", formatter.centsLabel(73.0))
    }
}
