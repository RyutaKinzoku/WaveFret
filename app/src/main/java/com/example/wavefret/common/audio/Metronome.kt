package com.example.wavefret.common.audio

import com.example.wavefret.common.time.BeatClock
import com.example.wavefret.common.time.Clock
import kotlinx.coroutines.delay

/**
 * Clicks on every beat of a [BeatClock], accenting the first beat of each
 * bar. Each click is scheduled against the clock rather than by chaining
 * fixed delays, so timing drift never accumulates.
 *
 * @property clickPlayer Produces the audible click. Type: MetronomeClickPlayer
 * @property clock Time source shared with the BeatClock. Type: Clock
 * @property beatsPerBar Beats between accented clicks. Type: Int
 */
class Metronome(
    private val clickPlayer: MetronomeClickPlayer,
    private val clock: Clock,
    private val beatsPerBar: Int = DEFAULT_BEATS_PER_BAR
) {

    /**
     * Suspends while clicking beats 0 until beatCount; cancel the calling
     * coroutine to stop early. Beats already in the past are skipped.
     *
     * @param beatClock Timeline to click along. Type: BeatClock
     * @param beatCount How many beats to click. Type: Int
     * @return Unit
     */
    suspend fun play(beatClock: BeatClock, beatCount: Int) {
        for (beatIndex in 0 until beatCount) {
            val millisUntilBeat = beatClock.timeOfBeatMillis(beatIndex) - clock.currentTimeMillis()
            if (millisUntilBeat < 0) continue
            delay(millisUntilBeat)
            clickPlayer.playClick(isAccented = beatIndex % beatsPerBar == 0)
        }
    }

    private companion object {
        const val DEFAULT_BEATS_PER_BAR = 4
    }
}
