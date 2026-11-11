package com.example.wavefret.common.transcription

import kotlin.math.roundToInt

/**
 * Estimates a lesson tempo from note start times: the median gap between
 * notes is taken as one beat and folded by octaves into 60-160 BPM, so
 * eighth-note or half-note lines still give a sensible practice tempo.
 */
class TempoEstimator {

    /**
     * @param onsetsMillis Note start times, in order. Type: List<Long>
     * @return Estimated tempo in beats per minute. Type: Int
     */
    fun estimateBpm(onsetsMillis: List<Long>): Int {
        val gapsMillis = onsetsMillis.zipWithNext { earlier, later -> later - earlier }.filter { it > 0 }.sorted()
        if (gapsMillis.isEmpty()) return DEFAULT_TEMPO_BPM
        var bpm = MILLIS_PER_MINUTE / gapsMillis[gapsMillis.size / 2]
        while (bpm > MAXIMUM_TEMPO_BPM) bpm /= 2
        while (bpm < MINIMUM_TEMPO_BPM) bpm *= 2
        return bpm.roundToInt()
    }

    companion object {
        /** Tempo used when there are too few notes to tell. Type: Int */
        const val DEFAULT_TEMPO_BPM = 80

        private const val MILLIS_PER_MINUTE = 60_000.0
        private const val MINIMUM_TEMPO_BPM = 60.0
        private const val MAXIMUM_TEMPO_BPM = 160.0
    }
}
