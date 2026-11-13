package com.example.wavefret.common.textrecognition

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rebuilds monospaced text from recognized symbols and their positions,
 * so column-based formats such as ASCII tab survive a photo or a PDF:
 * symbols on the same height become one row, and each symbol is placed
 * at the column its horizontal position corresponds to.
 *
 * @property rowGapForBlankLine Vertical distance between rows, in symbol heights, above which a blank line is inserted. Type: Double
 */
class TextGridBuilder(private val rowGapForBlankLine: Double = DEFAULT_ROW_GAP_FOR_BLANK_LINE) {

    /**
     * @param symbols Recognized symbols in any order. Type: List<RecognizedSymbol>
     * @return The text, one line per row, top to bottom. Type: String
     */
    fun build(symbols: List<RecognizedSymbol>): String {
        if (symbols.isEmpty()) return ""
        val typicalHeight = median(symbols.map { it.height.toDouble() })
        val characterWidth = median(symbols.map { it.width.toDouble() / it.text.length.coerceAtLeast(1) }).coerceAtLeast(1.0)
        val leftEdge = symbols.minOf { it.left }
        val rows = groupIntoRows(symbols, typicalHeight)

        val lines = mutableListOf<String>()
        var previousRowCenter: Double? = null
        for (row in rows) {
            val rowCenter = row.map { it.centerY }.average()
            if (previousRowCenter != null && rowCenter - previousRowCenter > rowGapForBlankLine * typicalHeight) {
                lines += ""
            }
            lines += layOutRow(row, leftEdge, characterWidth)
            previousRowCenter = rowCenter
        }
        return lines.joinToString("\n")
    }

    /**
     * @param symbols All symbols. Type: List<RecognizedSymbol>
     * @param typicalHeight Median symbol height in pixels. Type: Double
     * @return Rows top to bottom, each a list of its symbols. Type: List<List<RecognizedSymbol>>
     */
    private fun groupIntoRows(symbols: List<RecognizedSymbol>, typicalHeight: Double): List<List<RecognizedSymbol>> {
        val rows = mutableListOf<MutableList<RecognizedSymbol>>()
        for (symbol in symbols.sortedBy { it.centerY }) {
            val currentRow = rows.lastOrNull()
            if (currentRow != null && abs(symbol.centerY - currentRow.map { it.centerY }.average()) <= typicalHeight / 2) {
                currentRow += symbol
            } else {
                rows += mutableListOf(symbol)
            }
        }
        return rows
    }

    /**
     * @param row Symbols of one row. Type: List<RecognizedSymbol>
     * @param leftEdge Leftmost pixel of all symbols, column 0. Type: Int
     * @param characterWidth Width of one column in pixels. Type: Double
     * @return The row as text, without trailing spaces. Type: String
     */
    private fun layOutRow(row: List<RecognizedSymbol>, leftEdge: Int, characterWidth: Double): String {
        val line = StringBuilder()
        var lastColumn = -1
        for (symbol in row.sortedBy { it.left }) {
            val pixelsPerCharacter = symbol.width.toDouble() / symbol.text.length.coerceAtLeast(1)
            symbol.text.forEachIndexed { characterIndex, character ->
                val x = symbol.left + characterIndex * pixelsPerCharacter
                val column = maxOf(((x - leftEdge) / characterWidth).roundToInt(), lastColumn + 1)
                while (line.length < column) line.append(' ')
                line.append(character)
                lastColumn = column
            }
        }
        return line.toString().trimEnd()
    }

    /**
     * @param values Values to take the median of; not empty. Type: List<Double>
     * @return The median value. Type: Double
     */
    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }

    private companion object {
        const val DEFAULT_ROW_GAP_FOR_BLANK_LINE = 2.0
    }
}
