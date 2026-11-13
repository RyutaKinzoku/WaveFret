package com.example.wavefret.common.textrecognition

import org.junit.Assert.assertEquals
import org.junit.Test

class TextGridBuilderTest {

    private val textGridBuilder = TextGridBuilder()

    private fun symbol(text: String, left: Int, top: Int, width: Int = 10, height: Int = 20) =
        RecognizedSymbol(text, left, top, left + width, top + height)

    @Test
    fun placesSymbolsOnARowAtTheirColumns() {
        val symbols = listOf(symbol("G", 0, 0), symbol("|", 10, 0), symbol("3", 40, 0))

        assertEquals("G|  3", textGridBuilder.build(symbols))
    }

    @Test
    fun ordersRowsTopToBottomAndSymbolsLeftToRight() {
        val symbols = listOf(symbol("b", 10, 24), symbol("2", 10, 0), symbol("a", 0, 24), symbol("1", 0, 0))

        assertEquals("12\nab", textGridBuilder.build(symbols))
    }

    @Test
    fun keepsSymbolsWithSlightlyDifferentHeightsOnOneRow() {
        val symbols = listOf(symbol("x", 0, 0), symbol("y", 10, 4), symbol("z", 20, -3))

        assertEquals("xyz", textGridBuilder.build(symbols))
    }

    @Test
    fun alignsColumnsAcrossRows() {
        val symbols = listOf(
            symbol("G", 0, 0), symbol("5", 50, 0),
            symbol("D", 0, 24), symbol("7", 50, 24)
        )

        assertEquals("G    5\nD    7", textGridBuilder.build(symbols))
    }

    @Test
    fun separatesDistantRowsWithABlankLine() {
        val symbols = listOf(symbol("a", 0, 0), symbol("b", 0, 100))

        assertEquals("a\n\nb", textGridBuilder.build(symbols))
    }

    @Test
    fun spreadsAMultiCharacterSymbolAcrossItsWidth() {
        val symbols = listOf(symbol("E", 0, 0), symbol("---", 10, 0, width = 30), symbol("1", 40, 0))

        assertEquals("E---1", textGridBuilder.build(symbols))
    }

    @Test
    fun overlappingSymbolsTakeTheNextFreeColumn() {
        val symbols = listOf(symbol("1", 0, 0), symbol("2", 3, 0))

        assertEquals("12", textGridBuilder.build(symbols))
    }

    @Test
    fun noSymbolsGiveEmptyText() {
        assertEquals("", textGridBuilder.build(emptyList()))
    }
}
