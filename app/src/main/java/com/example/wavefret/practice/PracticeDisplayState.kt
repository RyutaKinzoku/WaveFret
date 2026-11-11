package com.example.wavefret.practice

/**
 * Everything the practice screen shows, independent of Android Views, so
 * the formatting decisions can be tested in JUnit.
 *
 * @property currentStringName String of the note to play now, or null once complete. Type: String?
 * @property currentFret Fret of the note to play now, or null once complete. Type: Int?
 * @property currentNoteNumber One-based number of the current note, capped at noteCount. Type: Int
 * @property noteCount Total notes in the exercise. Type: Int
 * @property feedback What to tell the player about the last reading. Type: PracticeFeedback
 * @property scorePercent Percentage of notes hit, shown only once complete. Type: Int?
 * @property noteStatuses How to draw each note of the tab, by index. Type: List<TabNoteStatus>
 * @property playheadPosition Where the staff is scrolled to, in note positions. Type: Double
 */
data class PracticeDisplayState(
    val currentStringName: String?,
    val currentFret: Int?,
    val currentNoteNumber: Int,
    val noteCount: Int,
    val feedback: PracticeFeedback,
    val scorePercent: Int?,
    val noteStatuses: List<TabNoteStatus>,
    val playheadPosition: Double
)
