package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import org.junit.Assert.assertEquals
import org.junit.Test

class NotationStaffReaderTest {

    private val spacing = 12
    private val notationStaffReader = NotationStaffReader()

    /** A page with an empty bass-clef staff; lines at rows 100, 112, 124, 136 and 148. */
    private fun blankStaffPage(): BinaryImage {
        val image = BinaryImage.blank(600, 300)
        ScoreImageDrawing.staff(image, top = 100, lineCount = 5, spacing = spacing, left = 20, right = 580)
        return image
    }

    /** Row of staff position [position]: 0 is the bottom line, each step is a line or a space higher. */
    private fun rowOf(position: Int): Int = 149 - position * spacing / 2

    private fun read(image: BinaryImage): List<Int> {
        val staff = StaffFinder().find(image).single()
        return notationStaffReader.read(image, staff)
    }

    @Test
    fun readsFilledNotesOnLinesAndSpacesAnOctaveBelowWritten() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 100, rowOf(0), spacing)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(1), spacing)
        ScoreImageDrawing.quarterNote(image, 200, rowOf(4), spacing)

        assertEquals(listOf(31, 33, 38), read(image))
    }

    @Test
    fun readsNotesBelowTheStaffOnLedgerLines() {
        val image = blankStaffPage()
        ScoreImageDrawing.horizontalLine(image, rowOf(-2) - 1, 88, 112)
        ScoreImageDrawing.quarterNote(image, 100, rowOf(-2), spacing)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(-1), spacing)

        assertEquals(listOf(28, 29), read(image))
    }

    @Test
    fun readsHalfAndWholeNotes() {
        val image = blankStaffPage()
        ScoreImageDrawing.halfNote(image, 100, rowOf(2), spacing)
        ScoreImageDrawing.wholeNote(image, 160, rowOf(3), spacing)

        assertEquals(listOf(35, 36), read(image))
    }

    @Test
    fun aSolidShapeWithoutAStemIsNotANote() {
        val image = blankStaffPage()
        ScoreImageDrawing.filledNotehead(image, 60, rowOf(4), halfWidth = 9, halfHeight = 9)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(0), spacing)

        assertEquals(listOf(31), read(image))
    }

    @Test
    fun aFlatInTheKeySignatureLowersThatNoteEverywhere() {
        val image = blankStaffPage()
        ScoreImageDrawing.flat(image, 50, rowOf(2), spacing)
        ScoreImageDrawing.quarterNote(image, 120, rowOf(2), spacing)
        ScoreImageDrawing.quarterNote(image, 170, rowOf(0), spacing)

        assertEquals(listOf(34, 31), read(image))
    }

    @Test
    fun aSharpJustBeforeANoteRaisesIt() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 60, rowOf(0), spacing)
        ScoreImageDrawing.sharp(image, 120, rowOf(-1), spacing)
        ScoreImageDrawing.quarterNote(image, 140, rowOf(-1), spacing)

        assertEquals(listOf(31, 30), read(image))
    }

    @Test
    fun aChordIsReadAsItsLowestNote() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 100, rowOf(4), spacing)
        ScoreImageDrawing.quarterNote(image, 100, rowOf(0), spacing)

        assertEquals(listOf(31), read(image))
    }

    @Test
    fun anEmptyStaffHasNoNotes() {
        assertEquals(emptyList<Int>(), read(blankStaffPage()))
    }

    @Test
    fun beamedNotesAreReadOnceAndTheBeamIsNotANote() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 100, rowOf(0), spacing)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(1), spacing)
        val beamTop = rowOf(1) - spacing * 7 / 2
        (0 until 5).forEach { ScoreImageDrawing.horizontalLine(image, beamTop - 8 + it, 106, 157, thickness = 1) }

        assertEquals(listOf(31, 33), read(image))
    }

    @Test
    fun aStackedTimeSignatureIsNotANote() {
        val image = blankStaffPage()
        ScoreImageDrawing.verticalLine(image, 70, 101, 148, thickness = 3)
        ScoreImageDrawing.filledNotehead(image, 66, 116, halfWidth = 6, halfHeight = 4)
        ScoreImageDrawing.filledNotehead(image, 66, 140, halfWidth = 6, halfHeight = 4)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(0), spacing)

        assertEquals(listOf(31), read(image))
    }

    @Test
    fun aTrebleClefStaffIsReadTwoOctavesLowerSoItFitsTheBass() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 100, rowOf(0), spacing)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(2), spacing)
        val staff = StaffFinder().find(image).single()

        assertEquals(listOf(40, 43), notationStaffReader.read(image, staff, Clef.TREBLE))
    }

    @Test
    fun theClefAndAScannedKeySignatureSharpAreNotNotesAndTheSharpApplies() {
        val image = blankStaffPage()
        ScoreImageDrawing.bassClef(image, 26, 100, spacing)
        ScoreImageDrawing.blurredSharp(image, 76, rowOf(6), spacing)
        ScoreImageDrawing.quarterNote(image, 140, rowOf(6), spacing)
        ScoreImageDrawing.quarterNote(image, 190, rowOf(0), spacing)

        assertEquals(listOf(42, 31), read(image))
    }

    @Test
    fun aTimeSignatureIsNeitherNotesNorAKeySignature() {
        val image = blankStaffPage()
        ScoreImageDrawing.bassClef(image, 26, 100, spacing)
        ScoreImageDrawing.timeSignature(image, 72, 100, spacing)
        (0..8).forEach { position -> ScoreImageDrawing.quarterNote(image, 130 + position * 45, rowOf(position), spacing) }

        assertEquals(listOf(31, 33, 35, 36, 38, 40, 41, 43, 45), read(image))
    }

    @Test
    fun notesHangingBelowTheStaffAboveAreNotRead() {
        val image = blankStaffPage()
        ScoreImageDrawing.quarterNote(image, 100, rowOf(17), spacing)
        ScoreImageDrawing.quarterNote(image, 150, rowOf(0), spacing)

        assertEquals(listOf(31), read(image))
    }
}
