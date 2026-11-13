package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawnTabReaderTest {

    private val drawnTabReader = DrawnTabReader(BassTunings.STANDARD)
    private val fourLineStaff = Staff(lineYs = listOf(50.0, 64.0, 78.0, 92.0), lineThickness = 2.0, left = 10, right = 400)

    /** A digit 8 px wide and 12 px tall centred at ([centreX], [centreY]). */
    private fun digit(text: String, centreX: Int, centreY: Int) =
        RecognizedSymbol(text, centreX - 4, centreY - 6, centreX + 4, centreY + 6)

    @Test
    fun readsEachNumberOnTheStringOfItsLine() {
        val symbols = listOf(digit("3", 100, 92), digit("5", 130, 78), digit("0", 160, 50))

        val columns = drawnTabReader.read(fourLineStaff, symbols)

        assertEquals(listOf(listOf(TabNote(0, 3)), listOf(TabNote(1, 5)), listOf(TabNote(3, 0))), columns)
    }

    @Test
    fun joinsNeighbouringDigitsIntoOneFret() {
        val symbols = listOf(digit("1", 100, 64), digit("2", 109, 64))

        assertEquals(listOf(listOf(TabNote(2, 12))), drawnTabReader.read(fourLineStaff, symbols))
    }

    @Test
    fun numbersStackedInOneColumnAreOneChordLowStringFirst() {
        val symbols = listOf(digit("2", 100, 78), digit("0", 101, 92))

        assertEquals(listOf(listOf(TabNote(0, 0), TabNote(1, 2))), drawnTabReader.read(fourLineStaff, symbols))
    }

    @Test
    fun ignoresBarNumbersLettersAndMarksAwayFromTheLines() {
        val symbols = listOf(
            digit("7", 100, 30), digit("T", 20, 57), RecognizedSymbol("A", 16, 65, 24, 77),
            digit("3", 140, 57), digit("5", 180, 92)
        )

        assertEquals(listOf(listOf(TabNote(0, 5))), drawnTabReader.read(fourLineStaff, symbols))
    }

    @Test
    fun readsTheFourHighStringsOfAFiveStringTab() {
        val fiveLineStaff = Staff(listOf(50.0, 64.0, 78.0, 92.0, 106.0), 2.0, 10, 400)
        val symbols = listOf(digit("2", 100, 106), digit("4", 130, 92), digit("1", 160, 50))

        assertEquals(listOf(listOf(TabNote(0, 4)), listOf(TabNote(3, 1))), drawnTabReader.read(fiveLineStaff, symbols))
    }

    @Test
    fun guitarTabsAreNotRead() {
        val sixLineStaff = Staff(listOf(50.0, 64.0, 78.0, 92.0, 106.0, 120.0), 2.0, 10, 400)

        assertEquals(emptyList<List<TabNote>>(), drawnTabReader.read(sixLineStaff, listOf(digit("3", 100, 120))))
    }

    @Test
    fun countsDigitsSittingOnTheLines() {
        val symbols = listOf(digit("3", 100, 92), digit("7", 100, 30), digit("5", 130, 78))

        assertEquals(2, drawnTabReader.countNumbersOnLines(fourLineStaff, symbols))
    }

    @Test
    fun readsLettersThatTextRecognitionConfusesWithDigits() {
        val symbols = listOf(digit("O", 100, 92), digit("l", 130, 78), digit("S", 160, 64))

        assertEquals(listOf(listOf(TabNote(0, 0)), listOf(TabNote(1, 1)), listOf(TabNote(2, 5))), drawnTabReader.read(fourLineStaff, symbols))
    }

    @Test
    fun ignoresTheTabClefAtTheStartOfTheStaff() {
        val symbols = listOf(digit("T", 22, 57), digit("A", 22, 71), digit("B", 22, 85), digit("0", 22, 92), digit("3", 100, 92))

        assertEquals(listOf(listOf(TabNote(0, 3))), drawnTabReader.read(fourLineStaff, symbols))
    }
}
