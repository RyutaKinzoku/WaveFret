package com.example.wavefret.common.time

/**
 * Clock that advances by a fixed step every time it is read, so tests can
 * reason about a sequence of evenly spaced timestamps.
 *
 * @param firstTimeMillis Time returned by the first read. Type: Long
 * @param stepMillis How far time advances between reads. Type: Long
 */
class SteppingFakeClock(firstTimeMillis: Long, private val stepMillis: Long) : Clock {

    private var nextTimeMillis = firstTimeMillis

    /** @return The next time in the sequence. Type: Long */
    override fun currentTimeMillis(): Long = nextTimeMillis.also { nextTimeMillis += stepMillis }
}
