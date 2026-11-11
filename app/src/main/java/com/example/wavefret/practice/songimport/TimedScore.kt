package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.NoteEvent

/**
 * A bass part read from a file, before it becomes a lesson.
 *
 * @property title Title found in the file, or null. Type: String?
 * @property tempoBpm Tempo at the start, in beats per minute. Type: Int
 * @property noteEvents The part's notes with their timing. Type: List<NoteEvent>
 */
data class TimedScore(
    val title: String?,
    val tempoBpm: Int,
    val noteEvents: List<NoteEvent>
)
