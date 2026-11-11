package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.TabTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class SongFileImporterTest {

    private val tuning = BassTunings.STANDARD
    private val positionFinder = FretboardPositionFinder(tuning)
    private val songFileImporter = SongFileImporter(
        songFileFormatDetector = SongFileFormatDetector(),
        songImporter = SongImporter(AsciiTabParser(tuning), TabTextParser(tuning)),
        musicXmlFileReader = MusicXmlFileReader(),
        musicXmlBassParser = MusicXmlBassParser(tuning, positionFinder),
        midiFileParser = MidiFileParser(),
        guitarProFileParser = GuitarProFileParser(),
        noteEventsLessonBuilder = NoteEventsLessonBuilder(tuning, positionFinder)
    )

    private val oneNoteMidi: ByteArray = ByteArrayOutputStream().apply {
        write("MThd".toByteArray()); write(byteArrayOf(0, 0, 0, 6, 0, 0, 0, 1, 0x01, 0xE0.toByte()))
        val track = byteArrayOf(0, 0x90.toByte(), 33, 100, 0x83.toByte(), 0x60, 0x80.toByte(), 33, 0, 0, 0xFF.toByte(), 0x2F, 0)
        write("MTrk".toByteArray()); write(byteArrayOf(0, 0, 0, track.size.toByte())); write(track)
    }.toByteArray()

    @Test
    fun midiFileBecomesATimedLessonNamedAfterTheFile() {
        val outcome = songFileImporter.import("Billie Jean.mid", "audio/midi", oneNoteMidi, titleOverride = "")

        val lesson = (outcome as SongImportOutcome.Imported).exercise
        assertEquals("Billie Jean", lesson.title)
        assertEquals(listOf(TabNote(1, 0)), lesson.notes)
        assertEquals(listOf(0L), lesson.noteOnsetsMillis)
    }

    @Test
    fun titleOverrideWins() {
        val outcome = songFileImporter.import("x.mid", null, oneNoteMidi, titleOverride = "My groove")

        assertEquals("My groove", (outcome as SongImportOutcome.Imported).exercise.title)
    }

    @Test
    fun asciiTabTextFileIsImported() {
        val outcome = songFileImporter.import("riff.txt", "text/plain", "G|---|\nD|---|\nA|-2-|\nE|---|".toByteArray(), "")

        assertEquals(listOf(TabNote(1, 2)), (outcome as SongImportOutcome.Imported).exercise.notes)
    }

    @Test
    fun audioIsHandedOnForTranscription() {
        val outcome = songFileImporter.import("song.mp3", "audio/mpeg", byteArrayOf(1, 2, 3), "")

        assertEquals(SongImportOutcome.NeedsAudioTranscription, outcome)
    }

    @Test
    fun pdfOrPhotoExplainsHowToConvertIt() {
        val outcome = songFileImporter.import("partitura.pdf", "application/pdf", "%PDF".toByteArray(), "")

        assertEquals(SongImportOutcome.SheetMusicImageNotSupported, outcome)
    }

    @Test
    fun unreadableFileGivesAReason() {
        val outcome = songFileImporter.import("broken.mid", "audio/midi", "MThd-broken".toByteArray(), "")

        assertTrue((outcome as SongImportOutcome.Failed).reason.isNotBlank())
    }

    @Test
    fun oldGuitarProFilesAreExplained() {
        val outcome = songFileImporter.import("song.gp5", null, byteArrayOf(0x18), "")

        assertEquals(SongImportOutcome.Failed(SongFileImporter.OLD_GUITAR_PRO_REASON), outcome)
    }
}
