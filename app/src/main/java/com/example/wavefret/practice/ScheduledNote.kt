package com.example.wavefret.practice

/**
 * One note of a lesson as it is played back for listening.
 *
 * @property noteIndex Index of the note in the lesson. Type: Int
 * @property frequencyHz Pitch to play, in Hz. Type: Double
 * @property startMillis When the note starts, from the start of playback. Type: Long
 * @property durationMillis How long it sounds before the next note. Type: Long
 */
data class ScheduledNote(val noteIndex: Int, val frequencyHz: Double, val startMillis: Long, val durationMillis: Long)
