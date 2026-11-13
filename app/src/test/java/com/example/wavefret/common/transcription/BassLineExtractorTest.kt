package com.example.wavefret.common.transcription

import com.example.wavefret.common.music.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class BassLineExtractorTest {

    private val bassLineExtractor = BassLineExtractor()

    private fun note(midiNumber: Int, onsetMillis: Long, durationMillis: Long = 400) =
        NoteEvent(midiNumber, onsetMillis, durationMillis)

    @Test
    fun keepsOnlyTheLowestOfNotesStartingTogether() {
        val chordOverBass = listOf(note(52, 0), note(40, 20), note(59, 40), note(43, 500), note(55, 510))

        assertEquals(listOf(40, 43), bassLineExtractor.extract(chordOverBass).map { it.midiNumber })
    }

    @Test
    fun keepsEveryNoteOfAMelodicBassLineEvenWhenTheyRingIntoEachOther() {
        val legatoBassLine = listOf(note(28, 0, 1_000), note(31, 500, 1_000), note(33, 1_000, 1_000), note(31, 1_500, 600))

        assertEquals(listOf(28, 31, 33, 31), bassLineExtractor.extract(legatoBassLine).map { it.midiNumber })
    }

    @Test
    fun dropsNotesFarAboveTheBassRegister() {
        val bassLineWithAGuitarLick = listOf(
            note(28, 0), note(31, 500), note(33, 1_000), note(57, 1_250, 100), note(35, 1_500), note(28, 2_000)
        )

        assertEquals(listOf(28, 31, 33, 35, 28), bassLineExtractor.extract(bassLineWithAGuitarLick).map { it.midiNumber })
    }

    @Test
    fun dropsAnOctaveOrFifthGhostOfANoteStillRinging() {
        val ringingNoteWithGhosts = listOf(note(28, 0, 1_000), note(40, 300, 200), note(47, 600, 200), note(31, 1_000))

        assertEquals(listOf(28, 31), bassLineExtractor.extract(ringingNoteWithGhosts).map { it.midiNumber })
    }

    @Test
    fun keepsTheExtractedNotesTimingsAndOrder() {
        val notes = listOf(note(33, 900, 300), note(28, 100, 250))

        assertEquals(listOf(note(28, 100, 250), note(33, 900, 300)), bassLineExtractor.extract(notes))
    }

    @Test
    fun noNotesGiveNoBassLine() {
        assertEquals(emptyList<NoteEvent>(), bassLineExtractor.extract(emptyList()))
    }
}
