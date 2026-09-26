package com.example.wavefret.tuner

/**
 * The outcome of comparing a detected pitch against a BassTuning.
 *
 * @property closestString The string the detected pitch was nearest to. Type: BassString
 * @property centsOffset How far the pitch was from closestString's correct
 *   pitch, in cents. Positive is sharp, negative is flat. Type: Double
 * @property status Whether that offset counts as flat, in tune, or sharp. Type: TuningStatus
 */
data class TuningResult(
    val closestString: BassString,
    val centsOffset: Double,
    val status: TuningStatus
)