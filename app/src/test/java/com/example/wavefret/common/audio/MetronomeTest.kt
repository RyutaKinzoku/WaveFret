package com.example.wavefret.common.audio

import com.example.wavefret.common.time.BeatClock
import com.example.wavefret.common.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeTest {

    /** Records when each click happened, and whether it was accented. */
    private class RecordingClickPlayer(private val clock: Clock) : MetronomeClickPlayer {
        val clicks = mutableListOf<Pair<Long, Boolean>>()
        override fun playClick(isAccented: Boolean) {
            clicks += clock.currentTimeMillis() to isAccented
        }
    }

    private fun TestScope.virtualClock(): Clock = object : Clock {
        override fun currentTimeMillis(): Long = currentTime
    }

    @Test
    fun clicksOncePerBeatOnTheBeatClock() = runTest {
        val clock = virtualClock()
        val clickPlayer = RecordingClickPlayer(clock)
        val metronome = Metronome(clickPlayer, clock, beatsPerBar = 4)

        metronome.play(BeatClock(tempoBpm = 120, startTimeMillis = 1_000), beatCount = 3)

        assertEquals(listOf(1_000L, 1_500L, 2_000L), clickPlayer.clicks.map { it.first })
    }

    @Test
    fun accentsTheFirstBeatOfEveryBar() = runTest {
        val clock = virtualClock()
        val clickPlayer = RecordingClickPlayer(clock)
        val metronome = Metronome(clickPlayer, clock, beatsPerBar = 2)

        metronome.play(BeatClock(tempoBpm = 60, startTimeMillis = 0), beatCount = 4)

        assertEquals(listOf(true, false, true, false), clickPlayer.clicks.map { it.second })
    }

    @Test
    fun beatsAlreadyInThePastAreSkipped() = runTest {
        advanceTimeBy(1_200)
        val clock = virtualClock()
        val clickPlayer = RecordingClickPlayer(clock)
        val metronome = Metronome(clickPlayer, clock, beatsPerBar = 4)

        metronome.play(BeatClock(tempoBpm = 60, startTimeMillis = 0), beatCount = 3)

        assertEquals(listOf(2_000L), clickPlayer.clicks.map { it.first })
    }

    @Test
    fun cancellingStopsFurtherClicks() = runTest {
        val clock = virtualClock()
        val clickPlayer = RecordingClickPlayer(clock)
        val metronome = Metronome(clickPlayer, clock, beatsPerBar = 4)

        val metronomeJob = launch { metronome.play(BeatClock(60, startTimeMillis = 0), beatCount = 10) }
        advanceTimeBy(1_500)
        runCurrent()
        metronomeJob.cancel()
        advanceTimeBy(10_000)

        assertEquals(2, clickPlayer.clicks.size)
    }
}
