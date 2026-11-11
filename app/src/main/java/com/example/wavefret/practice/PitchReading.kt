package com.example.wavefret.practice

/** How one audio buffer compares against the note currently expected. */
enum class PitchReading {
    /** No usable pitch: silence, or nothing clear enough to detect. */
    SILENT,

    /** A pitch that counts as the expected note. */
    MATCHING,

    /** A pitch that is some other note. */
    NOT_MATCHING
}
