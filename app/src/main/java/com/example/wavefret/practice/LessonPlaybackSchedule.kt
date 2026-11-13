package com.example.wavefret.practice

/**
 * Decides when each note of a lesson sounds when it is played back to be
 * heard. Lessons taken from a song keep the song's rhythm, starting at
 * the first note, with long silences shortened; other lessons play one
 * note per beat at their tempo. Notes are played one after another, so
 * notes that started together in the song are still each heard briefly.
 */
class LessonPlaybackSchedule {

    /**
     * @param lesson Lesson to play back. Type: Exercise
     * @return Its notes in order, each with its start and length. Type: List<ScheduledNote>
     */
    fun schedule(lesson: Exercise): List<ScheduledNote> {
        val beatMillis = MILLIS_PER_MINUTE / lesson.tempoBpm
        val onsetsMillis = lesson.noteOnsetsMillis
        var startMillis = 0L
        return lesson.notes.mapIndexed { index, note ->
            val durationMillis = if (onsetsMillis == null || index == lesson.notes.lastIndex) {
                beatMillis
            } else {
                (onsetsMillis[index + 1] - onsetsMillis[index]).coerceIn(SHORTEST_NOTE_MILLIS, LONGEST_GAP_MILLIS)
            }
            ScheduledNote(index, note.expectedFrequencyHz(lesson.tuning), startMillis, durationMillis)
                .also { startMillis += durationMillis }
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val SHORTEST_NOTE_MILLIS = 80L
        const val LONGEST_GAP_MILLIS = 2_000L
    }
}
