package com.example.wavefret.common.audio

/**
 * Recognises the attack of a plucked string: a sudden rise in loudness
 * well above the level of the reading just before. A ringing string
 * only fades, and fretting a still-ringing string barely changes its level,
 * so neither counts as a new pluck.
 *
 * @property riseRatio How many times louder than the previous reading an attack is. Type: Double
 * @property minimumRms Quieter readings are never attacks (room noise). Type: Double
 * @property memoryReadings How many previous readings are compared against. Type: Int
 */
class AttackDetector(
    private val riseRatio: Double = DEFAULT_RISE_RATIO,
    private val minimumRms: Double = DEFAULT_MINIMUM_RMS,
    private val memoryReadings: Int = DEFAULT_MEMORY_READINGS
) {

    private val recentLevels = ArrayDeque<Double>()

    /**
     * @param rms Loudness of the latest audio window. Type: Double
     * @return True if this window starts a new pluck. Type: Boolean
     */
    fun onLevel(rms: Double): Boolean {
        val recentQuietestLevel = recentLevels.minOrNull() ?: 0.0
        recentLevels.addLast(rms)
        if (recentLevels.size > memoryReadings) recentLevels.removeFirst()
        return rms >= minimumRms && rms >= recentQuietestLevel * riseRatio
    }

    private companion object {
        const val DEFAULT_RISE_RATIO = 1.8
        const val DEFAULT_MINIMUM_RMS = 0.003
        /** Only the previous reading: older ones may still hold the silence before the pluck. */
        const val DEFAULT_MEMORY_READINGS = 1
    }
}
