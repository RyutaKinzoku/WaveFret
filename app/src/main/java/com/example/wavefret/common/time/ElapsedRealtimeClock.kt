package com.example.wavefret.common.time

import android.os.SystemClock as AndroidSystemClock

/**
 * Monotonic [Clock] backed by [AndroidSystemClock.elapsedRealtime], for
 * measuring intervals (tempo, timing windows) that must not jump when the
 * wall clock is changed.
 *
 * Thin framework wrapper with no logic, so per this project's convention it
 * is verified on-device rather than in JUnit.
 */
class ElapsedRealtimeClock : Clock {

    /** @return Milliseconds since boot, including deep sleep. Type: Long */
    override fun currentTimeMillis(): Long = AndroidSystemClock.elapsedRealtime()
}
