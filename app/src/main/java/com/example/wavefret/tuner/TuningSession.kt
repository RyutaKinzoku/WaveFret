package com.example.wavefret.tuner

/**
 * Which string is being tuned and which are already done. With no string
 * chosen, strings are tuned from the lowest up, skipping tuned ones; the
 * player can choose a string instead (choosing it again returns to
 * automatic). Choosing or unmarking a tuned string makes it out of tune again.
 *
 * @param stringCount Strings on the instrument. Type: Int
 */
class TuningSession(private val stringCount: Int) {

    private val tunedStrings = sortedSetOf<Int>()

    /** String the player chose, or null for automatic. Type: Int? */
    var selectedStringIndex: Int? = null
        private set

    /** Strings marked as tuned. Type: Set<Int> */
    val tunedStringIndices: Set<Int> get() = tunedStrings.toSet()

    /** True when no string is chosen, so strings are tuned lowest first. Type: Boolean */
    val isAutomatic: Boolean get() = selectedStringIndex == null

    /** True once every string is marked tuned. Type: Boolean */
    val isAllTuned: Boolean get() = tunedStrings.size == stringCount

    /** The string being tuned now, or null when every string is tuned. Type: Int? */
    val targetStringIndex: Int?
        get() = selectedStringIndex ?: (0 until stringCount).firstOrNull { it !in tunedStrings }

    /**
     * @param stringIndex String the player tapped. Type: Int
     * @return Unit
     */
    fun select(stringIndex: Int) {
        if (stringIndex == selectedStringIndex) {
            selectedStringIndex = null
            return
        }
        tunedStrings.remove(stringIndex)
        selectedStringIndex = stringIndex
    }

    /**
     * @param stringIndex String now in tune. Type: Int
     * @return Unit
     */
    fun markTuned(stringIndex: Int) {
        tunedStrings.add(stringIndex)
        if (selectedStringIndex == stringIndex) selectedStringIndex = null
    }

    /**
     * @param stringIndex String to treat as out of tune again. Type: Int
     * @return Unit
     */
    fun unmark(stringIndex: Int) {
        tunedStrings.remove(stringIndex)
    }

    /**
     * Back to every string out of tune, tuning lowest first.
     *
     * @return Unit
     */
    fun resetAll() {
        tunedStrings.clear()
        selectedStringIndex = null
    }
}
