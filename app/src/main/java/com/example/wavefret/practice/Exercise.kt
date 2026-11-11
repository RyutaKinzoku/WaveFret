package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTuning

/**
 * A practice piece: an ordered sequence of tab notes, the tuning they are
 * played in, and the tempo used when practicing in time.
 *
 * @property title Display name of the exercise. Type: String
 * @property tuning Tuning the notes are written for. Type: BassTuning
 * @property tempoBpm Tempo for tempo mode, one note per beat, in beats per minute. Type: Int
 * @property notes The notes to play, in order. Type: List<TabNote>
 * @property noteOnsetsMillis When each note starts in the original recording, or null
 *   for exercises without timing. Type: List<Long>?
 */
data class Exercise(
    val title: String,
    val tuning: BassTuning,
    val tempoBpm: Int,
    val notes: List<TabNote>,
    val noteOnsetsMillis: List<Long>? = null
) {
    init {
        require(notes.isNotEmpty()) { "Exercise must contain at least one note" }
        require(tempoBpm > 0) { "Tempo must be positive, was $tempoBpm" }
        require(notes.all { it.stringIndex < tuning.strings.size }) {
            "Every note must be on a string of the ${tuning.name} tuning"
        }
        if (noteOnsetsMillis != null) {
            require(noteOnsetsMillis.size == notes.size) { "There must be one timing per note" }
            require(noteOnsetsMillis.zipWithNext().all { (earlier, later) -> later >= earlier }) {
                "Note timings must not go backwards"
            }
        }
    }

    /** How many notes the exercise has. Type: Int */
    val noteCount: Int get() = notes.size
}
