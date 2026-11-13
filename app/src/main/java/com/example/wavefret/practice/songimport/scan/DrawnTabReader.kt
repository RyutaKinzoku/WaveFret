package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.practice.TabNote
import kotlin.math.abs

/**
 * Reads a tab staff drawn as lines with fret numbers on them (as Guitar
 * Pro, MuseScore and most video lessons draw tabs): each recognized
 * number belongs to the string of the line it sits on, numbers in one
 * column are played together, and columns are read left to right.
 * Numbers away from the lines (bar numbers, tuplets) and letters are
 * ignored. 4-line bass tabs are read; on a 5-line tab the low B line is
 * dropped; guitar tabs are not read.
 *
 * @property tuning Tuning the tab is read in. Type: BassTuning
 * @property lineTolerance How far a number's centre may be from a line, as a share of the line spacing. Type: Double
 */
class DrawnTabReader(
    private val tuning: BassTuning,
    private val lineTolerance: Double = DEFAULT_LINE_TOLERANCE
) {

    /**
     * @param staff A tab staff. Type: Staff
     * @param symbols Symbols recognized around it, in page pixels. Type: List<RecognizedSymbol>
     * @return The staff's notes as columns, left to right; each column low string first. Type: List<List<TabNote>>
     */
    fun read(staff: Staff, symbols: List<RecognizedSymbol>): List<List<TabNote>> {
        if (staff.lineYs.size !in MINIMUM_LINES..MAXIMUM_LINES) return emptyList()
        val numbersByLine = digitsOnLines(staff, symbols).groupBy({ it.first }, { it.second })
        val placedNotes = numbersByLine.flatMap { (lineIndex, digits) ->
            val stringIndex = tuning.strings.size - 1 - lineIndex
            if (stringIndex < 0) emptyList() else joinNeighbouringDigits(digits).map { (centreX, fret) ->
                PlacedNote(centreX, TabNote(stringIndex, fret))
            }
        }.sortedBy { it.centreX }

        val columns = mutableListOf<MutableList<PlacedNote>>()
        placedNotes.forEach { placedNote ->
            val currentColumn = columns.lastOrNull()
            if (currentColumn != null && placedNote.centreX - currentColumn.first().centreX <= staff.lineSpacing * COLUMN_WIDTH_IN_SPACINGS) {
                currentColumn += placedNote
            } else {
                columns += mutableListOf(placedNote)
            }
        }
        return columns.map { column -> column.map { it.tabNote }.sortedBy { it.stringIndex } }
    }

    /**
     * @param staff A staff. Type: Staff
     * @param symbols Symbols recognized around it. Type: List<RecognizedSymbol>
     * @return How many digits sit on its lines, used to tell tab staffs from music staffs. Type: Int
     */
    fun countNumbersOnLines(staff: Staff, symbols: List<RecognizedSymbol>): Int = digitsOnLines(staff, symbols).size

    /**
     * @param staff A staff. Type: Staff
     * @param symbols Recognized symbols. Type: List<RecognizedSymbol>
     * @return Digit symbols on a line, paired with that line's index (0 = top). Type: List<Pair<Int, RecognizedSymbol>>
     */
    private fun digitsOnLines(staff: Staff, symbols: List<RecognizedSymbol>): List<Pair<Int, RecognizedSymbol>> {
        return symbols
            .filter { symbol -> symbol.text.isNotEmpty() && symbol.text.all(Char::isDigit) }
            .mapNotNull { symbol ->
                val nearestLineIndex = staff.lineYs.indices.minBy { abs(staff.lineYs[it] - symbol.centerY) }
                val distance = abs(staff.lineYs[nearestLineIndex] - symbol.centerY)
                if (distance <= staff.lineSpacing * lineTolerance) nearestLineIndex to symbol else null
            }
    }

    /**
     * @param digits Digit symbols on one line. Type: List<RecognizedSymbol>
     * @return Each number's horizontal centre and value, digits that touch joined (1 and 2 make 12). Type: List<Pair<Double, Int>>
     */
    private fun joinNeighbouringDigits(digits: List<RecognizedSymbol>): List<Pair<Double, Int>> {
        val numbers = mutableListOf<MutableList<RecognizedSymbol>>()
        digits.sortedBy { it.left }.forEach { digit ->
            val currentNumber = numbers.lastOrNull()
            if (currentNumber != null && digit.left - currentNumber.last().right <= digit.height * JOIN_GAP_IN_HEIGHTS) {
                currentNumber += digit
            } else {
                numbers += mutableListOf(digit)
            }
        }
        return numbers.mapNotNull { numberDigits ->
            val fret = numberDigits.joinToString("") { it.text }.toIntOrNull() ?: return@mapNotNull null
            val centreX = (numberDigits.first().left + numberDigits.last().right) / 2.0
            centreX to fret
        }
    }

    /**
     * A note with its horizontal position on the page.
     *
     * @property centreX Horizontal centre of the number in pixels. Type: Double
     * @property tabNote The note. Type: TabNote
     */
    private data class PlacedNote(val centreX: Double, val tabNote: TabNote)

    private companion object {
        const val DEFAULT_LINE_TOLERANCE = 0.3
        const val MINIMUM_LINES = 4
        const val MAXIMUM_LINES = 5
        const val COLUMN_WIDTH_IN_SPACINGS = 0.5
        const val JOIN_GAP_IN_HEIGHTS = 0.5
    }
}
