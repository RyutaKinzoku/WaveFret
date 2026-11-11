package com.example.wavefret.practice.songimport

import org.junit.Assert.assertEquals
import org.junit.Test

class SongFileFormatDetectorTest {

    private val detector = SongFileFormatDetector()
    private val zipBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0)
    private val midiBytes = "MThd".toByteArray() + byteArrayOf(0, 0, 0, 6)
    private val xmlBytes = "<?xml version=\"1.0\"?><score-partwise/>".toByteArray()

    @Test
    fun midiIsRecognisedByItsHeaderWhateverTheName() {
        assertEquals(SongFileFormat.MIDI, detector.detect("song.bin", "application/octet-stream", midiBytes))
    }

    @Test
    fun guitarProSevenFileIsAZipNamedGp() {
        assertEquals(SongFileFormat.GUITAR_PRO, detector.detect("riff.gp", null, zipBytes))
    }

    @Test
    fun compressedMusicXmlIsAZipNamedMxl() {
        assertEquals(SongFileFormat.MUSIC_XML, detector.detect("score.mxl", null, zipBytes))
    }

    @Test
    fun plainMusicXmlIsRecognisedByContent() {
        assertEquals(SongFileFormat.MUSIC_XML, detector.detect("export", "text/plain", xmlBytes))
    }

    @Test
    fun audioIsRecognisedByMimeType() {
        assertEquals(SongFileFormat.AUDIO, detector.detect("track", "audio/mpeg", byteArrayOf(1, 2, 3)))
    }

    @Test
    fun audioIsRecognisedByExtension() {
        assertEquals(SongFileFormat.AUDIO, detector.detect("Song.WAV", null, byteArrayOf(1, 2, 3)))
    }

    @Test
    fun pdfAndPhotosAreSheetMusicImagesThatNeedConverting() {
        assertEquals(SongFileFormat.SHEET_MUSIC_IMAGE, detector.detect("partitura.pdf", "application/pdf", "%PDF-1.7".toByteArray()))
        assertEquals(SongFileFormat.SHEET_MUSIC_IMAGE, detector.detect("photo.jpg", "image/jpeg", byteArrayOf(1)))
    }

    @Test
    fun plainTextIsTabText() {
        assertEquals(SongFileFormat.TAB_TEXT, detector.detect("riff.txt", "text/plain", "G|--3--|".toByteArray()))
    }

    @Test
    fun olderGuitarProFormatsAreReportedAsUnsupported() {
        assertEquals(SongFileFormat.UNSUPPORTED, detector.detect("old.gp5", null, byteArrayOf(0x18, 0x46)))
    }
}
