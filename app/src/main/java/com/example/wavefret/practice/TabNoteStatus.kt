package com.example.wavefret.practice

/** How one note of the tab should be drawn on the scrolling staff. */
enum class TabNoteStatus {
    /** Already played correctly. */
    HIT,

    /** Its time passed without it being played (tempo mode). */
    MISSED,

    /** The note the player should play now. */
    CURRENT,

    /** Still to come. */
    UPCOMING
}
