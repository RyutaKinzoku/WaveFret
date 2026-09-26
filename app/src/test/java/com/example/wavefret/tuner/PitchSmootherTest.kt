package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class PitchSmootherTest {

    @Test
    fun addReadingWithASingleValueReturnsThatValue() {
        val smoother = PitchSmoother(windowSize = 5)
        assertEquals(110.0, smoother.addReading(110.0), 0.0001)
    }

    @Test
    fun addReadingAveragesTheTwoMiddleValuesWhenReadingCountIsEven() {
        val smoother = PitchSmoother(windowSize = 5)
        smoother.addReading(100.0)
        val median = smoother.addReading(200.0)
        assertEquals(150.0, median, 0.0001)
    }

    @Test
    fun addReadingOnlyConsidersTheMostRecentWindowSizeReadings() {
        val smoother = PitchSmoother(windowSize = 3)
        smoother.addReading(10.0)
        smoother.addReading(10.0)
        smoother.addReading(10.0)
        // Window is now full at [10, 10, 10]; this reading pushes out the
        // oldest 10, leaving [10, 10, 1000].
        val median = smoother.addReading(1000.0)
        assertEquals(10.0, median, 0.0001)
    }

    @Test
    fun addReadingSuppressesASingleOctaveJumpOutlier() {
        val smoother = PitchSmoother(windowSize = 5)
        smoother.addReading(110.0)
        smoother.addReading(110.0)
        smoother.addReading(220.0) // a single stray octave-up misread
        smoother.addReading(110.0)
        val median = smoother.addReading(110.0)
        assertEquals(110.0, median, 0.0001)
    }
}