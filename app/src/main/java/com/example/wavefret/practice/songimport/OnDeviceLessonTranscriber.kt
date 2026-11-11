package com.example.wavefret.practice.songimport

import android.content.Context
import com.example.wavefret.common.audio.AudioResampler
import com.example.wavefret.common.audio.YinPitchDetector
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.transcription.BasicPitchTranscriber
import com.example.wavefret.common.transcription.BasicPitchWindowing
import com.example.wavefret.common.transcription.MonophonicTranscriber
import com.example.wavefret.common.transcription.PosteriorgramNoteDecoder
import com.example.wavefret.common.transcription.TempoEstimator
import com.example.wavefret.common.transcription.TfLiteBasicPitchModel
import com.example.wavefret.practice.Exercise

/**
 * Picks and wires the right on-device note detector for a piece of audio
 * and builds a draft lesson from it: YIN for bass-only audio, the bundled
 * basic-pitch model for full songs. The model is opened per call and
 * closed straight after, so it never stays in memory.
 *
 * Composition glue around the tested [AudioLessonBuilder] and transcribers;
 * verified on-device.
 *
 * @param context Context used to open the model asset. Type: Context
 */
class OnDeviceLessonTranscriber(private val context: Context) {

    /**
     * Call from a background thread: this can take a while for long songs.
     *
     * @param title Draft lesson title. Type: String
     * @param samples Mono audio in -1..1. Type: FloatArray
     * @param sampleRateHz Its sample rate. Type: Int
     * @param isFullSong True for full mixes (basic-pitch), false for bass only (YIN). Type: Boolean
     * @return The draft lesson. Type: Exercise
     * @throws IllegalArgumentException If no notes are heard.
     */
    fun transcribe(title: String, samples: FloatArray, sampleRateHz: Int, isFullSong: Boolean): Exercise {
        val tuning = BassTunings.STANDARD
        val lessonBuilder = NoteEventsLessonBuilder(tuning, FretboardPositionFinder(tuning))
        if (!isFullSong) {
            return AudioLessonBuilder(MonophonicTranscriber(YinPitchDetector()), TempoEstimator(), lessonBuilder)
                .build(title, samples, sampleRateHz)
        }
        return TfLiteBasicPitchModel(context).use { basicPitchModel ->
            val transcriber = BasicPitchTranscriber(basicPitchModel, BasicPitchWindowing(), PosteriorgramNoteDecoder(), AudioResampler())
            AudioLessonBuilder(transcriber, TempoEstimator(), lessonBuilder).build(title, samples, sampleRateHz)
        }
    }
}
