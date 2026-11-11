package com.example.wavefret.common.transcription

import com.example.wavefret.common.audio.SyntheticSignals
import com.example.wavefret.common.audio.YinPitchDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
}
