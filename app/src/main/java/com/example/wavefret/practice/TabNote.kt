package com.example.wavefret.practice

import com.example.wavefret.common.music.BassString
import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.music.fretFrequencyHz

/**
 * One note of a tab: which string to play and at which fret. Strings are
 * indexed low to high, matching [BassTuning.strings] ordering, so the same
 * tab can be resolved against any tuning.
 *
 * @property stringIndex Zero-based string index, 0 being the lowest string. Type: Int
 * @property fret Fret number, 0 being the open string. Type: Int
 */
data class TabNote(
    val stringIndex: Int,
    val fret: Int
) {
    init {
        require(stringIndex >= 0) { "String index must not be negative, was $stringIndex" }
        require(fret >= 0) { "Fret must not be negative, was $fret" }
    }

    /**
     * @param tuning Tuning the tab is played in. Type: BassTuning
     * @return The pitch this note should sound at, in Hz. Type: Double
     */
    fun expectedFrequencyHz(tuning: BassTuning): Double {
        return fretFrequencyHz(stringIn(tuning).frequencyHz, fret)
    }

    /**
     * @param tuning Tuning the tab is played in. Type: BassTuning
     * @return Display name of the string this note is played on. Type: String
     */
    fun stringName(tuning: BassTuning): String = stringIn(tuning).name

    /**
     * @param tuning Tuning to look the string up in. Type: BassTuning
     * @return The string this note is played on. Type: BassString
     */
    private fun stringIn(tuning: BassTuning): BassString {
        require(stringIndex < tuning.strings.size) {
            "String index $stringIndex is outside the ${tuning.strings.size}-string ${tuning.name} tuning"
        }
        return tuning.strings[stringIndex]
    }
}
