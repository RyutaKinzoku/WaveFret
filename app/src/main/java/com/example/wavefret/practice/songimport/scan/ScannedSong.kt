package com.example.wavefret.practice.songimport.scan

/**
 * The bass part read from a photo or PDF, ready to be checked and saved.
 *
 * @property source Where the notes came from. Type: Source
 * @property tabText The notes as ASCII tab, or null if none were found. Type: String?
 * @property recognizedText All text recognized, shown when no tab was found. Type: String
 */
data class ScannedSong(val source: Source, val tabText: String?, val recognizedText: String) {

    /** Where the notes of a scanned song came from. */
    enum class Source {
        /** Tab staffs drawn with lines and numbers. */
        DRAWN_TAB,

        /** Sheet music in bass clef (experimental). */
        SHEET_MUSIC,

        /** A tab written as text (G|---). */
        TEXT_TAB,

        /** No notes were found. */
        NOTHING
    }
}
