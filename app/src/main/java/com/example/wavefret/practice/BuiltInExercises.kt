package com.example.wavefret.practice

/**
 * The exercises WaveFret ships with, written in the app's own tab format
 * so they double as examples of it.
 *
 * @property tabTextParser Parser that turns each built-in tab into an Exercise. Type: TabTextParser
 */
class BuiltInExercises(private val tabTextParser: TabTextParser) {

    /** @return Every built-in exercise, easiest first. Type: List<Exercise> */
    fun all(): List<Exercise> = BUILT_IN_TAB_TEXTS.map(tabTextParser::parse)

    private companion object {
        val BUILT_IN_TAB_TEXTS = listOf(
            """
            title: Open strings
            tempo: 60
            # Every open string, low to high and back
            E0 A0 D0 G0
            G0 D0 A0 E0
            """,
            """
            title: E minor pentatonic
            tempo: 70
            E0 E3 A0 A2 D0 D2 G0 G2
            G2 G0 D2 D0 A2 A0 E3 E0
            """,
            """
            title: C major scale
            tempo: 70
            # One octave from the 3rd fret of the A string
            A3 A5 D2 D3 D5 G2 G4 G5
            """,
            """
            title: Root-fifth groove
            tempo: 80
            # Root and fifth over A, D and E
            A0 E0 A0 E0
            D0 A0 D0 A0
            E0 E7 E0 E7
            """,
            """
            title: Octave jumps
            tempo: 75
            E3 D5 A3 G5 A5 G7 E5 D7
            """
        )
    }
}
