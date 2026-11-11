package com.example.wavefret.common.transcription

import com.example.wavefret.common.music.NoteEvent

/** Detects the notes played in a stretch of recorded audio. */
interface NoteTranscriber {

    /**
     * @param samples Mono audio samples in -1..1. Type: FloatArray
     * @param sampleRateHz Their sample rate. Type: Int
     * @return Detected notes in time order. Type: List<NoteEvent>
     */
    fun transcribe(samples: FloatArray, sampleRateHz: Int): List<NoteEvent>
}
