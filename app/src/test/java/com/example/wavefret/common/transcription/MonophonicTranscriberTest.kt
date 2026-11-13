package com.example.wavefret.common.transcription

import com.example.wavefret.common.audio.SyntheticSignals
import com.example.wavefret.common.audio.YinPitchDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class MonophonicTranscriberTest {

    private val sampleRate = 22_050
    private val transcriber = MonophonicTranscriber(YinPitchDetector())

    /** Concatenates tones and silences: each pair is (frequency or null for silence, milliseconds). */
    private fun sequence(vararg parts: Pair<Double?, Int>): FloatArray {
        return parts.map { (frequencyHz, durationMillis) ->
            val sampleCount = sampleRate * durationMillis / 1_000
            if (frequencyHz == null) SyntheticSignals.silence(sampleCount)
            else SyntheticSignals.sine(frequencyHz, sampleRate, sampleCount).map { it * 0.5f }.toFloatArray()
        }.reduce { all, part -> all + part }
    }

    @Test
    fun aSingleSustainedToneIsOneNote() {
        val notes = transcriber.transcribe(sequence(55.0 to 600), sampleRate)

        assertEquals(listOf(33), notes.map { it.midiNumber })
    }

    @Test
    fun aPitchChangeStartsANewNoteAtTheRightTime() {
        val notes = transcriber.transcribe(sequence(55.0 to 500, 73.42 to 500), sampleRate)

        assertEquals(listOf(33, 38), notes.map { it.midiNumber })
        assertTrue("second onset was ${notes[1].onsetMillis}", abs(notes[1].onsetMillis - 500) <= 80)
    }

    @Test
    fun theSameNotePluckedTwiceAfterASilenceIsTwoNotes() {
        val notes = transcriber.transcribe(sequence(41.2 to 500, null to 150, 41.2 to 500), sampleRate)

        assertEquals(listOf(28, 28), notes.map { it.midiNumber })
    }

    @Test
    fun silenceProducesNoNotes() {
        assertEquals(0, transcriber.transcribe(sequence(null to 1_000), sampleRate).size)
    }

    @Test
    fun veryShortBlipsAreIgnored() {
        val notes = transcriber.transcribe(sequence(null to 300, 98.0 to 30, null to 300), sampleRate)

        assertEquals(0, notes.size)
    }

    /** A plucked bass note: harmonic-rich and decaying, like a real string. */
    private fun pluck(frequencyHz: Double, durationMillis: Int, amplitude: Double = 0.6): FloatArray {
        val sampleCount = sampleRate * durationMillis / 1_000
        val tone = SyntheticSignals.bassLikeTone(frequencyHz, sampleRate, sampleCount)
        return FloatArray(sampleCount) { index ->
            (tone[index] * amplitude * exp(-index / (sampleRate * 0.35))).toFloat()
        }
    }

    @Test
    fun repeatedPlucksOfTheSameNoteWithoutSilenceAreSeparateNotes() {
        val pluckSamples = sampleRate * 300 / 1_000
        val ringingString = SyntheticSignals.bassLikeTone(41.2, sampleRate, pluckSamples * 4)
        val bassLine = FloatArray(ringingString.size) { index ->
            (ringingString[index] * 0.6 * exp(-(index % pluckSamples) / (sampleRate * 0.35))).toFloat()
        }

        val notes = transcriber.transcribe(bassLine, sampleRate)

        assertEquals(listOf(28, 28, 28, 28), notes.map { it.midiNumber })
        notes.zip(listOf(0L, 300L, 600L, 900L)).forEach { (note, expectedOnset) ->
            assertTrue("onset ${note.onsetMillis} expected near $expectedOnset", abs(note.onsetMillis - expectedOnset) <= 60)
        }
    }

    @Test
    fun aQuietRecordingIsStillTranscribed() {
        val quietBassLine = pluck(55.0, 500, amplitude = 0.01) + pluck(73.42, 500, amplitude = 0.01)

        assertEquals(listOf(33, 38), transcriber.transcribe(quietBassLine, sampleRate).map { it.midiNumber })
    }

    @Test
    fun harmonicRichBassNotesAreNotReadAnOctaveHigh() {
        val bassLine = pluck(41.2, 500) + pluck(61.74, 500) + pluck(82.41, 500) + pluck(110.0, 500)

        assertEquals(listOf(28, 35, 40, 45), transcriber.transcribe(bassLine, sampleRate).map { it.midiNumber })
    }

    @Test
    fun quietRoomNoiseBetweenNotesIsNotANote() {
        val noise = SyntheticSignals.whiteNoise(sampleRate / 2, amplitude = 0.01)
        val bassLine = noise + pluck(55.0, 500) + SyntheticSignals.whiteNoise(sampleRate / 2, amplitude = 0.01, seed = 7)

        assertEquals(listOf(33), transcriber.transcribe(bassLine, sampleRate).map { it.midiNumber })
    }
}
