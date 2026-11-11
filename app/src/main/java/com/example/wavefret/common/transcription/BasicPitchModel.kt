package com.example.wavefret.common.transcription

/**
 * The raw output of one basic-pitch model run: two frames-by-88 maps whose
 * roles (note vs onset) are resolved by [BasicPitchOutputRoles].
 *
 * @property firstMap First 88-bin output. Type: Array<FloatArray>
 * @property secondMap Second 88-bin output. Type: Array<FloatArray>
 */
class BasicPitchPrediction(val firstMap: Array<FloatArray>, val secondMap: Array<FloatArray>)

/** Runs Spotify's basic-pitch network on one window of audio. */
interface BasicPitchModel {

    /**
     * @param window Exactly [BasicPitchWindowing.WINDOW_SAMPLES] mono samples at 22 050 Hz. Type: FloatArray
     * @return The model's note and onset maps for that window. Type: BasicPitchPrediction
     */
    fun predict(window: FloatArray): BasicPitchPrediction
}
