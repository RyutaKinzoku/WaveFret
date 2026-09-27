package com.example.wavefret.tuner

/**
 * What the tuner screen should currently show, derived from the latest
 * tuning pipeline output.
 */
sealed class TunerDisplayState {

    /**
     * No confident pitch reading yet (silence, or no clear pitch) — the
     * screen should invite the player to play a note.
     */
    object Listening : TunerDisplayState()

    /**
     * A string was confidently identified and evaluated.
     *
     * @property stringName The name of the closest bass string, e.g. "E". Type: String
     * @property centsOffsetLabel A human-readable cents offset, e.g. "+7¢". Type: String
     * @property status Whether the detected pitch is flat, in tune, or sharp. Type: TuningStatus
     * @property needleRotationDegrees How far to rotate the tuner needle from center. Type: Float
     */
    data class Detected(
        val stringName: String,
        val centsOffsetLabel: String,
        val status: TuningStatus,
        val needleRotationDegrees: Float
    ) : TunerDisplayState()
}