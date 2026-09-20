package com.example.wavefret.recording

/** Real Clock implementation backed by System.currentTimeMillis(). */
class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}