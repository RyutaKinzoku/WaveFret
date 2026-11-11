package com.example.wavefret.common.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BeatClockTest {

    @Test
    fun beatDurationAtSixtyBpmIsOneSecond() {
        assertEquals(1000.0, BeatClock(tempoBpm = 60, startTimeMillis = 0).beatDurationMillis, 1e-9)
    }

    @Test
    fun beatPositionAtStartIsZero() {
        assertEquals(0.0, BeatClock(tempoBpm = 120, startTimeMillis = 5_000).beatPositionAt(5_000), 1e-9)
    }

    @Test
    fun beatPositionCountsFractionalBeatsSinceStart() {
        val beatClock = BeatClock(tempoBpm = 120, startTimeMillis = 1_000)

        assertEquals(2.5, beatClock.beatPositionAt(2_250), 1e-9)
    }

    @Test
    fun beatPositionBeforeStartIsNegative() {
        val beatClock = BeatClock(tempoBpm = 60, startTimeMillis = 1_000)

        assertEquals(-0.5, beatClock.beatPositionAt(500), 1e-9)
    }

    @Test
    fun timeOfBeatIsTheInverseOfBeatPosition() {
        val beatClock = BeatClock(tempoBpm = 90, startTimeMillis = 200)

        assertEquals(2_867L, beatClock.timeOfBeatMillis(4))
    }

    @Test
    fun nonPositiveTempoIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { BeatClock(tempoBpm = 0, startTimeMillis = 0) }
    }
}
