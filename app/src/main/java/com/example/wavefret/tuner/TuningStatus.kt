package com.example.wavefret.tuner

/** How a detected pitch compares to its closest string's correct pitch. */
enum class TuningStatus {
    FLAT,
    IN_TUNE,
    SHARP
}