package com.example.wavefret.practice

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Geometry for the scrolling tab staff: notes move right-to-left past a
 * fixed playhead line, one note spacing per note position, with the
 * highest string on top as in standard bass tab.
 *
 * @property noteSpacingPx Horizontal distance between consecutive notes. Type: Float
 * @property playheadXPx Horizontal position of the playhead line. Type: Float
 * @property topPaddingPx Vertical position of the top string line. Type: Float
 * @property stringSpacingPx Vertical distance between string lines. Type: Float
 */
class TabStaffLayout(
    private val noteSpacingPx: Float,
    private val playheadXPx: Float,
    private val topPaddingPx: Float,
    private val stringSpacingPx: Float
) {

    /**
     * @param noteIndex Note to place. Type: Int
     * @param playheadPosition Current scroll position, in note positions. Type: Double
     * @return Horizontal center of the note. Type: Float
     */
    fun noteCenterXPx(noteIndex: Int, playheadPosition: Double): Float {
        return playheadXPx + ((noteIndex - playheadPosition) * noteSpacingPx).toFloat()
    }

    /**
     * @param stringIndex String to place, 0 being the lowest. Type: Int
     * @param stringCount Strings in the tuning. Type: Int
     * @return Vertical position of that string's line. Type: Float
     */
    fun stringLineYPx(stringIndex: Int, stringCount: Int): Float {
        return topPaddingPx + (stringCount - 1 - stringIndex) * stringSpacingPx
    }

    /**
     * Notes whose centers lie on screen, with one note spacing of margin on
     * each side so notes slide in and out instead of popping.
     *
     * @param noteCount Notes in the exercise. Type: Int
     * @param playheadPosition Current scroll position, in note positions. Type: Double
     * @param viewWidthPx Width of the staff view. Type: Float
     * @return Range of note indices worth drawing. Type: IntRange
     */
    fun visibleNoteIndices(noteCount: Int, playheadPosition: Double, viewWidthPx: Float): IntRange {
        val firstVisiblePosition = playheadPosition + (-noteSpacingPx - playheadXPx) / noteSpacingPx
        val lastVisiblePosition = playheadPosition + (viewWidthPx + noteSpacingPx - playheadXPx) / noteSpacingPx
        val firstIndex = maxOf(0, ceil(firstVisiblePosition).toInt())
        val lastIndex = minOf(noteCount - 1, floor(lastVisiblePosition).toInt())
        return firstIndex..lastIndex
    }
}
