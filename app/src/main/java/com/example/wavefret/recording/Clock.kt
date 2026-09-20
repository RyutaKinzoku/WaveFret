package com.example.wavefret.recording

/**
 * Abstraction over the current time, so time-dependent logic (like file
 * naming) can be unit tested deterministically instead of depending on
 * the real system clock.
 */
interface Clock {
    /** @return Current time in epoch milliseconds. Type: Long */
    fun currentTimeMillis(): Long
}