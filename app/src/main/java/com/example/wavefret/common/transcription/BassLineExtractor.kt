package com.example.wavefret.common.transcription

import com.example.wavefret.common.music.NoteEvent

/**
 * Picks the bass line out of a polyphonic transcription of a full song,
 * where guitar, keys and voice notes are mixed in with the bass:
 *
 * 1. Of notes starting together, only the lowest is kept (the bass under a chord).
 * 2. A note that is a harmonic of a note still ringing (an octave, an
 *    octave and a fifth, two octaves…) is a ghost of that note and dropped.
 * 3. A note more than an octave above the surrounding bass notes belongs
 *    to another instrument and is dropped.
 *
 * Notes keep their timings; the bass line is monophonic, so overlapping
 * durations from the model don't hide the next bass note.
 *
 * @property simultaneousOnsetMillis Onsets closer than this count as starting together. Type: Long
 * @property registerNeighbourCount Notes on each side used to judge the bass register. Type: Int
 * @property highestSemitonesAboveRegister Notes further above the register's median are dropped. Type: Int
 */
class BassLineExtractor(
    private val simultaneousOnsetMillis: Long = DEFAULT_SIMULTANEOUS_ONSET_MILLIS,
    private val registerNeighbourCount: Int = DEFAULT_REGISTER_NEIGHBOUR_COUNT,
    private val highestSemitonesAboveRegister: Int = DEFAULT_HIGHEST_SEMITONES_ABOVE_REGISTER
) {

    /**
     * @param notes Polyphonic notes in any order. Type: List<NoteEvent>
     * @return The bass line, in time order. Type: List<NoteEvent>
     */
    fun extract(notes: List<NoteEvent>): List<NoteEvent> {
        val lowestOfEachOnset = lowestOfSimultaneousNotes(notes.sortedBy { it.onsetMillis })
        val withoutGhosts = lowestOfEachOnset.filterNot { candidate -> isGhostOfARingingNote(candidate, lowestOfEachOnset) }
        return withoutGhosts.filterIndexed { index, candidate -> isInBassRegister(candidate, index, withoutGhosts) }
    }

    /**
     * @param sortedNotes Notes sorted by onset. Type: List<NoteEvent>
     * @return The lowest note of each group of notes starting together. Type: List<NoteEvent>
     */
    private fun lowestOfSimultaneousNotes(sortedNotes: List<NoteEvent>): List<NoteEvent> {
        val lowestNotes = mutableListOf<NoteEvent>()
        var groupStart = 0
        while (groupStart < sortedNotes.size) {
            var groupEnd = groupStart
            while (groupEnd + 1 < sortedNotes.size &&
                sortedNotes[groupEnd + 1].onsetMillis - sortedNotes[groupStart].onsetMillis <= simultaneousOnsetMillis
            ) groupEnd++
            lowestNotes += sortedNotes.subList(groupStart, groupEnd + 1).minBy { it.midiNumber }
            groupStart = groupEnd + 1
        }
        return lowestNotes
    }

    /**
     * @param candidate Note to check. Type: NoteEvent
     * @param candidates All bass candidates. Type: List<NoteEvent>
     * @return True if a lower candidate is still ringing and the candidate sits on one of its harmonics. Type: Boolean
     */
    private fun isGhostOfARingingNote(candidate: NoteEvent, candidates: List<NoteEvent>): Boolean {
        return candidates.any { ringingNote ->
            ringingNote.onsetMillis < candidate.onsetMillis &&
                ringingNote.onsetMillis + ringingNote.durationMillis > candidate.onsetMillis &&
                candidate.midiNumber - ringingNote.midiNumber in HARMONIC_INTERVALS_SEMITONES
        }
    }

    /**
     * @param candidate Note to check. Type: NoteEvent
     * @param index Its position among the candidates. Type: Int
     * @param candidates All bass candidates, in time order. Type: List<NoteEvent>
     * @return True unless the note is far above the median of its neighbours. Type: Boolean
     */
    private fun isInBassRegister(candidate: NoteEvent, index: Int, candidates: List<NoteEvent>): Boolean {
        val neighbourhood = candidates
            .subList(maxOf(0, index - registerNeighbourCount), minOf(candidates.size, index + registerNeighbourCount + 1))
            .map { it.midiNumber }
            .sorted()
        val registerMedian = neighbourhood[neighbourhood.size / 2]
        return candidate.midiNumber - registerMedian <= highestSemitonesAboveRegister
    }

    private companion object {
        const val DEFAULT_SIMULTANEOUS_ONSET_MILLIS = 60L
        const val DEFAULT_REGISTER_NEIGHBOUR_COUNT = 4
        const val DEFAULT_HIGHEST_SEMITONES_ABOVE_REGISTER = 12

        /** Semitones from a note to its 2nd to 6th harmonics. */
        val HARMONIC_INTERVALS_SEMITONES = setOf(12, 19, 24, 28, 31)
    }
}
