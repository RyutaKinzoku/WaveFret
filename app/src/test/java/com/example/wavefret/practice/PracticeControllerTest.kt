package com.example.wavefret.practice

import com.example.wavefret.common.audio.FakePcmAudioSource
import com.example.wavefret.common.audio.FakePitchDetector
import com.example.wavefret.common.audio.AdaptiveSilenceGate
import com.example.wavefret.common.audio.SilenceGate
import com.example.wavefret.common.audio.TargetedPitchDetector
import com.example.wavefret.common.audio.SyntheticSignals
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.time.SteppingFakeClock
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeControllerTest {

    private val openAHz = 55.0
    private val twoOpenAsAtSixtyBpm = Exercise(
        title = "Two As",
        tuning = BassTunings.STANDARD,
        tempoBpm = 60,
        notes = listOf(TabNote(1, 0), TabNote(1, 0))
    )
    private val audibleBuffer = SyntheticSignals.sine(openAHz, SAMPLE_RATE, SAMPLE_COUNT)
    private val silentBuffer = SyntheticSignals.silence(SAMPLE_COUNT)

    private fun controllerFor(buffers: List<FloatArray>, scriptedFrequenciesHz: List<Double?>) = PracticeController(
        audioSource = FakePcmAudioSource(buffers, SAMPLE_RATE),
        signalGate = SilenceGate(),
        pitchDetector = FakePitchDetector(scriptedFrequenciesHz),
        clock = SteppingFakeClock(firstTimeMillis = 0, stepMillis = 1_000)
    )

    private fun waitSession() = WaitModePracticeSession(twoOpenAsAtSixtyBpm, NoteMatcher(), HitConfirmer(1))

    @Test
    fun emitsOneProgressPerBuffer() = runTest {
        val practiceController = controllerFor(listOf(silentBuffer, silentBuffer), emptyList())

        val emittedProgress = practiceController.progressStream(waitSession()).toList()

        assertEquals(2, emittedProgress.size)
    }

    @Test
    fun silentBufferIsReportedAsSilentWithoutRunningPitchDetection() = runTest {
        val practiceController = controllerFor(listOf(silentBuffer, audibleBuffer), listOf(openAHz))

        val emittedProgress = practiceController.progressStream(waitSession()).toList()

        assertEquals(PitchReading.SILENT, emittedProgress[0].lastReading)
        assertEquals(1, emittedProgress[1].hitCount)
    }

    @Test
    fun undetectablePitchIsReportedAsSilent() = runTest {
        val practiceController = controllerFor(listOf(audibleBuffer), listOf(null))

        val emittedProgress = practiceController.progressStream(waitSession()).toList().single()

        assertEquals(PitchReading.SILENT, emittedProgress.lastReading)
    }

    @Test
    fun repluckedRepeatedNoteCompletesTheExercise() = runTest {
        val practiceController = controllerFor(
            listOf(audibleBuffer, silentBuffer, audibleBuffer),
            listOf(openAHz, openAHz)
        )

        val finalProgress = practiceController.progressStream(waitSession()).toList().last()

        assertEquals(true, finalProgress.isComplete)
    }

    @Test
    fun readingsAreTimestampedFromTheClock() = runTest {
        val tempoSession = TempoModePracticeSession(
            twoOpenAsAtSixtyBpm, NoteMatcher(), HitConfirmer(1), countInBeats = 1
        )
        tempoSession.start(startTimeMillis = 0)
        // Buffers arrive at t = 0 s, 1 s, 2 s, 3 s: the first A (due 1 s) is hit,
        // the second A (due 2 s) is never re-plucked, so it is missed by t = 3 s.
        val practiceController = controllerFor(
            listOf(silentBuffer, audibleBuffer, audibleBuffer, silentBuffer),
            listOf(openAHz, openAHz)
        )

        val finalProgress = practiceController.progressStream(tempoSession).toList().last()

        assertEquals(1, finalProgress.hitCount)
        assertEquals(1, finalProgress.missCount)
    }

    @Test
    fun aSoftExpectedNoteMissedByGeneralDetectionIsHeardByTheTargetedDetector() = runTest {
        val tone = SyntheticSignals.bassLikeTone(openAHz, SAMPLE_RATE, SAMPLE_COUNT)
        val noise = SyntheticSignals.whiteNoise(SAMPLE_COUNT, amplitude = 0.012, seed = 3L)
        val softNoisyA = FloatArray(SAMPLE_COUNT) { tone[it] * 0.02f + noise[it] }
        val practiceController = PracticeController(
            audioSource = FakePcmAudioSource(listOf(softNoisyA), SAMPLE_RATE),
            signalGate = AdaptiveSilenceGate(),
            pitchDetector = FakePitchDetector(listOf(null)),
            clock = SteppingFakeClock(firstTimeMillis = 0, stepMillis = 1_000),
            targetedPitchDetector = TargetedPitchDetector()
        )

        val progress = practiceController.progressStream(waitSession()).toList().single()

        assertEquals(PitchReading.MATCHING, progress.lastReading)
    }

    @Test
    fun aWrongNoteIsStillReportedThroughGeneralDetection() = runTest {
        val dString = SyntheticSignals.bassLikeTone(73.42, SAMPLE_RATE, SAMPLE_COUNT)
        val practiceController = PracticeController(
            audioSource = FakePcmAudioSource(listOf(dString), SAMPLE_RATE),
            signalGate = SilenceGate(),
            pitchDetector = FakePitchDetector(listOf(73.42)),
            clock = SteppingFakeClock(firstTimeMillis = 0, stepMillis = 1_000),
            targetedPitchDetector = TargetedPitchDetector()
        )

        val progress = practiceController.progressStream(waitSession()).toList().single()

        assertEquals(PitchReading.NOT_MATCHING, progress.lastReading)
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val SAMPLE_COUNT = 4096
    }
}
