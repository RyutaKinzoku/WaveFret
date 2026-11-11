package com.example.wavefret.practice.songimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MusicXmlFileReaderTest {

    private val musicXmlFileReader = MusicXmlFileReader()

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zipOutputStream ->
            entries.forEach { (entryName, entryText) ->
                zipOutputStream.putNextEntry(ZipEntry(entryName))
                zipOutputStream.write(entryText.toByteArray())
                zipOutputStream.closeEntry()
            }
        }
        return zipBytes.toByteArray()
    }

    @Test
    fun uncompressedFileIsReadAsUtf8Text() {
        val musicXml = "<score-partwise>é</score-partwise>"

        assertEquals(musicXml, musicXmlFileReader.readMusicXmlText(musicXml.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun compressedFileYieldsTheRootFileNamedInTheContainer() {
        val containerXml = """
            <container><rootfiles><rootfile full-path="score/song.xml"/></rootfiles></container>
        """.trimIndent()
        val mxlBytes = zipOf(
            "META-INF/container.xml" to containerXml,
            "other.xml" to "<wrong/>",
            "score/song.xml" to "<score-partwise/>"
        )

        assertEquals("<score-partwise/>", musicXmlFileReader.readMusicXmlText(mxlBytes))
    }

    @Test
    fun compressedFileWithoutContainerYieldsTheFirstScoreEntry() {
        val mxlBytes = zipOf("META-INF/other.txt" to "x", "song.musicxml" to "<score-partwise/>")

        assertEquals("<score-partwise/>", musicXmlFileReader.readMusicXmlText(mxlBytes))
    }

    @Test
    fun compressedFileWithoutAnyScoreIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            musicXmlFileReader.readMusicXmlText(zipOf("readme.txt" to "hello"))
        }
    }
}
