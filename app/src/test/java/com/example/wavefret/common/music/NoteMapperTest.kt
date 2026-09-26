package com.example.wavefret.common.music

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class NoteMapperTest {

    private val noteMapper = NoteMapper()

    private fun frequencyForSemitonesFromA4(semitones: Double): Double {
        return 440.0 * 2.0.pow(semitones / 12.0)
    }

    @Test
    fun mapsReferenceAToA4WithZeroCents() {
        val mapped = noteMapper.mapFrequencyToNote(440.0)
        assertEquals("A", mapped.noteName)
        assertEquals(4, mapped.octave)
        assertEquals(0.0, mapped.centsOffset, 0.01)
    }

    @Test
    fun mapsLowEStringFrequencyToE1WithZeroCents() {
        // E1 is 41 semitones below A4.
        val frequency = frequencyForSemitonesFromA4(-41.0)
        val mapped = noteMapper.mapFrequencyToNote(frequency)
        assertEquals("E", mapped.noteName)
        assertEquals(1, mapped.octave)
        assertEquals(0.0, mapped.centsOffset, 0.01)
    }

    @Test
    fun mapsGStringFrequencyToG2WithZeroCents() {
        // G2 is 26 semitones below A4.
        val frequency = frequencyForSemitonesFromA4(-26.0)
        val mapped = noteMapper.mapFrequencyToNote(frequency)
        assertEquals("G", mapped.noteName)
        assertEquals(2, mapped.octave)
        assertEquals(0.0, mapped.centsOffset, 0.01)
    }

    @Test
    fun mapsASharpFrequencyToNearestNoteWithPositiveCents() {
        // 30 cents sharp of E1 is still well inside E1's half-semitone zone
        // (up to 50 cents before F1 becomes nearer), so this should
        // resolve to E1, not F1.
        val frequency = frequencyForSemitonesFromA4(-41.0 + 0.30)
        val mapped = noteMapper.mapFrequencyToNote(frequency)
        assertEquals("E", mapped.noteName)
        assertEquals(1, mapped.octave)
        assertEquals(30.0, mapped.centsOffset, 0.01)
    }

    @Test
    fun mapsAFlatFrequencyToNearestNoteWithNegativeCents() {
        val frequency = frequencyForSemitonesFromA4(-41.0 - 0.25)
        val mapped = noteMapper.mapFrequencyToNote(frequency)
        assertEquals("E", mapped.noteName)
        assertEquals(1, mapped.octave)
        assertEquals(-25.0, mapped.centsOffset, 0.01)
    }
}