package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.textrecognition.ImageSymbolRecognizer
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.practice.TabNote
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreImageReaderTest {

    /** Fake text recognition: knows the digits printed on the page and returns those inside the asked region. */
    private class PrintedDigits(private val digits: List<RecognizedSymbol>) : ImageSymbolRecognizer {
        val regionsAsked = mutableListOf<PixelRegion>()
        override suspend fun recognize(image: BinaryImage, region: PixelRegion): List<RecognizedSymbol> {
            regionsAsked += region
            return digits.filter { it.left >= region.left && it.right <= region.right && it.top >= region.top && it.bottom <= region.bottom }
        }
    }

    private fun digit(text: String, centreX: Int, centreY: Int) = RecognizedSymbol(text, centreX - 4, centreY - 6, centreX + 4, centreY + 6)

    private fun reader(recognizer: ImageSymbolRecognizer) = ScoreImageReader(BassTunings.STANDARD, recognizer)

    @Test
    fun aTabStaffIsReadWithItsNumbers() = runTest {
        val page = BinaryImage.blank(500, 200)
        val rows = ScoreImageDrawing.staff(page, top = 60, lineCount = 4, spacing = 14, left = 20, right = 480)
        val recognizer = PrintedDigits(listOf(digit("3", 100, rows[3] + 1), digit("5", 150, rows[2] + 1)))

        val pageReading = reader(recognizer).read(page)

        assertEquals(listOf(listOf(listOf(TabNote(0, 3)), listOf(TabNote(1, 5)))), pageReading.tabSystems)
        assertTrue(pageReading.notationSystems.isEmpty())
    }

    @Test
    fun aPageWithOnlySheetMusicIsReadAsNotes() = runTest {
        val page = BinaryImage.blank(500, 250)
        ScoreImageDrawing.staff(page, top = 80, lineCount = 5, spacing = 12, left = 20, right = 480)
        ScoreImageDrawing.quarterNote(page, 120, 129, 12)

        val pageReading = reader(PrintedDigits(emptyList())).read(page)

        assertEquals(listOf(listOf(31)), pageReading.notationSystems)
        assertTrue(pageReading.tabSystems.isEmpty())
    }

    @Test
    fun whenAPageHasSheetMusicAndTabOnlyTheTabIsRead() = runTest {
        val page = BinaryImage.blank(500, 300)
        ScoreImageDrawing.staff(page, top = 40, lineCount = 5, spacing = 12, left = 20, right = 480)
        ScoreImageDrawing.quarterNote(page, 120, 89, 12)
        val tabRows = ScoreImageDrawing.staff(page, top = 150, lineCount = 4, spacing = 14, left = 20, right = 480)
        val recognizer = PrintedDigits(listOf(digit("7", 120, tabRows[3] + 1)))

        val pageReading = reader(recognizer).read(page)

        assertEquals(listOf(listOf(listOf(TabNote(0, 7)))), pageReading.tabSystems)
        assertTrue(pageReading.notationSystems.isEmpty())
    }

    @Test
    fun aFiveLineStaffWithNumbersOnItsLinesIsAFiveStringTab() = runTest {
        val page = BinaryImage.blank(500, 250)
        val rows = ScoreImageDrawing.staff(page, top = 60, lineCount = 5, spacing = 14, left = 20, right = 480)
        val recognizer = PrintedDigits(
            listOf(digit("0", 100, rows[3] + 1), digit("2", 150, rows[2] + 1), digit("4", 200, rows[3] + 1))
        )

        val pageReading = reader(recognizer).read(page)

        assertEquals(listOf(listOf(listOf(TabNote(0, 0)), listOf(TabNote(1, 2)), listOf(TabNote(0, 4)))), pageReading.tabSystems)
    }

    @Test
    fun eachTabStaffIsRecognizedInItsOwnRegion() = runTest {
        val page = BinaryImage.blank(500, 300)
        ScoreImageDrawing.staff(page, top = 40, lineCount = 4, spacing = 14, left = 20, right = 480)
        ScoreImageDrawing.staff(page, top = 180, lineCount = 4, spacing = 14, left = 20, right = 480)
        val recognizer = PrintedDigits(emptyList())

        reader(recognizer).read(page)

        assertEquals(2, recognizer.regionsAsked.size)
        assertTrue(recognizer.regionsAsked[0].bottom < recognizer.regionsAsked[1].top)
    }

    @Test
    fun aPageWithoutStaffsReadsNothing() = runTest {
        val pageReading = reader(PrintedDigits(emptyList())).read(BinaryImage.blank(200, 200))

        assertTrue(pageReading.tabSystems.isEmpty() && pageReading.notationSystems.isEmpty())
    }

    @Test
    fun onAPianoPageOnlyTheBassClefStaffIsRead() = runTest {
        val page = BinaryImage.blank(500, 400)
        ScoreImageDrawing.staff(page, top = 60, lineCount = 5, spacing = 12, left = 20, right = 480)
        ScoreImageDrawing.trebleClef(page, 26, 60, 12)
        ScoreImageDrawing.quarterNote(page, 120, 109, 12)
        ScoreImageDrawing.staff(page, top = 220, lineCount = 5, spacing = 12, left = 20, right = 480)
        ScoreImageDrawing.bassClef(page, 26, 220, 12)
        ScoreImageDrawing.quarterNote(page, 120, 269, 12)

        val pageReading = reader(PrintedDigits(emptyList())).read(page)

        assertEquals(listOf(listOf(31)), pageReading.notationSystems)
    }

    @Test
    fun aPageWithOnlyTrebleClefStaffsIsReadInTrebleClef() = runTest {
        val page = BinaryImage.blank(500, 250)
        ScoreImageDrawing.staff(page, top = 60, lineCount = 5, spacing = 12, left = 20, right = 480)
        ScoreImageDrawing.trebleClef(page, 26, 60, 12)
        ScoreImageDrawing.quarterNote(page, 120, 109, 12)

        val pageReading = reader(PrintedDigits(emptyList())).read(page)

        assertEquals(listOf(listOf(40)), pageReading.notationSystems)
    }
}
