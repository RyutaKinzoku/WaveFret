package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedleSmootherTest {

    @Test
    fun theFirstReadingIsShownAsIs() {
        assertEquals(12.0, NeedleSmoother().onReading(12.0, 0)!!, 1e-9)
    }

    @Test
    fun smallWobblesAreSmoothedButTheNeedleStillMovesQuickly() {
        val smoother = NeedleSmoother(timeConstantMillis = 100)
        smoother.onReading(0.0, 0)

        val afterOneReading = smoother.onReading(10.0, 25)!!
        var position = afterOneReading
        for (time in 50L..400L step 25L) position = smoother.onReading(10.0, time)!!

        assertTrue("moved $afterOneReading", afterOneReading in 1.0..4.0)
        assertEquals(10.0, position, 0.5)
    }

    @Test
    fun aBigJumpMovesStraightThere() {
        val smoother = NeedleSmoother(jumpResetCents = 60.0)
        smoother.onReading(0.0, 0)

        assertEquals(-80.0, smoother.onReading(-80.0, 25)!!, 1e-9)
    }

    @Test
    fun aMomentOfSilenceKeepsTheNeedleWhereItWas() {
        val smoother = NeedleSmoother(holdMillis = 500)
        smoother.onReading(7.0, 0)

        assertEquals(7.0, smoother.onReading(null, 200)!!, 1e-9)
    }

    @Test
    fun longSilenceRestsTheNeedle() {
        val smoother = NeedleSmoother(holdMillis = 500)
        smoother.onReading(7.0, 0)

        assertNull(smoother.onReading(null, 700))
    }

    @Test
    fun resetForgetsThePreviousString() {
        val smoother = NeedleSmoother()
        smoother.onReading(30.0, 0)

        smoother.reset()

        assertEquals(-5.0, smoother.onReading(-5.0, 25)!!, 1e-9)
    }
}
