package com.example.wavefret.practice

/** What the practice screen tells the player about what it just heard. */
enum class PracticeFeedback {
    /** Nothing heard yet for the current note. */
    LISTENING,

    /** The expected note was heard. */
    CORRECT,

    /** A different note was heard. */
    WRONG_NOTE,

    /** The exercise is finished. */
    COMPLETE
}
