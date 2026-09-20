package com.example.wavefret.common.time

/** Real Clock implementation backed by System.currentTimeMillis(). */
class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}