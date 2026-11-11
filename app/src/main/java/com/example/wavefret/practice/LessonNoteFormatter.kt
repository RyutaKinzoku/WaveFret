package com.example.wavefret.practice

/** Formats the parts of a note row in the lesson editor. */
class LessonNoteFormatter {

    /**
     * @param onsetMillis When the note starts, or null for untimed lessons. Type: Long?
     * @return The time as minutes:seconds.tenths, e.g. "0:03.4", or a dash. Type: String
     */
    fun formatTime(onsetMillis: Long?): String {
        if (onsetMillis == null) return NO_TIME
        val totalTenths = onsetMillis / MILLIS_PER_TENTH
        val minutes = totalTenths / TENTHS_PER_MINUTE
        val seconds = totalTenths % TENTHS_PER_MINUTE / TENTHS_PER_SECOND
        val tenths = totalTenths % TENTHS_PER_SECOND
        return "$minutes:${seconds.toString().padStart(2, '0')}.$tenths"
    }

    /**
     * @param noteIndex Zero-based note index. Type: Int
     * @return The one-based number shown to the player. Type: String
     */
    fun formatNumber(noteIndex: Int): String = (noteIndex + 1).toString()

    private companion object {
        const val NO_TIME = "–"
        const val MILLIS_PER_TENTH = 100L
        const val TENTHS_PER_SECOND = 10L
        const val TENTHS_PER_MINUTE = 600L
    }
}
