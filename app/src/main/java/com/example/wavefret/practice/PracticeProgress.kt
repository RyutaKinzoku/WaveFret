package com.example.wavefret.practice

/**
 * Snapshot of how far through an exercise the player is.
 *
 * @property currentNoteIndex Index of the note now expected; equals noteCount once complete. Type: Int
 * @property noteCount Total notes in the exercise. Type: Int
 * @property hitCount Notes played correctly so far. Type: Int
 * @property missCount Notes missed so far (only tempo mode can miss). Type: Int
 * @property lastReading How the most recent buffer compared to the expected note. Type: PitchReading
 */
data class PracticeProgress(
    val currentNoteIndex: Int,
    val noteCount: Int,
    val hitCount: Int,
    val missCount: Int,
    val lastReading: PitchReading
) {
    /** True once every note has been either hit or missed. Type: Boolean */
    val isComplete: Boolean get() = currentNoteIndex >= noteCount
}
