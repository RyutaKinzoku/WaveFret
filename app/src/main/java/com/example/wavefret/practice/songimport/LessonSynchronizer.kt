package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.LessonMediaSource

/**
 * Gives a lesson whose notes are right (from a tab or sheet music) the
 * timing of a recording, so it can be played along with: the lesson's
 * notes are lined up with the notes transcribed from the song, in order,
 * matching equal pitches first and octave mistakes second, and skipping
 * notes of other instruments. Notes that weren't heard get a time between
 * their neighbours.
 *
 * @property fretboardPositionFinder Gives the pitch of each tab note. Type: FretboardPositionFinder
 */
class LessonSynchronizer(private val fretboardPositionFinder: FretboardPositionFinder) {

    /**
     * @param lesson Lesson with the right notes. Type: Exercise
     * @param heardNotes Notes transcribed from the song, with their times. Type: List<NoteEvent>
     * @param mediaSource The song to play along with. Type: LessonMediaSource
     * @return The lesson with a time for every note and the song attached. Type: Exercise
     * @throws IllegalArgumentException If too few notes of the lesson were heard in the song.
     */
    fun synchronize(lesson: Exercise, heardNotes: List<NoteEvent>, mediaSource: LessonMediaSource): Exercise {
        require(heardNotes.isNotEmpty()) { NOTHING_HEARD_REASON }
        val openStrings = fretboardPositionFinder.openStringMidiNumbers()
        val lessonPitches = lesson.notes.map { openStrings[it.stringIndex] + it.fret }
        val heard = heardNotes.sortedBy { it.onsetMillis }
        val matches = bestMatches(lessonPitches, heard.map { it.midiNumber })
        require(matches.size >= maxOf(MINIMUM_MATCHES, lessonPitches.size / MATCH_SHARE_DIVISOR)) { NOT_ENOUGH_MATCHES_REASON }

        val matchedOnsets = matches.associate { (lessonIndex, heardIndex) -> lessonIndex to heard[heardIndex].onsetMillis }
        return lesson.copy(noteOnsetsMillis = fillMissingOnsets(lessonPitches.size, matchedOnsets), mediaSource = mediaSource)
    }

    /**
     * Weighted longest common subsequence of the two pitch sequences.
     *
     * @param lessonPitches Pitches of the lesson, in order. Type: List<Int>
     * @param heardPitches Pitches heard, in time order. Type: List<Int>
     * @return Matched pairs (lesson index, heard index), in order. Type: List<Pair<Int, Int>>
     */
    private fun bestMatches(lessonPitches: List<Int>, heardPitches: List<Int>): List<Pair<Int, Int>> {
        val rows = lessonPitches.size + 1
        val columns = heardPitches.size + 1
        val scores = Array(rows) { IntArray(columns) }
        for (lessonIndex in 1 until rows) for (heardIndex in 1 until columns) {
            val matchWeight = matchWeight(lessonPitches[lessonIndex - 1], heardPitches[heardIndex - 1])
            scores[lessonIndex][heardIndex] = maxOf(
                scores[lessonIndex - 1][heardIndex],
                scores[lessonIndex][heardIndex - 1],
                if (matchWeight > 0) scores[lessonIndex - 1][heardIndex - 1] + matchWeight else 0
            )
        }
        val matches = mutableListOf<Pair<Int, Int>>()
        var lessonIndex = lessonPitches.size
        var heardIndex = heardPitches.size
        while (lessonIndex > 0 && heardIndex > 0) {
            val matchWeight = matchWeight(lessonPitches[lessonIndex - 1], heardPitches[heardIndex - 1])
            when {
                matchWeight > 0 && scores[lessonIndex][heardIndex] == scores[lessonIndex - 1][heardIndex - 1] + matchWeight -> {
                    matches += (lessonIndex - 1) to (heardIndex - 1)
                    lessonIndex--
                    heardIndex--
                }
                scores[lessonIndex][heardIndex] == scores[lessonIndex - 1][heardIndex] -> lessonIndex--
                else -> heardIndex--
            }
        }
        return matches.reversed()
    }

    /**
     * @param lessonPitch Pitch the lesson expects. Type: Int
     * @param heardPitch Pitch heard. Type: Int
     * @return How well they match: equal pitches best, octaves apart next, otherwise 0. Type: Int
     */
    private fun matchWeight(lessonPitch: Int, heardPitch: Int): Int = when {
        lessonPitch == heardPitch -> EXACT_MATCH_WEIGHT
        Math.floorMod(lessonPitch - heardPitch, SEMITONES_PER_OCTAVE) == 0 -> OCTAVE_MATCH_WEIGHT
        else -> 0
    }

    /**
     * @param noteCount Notes in the lesson. Type: Int
     * @param matchedOnsets Known times by lesson index. Type: Map<Int, Long>
     * @return A non-decreasing time for every note: known ones kept, others spread between neighbours. Type: List<Long>
     */
    private fun fillMissingOnsets(noteCount: Int, matchedOnsets: Map<Int, Long>): List<Long> {
        val knownIndices = matchedOnsets.keys.sorted()
        val typicalGap = knownIndices.zipWithNext()
            .map { (earlier, later) -> (matchedOnsets.getValue(later) - matchedOnsets.getValue(earlier)) / (later - earlier) }
            .sorted()
            .let { gaps -> if (gaps.isEmpty()) DEFAULT_GAP_MILLIS else gaps[gaps.size / 2] }
        val onsets = List(noteCount) { index ->
            matchedOnsets[index] ?: run {
                val previous = knownIndices.lastOrNull { it < index }
                val next = knownIndices.firstOrNull { it > index }
                when {
                    previous != null && next != null -> {
                        val previousOnset = matchedOnsets.getValue(previous)
                        previousOnset + (matchedOnsets.getValue(next) - previousOnset) * (index - previous) / (next - previous)
                    }
                    previous != null -> matchedOnsets.getValue(previous) + typicalGap * (index - previous)
                    else -> (matchedOnsets.getValue(next!!) - typicalGap * (next - index)).coerceAtLeast(0)
                }
            }
        }
        return onsets.runningReduce { earlier, later -> maxOf(earlier, later) }
    }

    companion object {
        /** Why a song with no notes can't be used. Type: String */
        const val NOTHING_HEARD_REASON = "No notes were heard in the song"

        /** Why a song that doesn't match the lesson can't be used. Type: String */
        const val NOT_ENOUGH_MATCHES_REASON = "The song's notes don't match this lesson"

        private const val EXACT_MATCH_WEIGHT = 3
        private const val OCTAVE_MATCH_WEIGHT = 2
        private const val SEMITONES_PER_OCTAVE = 12
        private const val MINIMUM_MATCHES = 2
        private const val MATCH_SHARE_DIVISOR = 3
        private const val DEFAULT_GAP_MILLIS = 500L
    }
}
