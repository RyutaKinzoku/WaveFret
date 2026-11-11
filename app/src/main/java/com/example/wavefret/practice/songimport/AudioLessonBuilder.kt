package com.example.wavefret.practice.songimport

import com.example.wavefret.common.transcription.NoteTranscriber
import com.example.wavefret.common.transcription.TempoEstimator
import com.example.wavefret.practice.Exercise

/**
 * Turns recorded or decoded audio into a timed bass lesson: transcribes the
 * notes, estimates a practice tempo from their spacing, and places them on
 * the fretboard.
 *
 * @property noteTranscriber Detects the notes (YIN for bass-only, basic-pitch for full songs). Type: NoteTranscriber
 * @property tempoEstimator Guesses a tempo from note spacing. Type: TempoEstimator
 * @property noteEventsLessonBuilder Places notes on the fretboard. Type: NoteEventsLessonBuilder
 */
class AudioLessonBuilder(
    private val noteTranscriber: NoteTranscriber,
    private val tempoEstimator: TempoEstimator,
    private val noteEventsLessonBuilder: NoteEventsLessonBuilder
) {

    /**
     * @param title Lesson title. Type: String
     * @param samples Mono audio samples in -1..1. Type: FloatArray
     * @param sampleRateHz Their sample rate. Type: Int
     * @return The draft lesson, ready for the editor. Type: Exercise
     * @throws IllegalArgumentException If no notes are heard.
     */
    fun build(title: String, samples: FloatArray, sampleRateHz: Int): Exercise {
        val noteEvents = noteTranscriber.transcribe(samples, sampleRateHz)
        require(noteEvents.isNotEmpty()) { NO_NOTES_REASON }
        val tempoBpm = tempoEstimator.estimateBpm(noteEvents.map { it.onsetMillis })
        return noteEventsLessonBuilder.build(title, tempoBpm, noteEvents)
    }

    companion object {
        /** Why audio produced no lesson. Type: String */
        const val NO_NOTES_REASON = "No notes could be heard in this audio"
    }
}
