package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote

/**
 * Turns timed notes from any source (MIDI, Guitar Pro, audio transcription)
 * into a playable bass lesson: notes starting together are reduced to the
 * lowest one (the bass line), notes outside the instrument's range are
 * moved by octaves until they fit, and each note gets a fret position near
 * the previous one.
 *
 * @property tuning Tuning the lesson is played in. Type: BassTuning
 * @property fretboardPositionFinder Chooses string and fret for each pitch. Type: FretboardPositionFinder
 * @property chordWindowMillis Notes starting within this time of each other count as one chord. Type: Long
 */
class NoteEventsLessonBuilder(
    private val tuning: BassTuning,
    private val fretboardPositionFinder: FretboardPositionFinder,
    private val chordWindowMillis: Long = DEFAULT_CHORD_WINDOW_MILLIS
) {

    /**
     * @param title Lesson title. Type: String
     * @param tempoBpm Tempo for tempo mode. Type: Int
     * @param noteEvents Timed notes, in any order. Type: List<NoteEvent>
     * @return The lesson, with each note's original start time. Type: Exercise
     * @throws IllegalArgumentException If there are no notes.
     */
    fun build(title: String, tempoBpm: Int, noteEvents: List<NoteEvent>): Exercise {
        require(noteEvents.isNotEmpty()) { "No notes were found" }
        val bassLine = lowestNotePerChord(noteEvents.sortedBy { it.onsetMillis })
        val playableRange = playableMidiRange()
        val notes = mutableListOf<TabNote>()
        for (noteEvent in bassLine) {
            notes += fretboardPositionFinder.positionFor(foldIntoRange(noteEvent.midiNumber, playableRange), notes.lastOrNull())
        }
        return Exercise(
            title = title,
            tuning = tuning,
            tempoBpm = tempoBpm,
            notes = notes,
            noteOnsetsMillis = bassLine.map { it.onsetMillis }
        )
    }

    /**
     * @param sortedEvents Notes sorted by onset. Type: List<NoteEvent>
     * @return One note per chord: the lowest. Type: List<NoteEvent>
     */
    private fun lowestNotePerChord(sortedEvents: List<NoteEvent>): List<NoteEvent> {
        val chords = mutableListOf<MutableList<NoteEvent>>()
        for (noteEvent in sortedEvents) {
            val currentChord = chords.lastOrNull()
            if (currentChord != null && noteEvent.onsetMillis - currentChord.first().onsetMillis <= chordWindowMillis) {
                currentChord += noteEvent
            } else {
                chords += mutableListOf(noteEvent)
            }
        }
        return chords.map { chord -> chord.minBy { it.midiNumber }.copy(onsetMillis = chord.first().onsetMillis) }
    }

    /** @return Lowest and highest MIDI numbers the tuning can play. Type: IntRange */
    private fun playableMidiRange(): IntRange {
        val openStringMidiNumbers = fretboardPositionFinder.openStringMidiNumbers()
        return openStringMidiNumbers.min()..(openStringMidiNumbers.max() + fretboardPositionFinder.highestFret)
    }

    /**
     * @param midiNumber Pitch to fit. Type: Int
     * @param playableRange Pitches the bass can play. Type: IntRange
     * @return The same note name moved by whole octaves into range. Type: Int
     */
    private fun foldIntoRange(midiNumber: Int, playableRange: IntRange): Int {
        var foldedMidiNumber = midiNumber
        while (foldedMidiNumber < playableRange.first) foldedMidiNumber += SEMITONES_PER_OCTAVE
        while (foldedMidiNumber > playableRange.last) foldedMidiNumber -= SEMITONES_PER_OCTAVE
        return foldedMidiNumber
    }

    private companion object {
        const val DEFAULT_CHORD_WINDOW_MILLIS = 30L
        const val SEMITONES_PER_OCTAVE = 12
    }
}
