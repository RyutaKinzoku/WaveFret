package com.example.wavefret.common.time

import kotlin.math.roundToLong

/**
 * Converts between wall time and musical beats at a fixed tempo, so the
 * metronome, scoring windows and scrolling display all share one timeline.
 *
 * @property tempoBpm Tempo in beats per minute. Type: Int
 * @property startTimeMillis Monotonic time of beat zero, in milliseconds. Type: Long
 */
class BeatClock(
    private val tempoBpm: Int,
    private val startTimeMillis: Long
) {
    init {
        require(tempoBpm > 0) { "Tempo must be positive, was $tempoBpm" }
    }

    /** Length of one beat, in milliseconds. Type: Double */
    val beatDurationMillis: Double = MILLIS_PER_MINUTE / tempoBpm

    /**
     * @param timestampMillis Monotonic time to convert. Type: Long
     * @return Fractional beats elapsed since beat zero; negative before it. Type: Double
     */
    fun beatPositionAt(timestampMillis: Long): Double {
        return (timestampMillis - startTimeMillis) / beatDurationMillis
    }

    /**
     * @param beatIndex Beat to locate, zero being the start. Type: Int
     * @return Monotonic time the beat falls on, rounded to the millisecond. Type: Long
     */
    fun timeOfBeatMillis(beatIndex: Int): Long {
        return startTimeMillis + (beatIndex * beatDurationMillis).roundToLong()
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000.0
    }
}
