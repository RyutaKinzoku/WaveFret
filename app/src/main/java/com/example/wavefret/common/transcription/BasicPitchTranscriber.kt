package com.example.wavefret.common.transcription

import com.example.wavefret.common.audio.AudioResampler
import com.example.wavefret.common.music.NoteEvent

/**
 * Transcribes full mixes (bass with drums, guitars, vocals) with Spotify's
 * basic-pitch model: audio is resampled to 22 050 Hz, cut into windows, run
 * through the model, stitched back together and decoded into notes, of
 * which only those in the bass register are kept. Rough on busy mixes;
 * results are meant to be checked in the lesson editor.
 *
 * @property basicPitchModel Runs the neural network. Type: BasicPitchModel
 * @property basicPitchWindowing Cuts and stitches model windows. Type: BasicPitchWindowing
 * @property posteriorgramNoteDecoder Turns model maps into notes. Type: PosteriorgramNoteDecoder
 * @property audioResampler Converts audio to the model's sample rate. Type: AudioResampler
 * @property highestBassMidiNumber Notes above this are treated as other instruments. Type: Int
 * @property bassLineExtractor Keeps only the bass line out of all instruments. Type: BassLineExtractor
 */
class BasicPitchTranscriber(
    private val basicPitchModel: BasicPitchModel,
    private val basicPitchWindowing: BasicPitchWindowing,
    private val posteriorgramNoteDecoder: PosteriorgramNoteDecoder,
    private val audioResampler: AudioResampler,
    private val highestBassMidiNumber: Int = DEFAULT_HIGHEST_BASS_MIDI_NUMBER,
    private val bassLineExtractor: BassLineExtractor = BassLineExtractor()
) : NoteTranscriber {

    /**
     * @param samples Mono audio samples in -1..1. Type: FloatArray
     * @param sampleRateHz Their sample rate. Type: Int
     * @return Bass-register notes in time order. Type: List<NoteEvent>
     */
    override fun transcribe(samples: FloatArray, sampleRateHz: Int): List<NoteEvent> {
        val modelAudio = audioResampler.resample(samples, sampleRateHz, BasicPitchWindowing.SAMPLE_RATE_HZ)
        val predictions = basicPitchWindowing.windows(modelAudio).map { window ->
            val prediction = basicPitchModel.predict(window)
            BasicPitchOutputRoles.assign(prediction.firstMap, prediction.secondMap)
        }
        val noteFrames = basicPitchWindowing.stitch(predictions.map { it.notes }, modelAudio.size)
        val onsetFrames = basicPitchWindowing.stitch(predictions.map { it.onsets }, modelAudio.size)
        val bassRangeNotes = posteriorgramNoteDecoder.decode(noteFrames, onsetFrames).filter { it.midiNumber <= highestBassMidiNumber }
        return bassLineExtractor.extract(bassRangeNotes)
    }

    private companion object {
        /** E4: the 20th fret of a bass's G string, with a little headroom. */
        const val DEFAULT_HIGHEST_BASS_MIDI_NUMBER = 64
    }
}
