package com.example.wavefret.common.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipArchiveReaderTest {

    private val zipArchiveReader = ZipArchiveReader()

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zip ->
            entries.forEach { (name, text) -> zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
        }
        return zipBytes.toByteArray()
    }

    @Test
    fun readsEveryFileEntryByPath() {
        val entries = zipArchiveReader.readEntries(zipOf("a.txt" to "first", "dir/b.xml" to "<b/>"))

        assertEquals(mapOf("a.txt" to "first", "dir/b.xml" to "<b/>"), entries.mapValues { it.value.toString(Charsets.UTF_8) })
    }

    @Test
    fun trustsTheCentralDirectoryWhenALocalSizeIsWrong() {
        // Guitar Pro writes data descriptors whose sizes disagree with the data; the central directory is right.
        val zipBytes = zipOf("Content/score.gpif" to "<GPIF/>".repeat(50))
        val descriptorIndex = (0 until zipBytes.size - 16).first {
            zipBytes[it] == 0x50.toByte() && zipBytes[it + 1] == 0x4B.toByte() &&
                zipBytes[it + 2] == 0x07.toByte() && zipBytes[it + 3] == 0x08.toByte()
        }
        zipBytes[descriptorIndex + 12] = (zipBytes[descriptorIndex + 12] + 7).toByte()

        val entries = zipArchiveReader.readEntries(zipBytes)

        assertEquals("<GPIF/>".repeat(50), entries.getValue("Content/score.gpif").toString(Charsets.UTF_8))
    }

    @Test
    fun bytesThatAreNotAZipAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { zipArchiveReader.readEntries("plain text".toByteArray()) }
    }
}
