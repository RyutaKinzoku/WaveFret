package com.example.wavefret.practice.songimport

import com.example.wavefret.common.storage.ZipArchiveReader

/**
 * Gets the MusicXML document text out of a file's bytes, whether it is a
 * plain `.musicxml`/`.xml` file or a compressed `.mxl` archive. An archive's
 * score is the root file named in `META-INF/container.xml`, falling back to
 * its first `.xml`/`.musicxml` entry outside `META-INF`.
 *
 * @property zipArchiveReader Opens .mxl archives. Type: ZipArchiveReader
 */
class MusicXmlFileReader(private val zipArchiveReader: ZipArchiveReader = ZipArchiveReader()) {

    /**
     * @param fileBytes Full contents of the chosen file. Type: ByteArray
     * @return The MusicXML document text. Type: String
     * @throws IllegalArgumentException If an archive holds no score.
     */
    fun readMusicXmlText(fileBytes: ByteArray): String {
        if (!isZipArchive(fileBytes)) return fileBytes.toString(Charsets.UTF_8)
        val archiveEntries = readArchiveEntries(fileBytes)
        val rootFilePath = archiveEntries[CONTAINER_ENTRY_PATH]
            ?.let { containerXml -> ROOT_FILE_PATTERN.find(containerXml)?.groupValues?.get(1) }
        val scoreText = rootFilePath?.let(archiveEntries::get)
            ?: archiveEntries.entries.firstOrNull { (entryPath, _) -> isScoreEntry(entryPath) }?.value
        return requireNotNull(scoreText) { "The .mxl archive contains no MusicXML score" }
    }

    /**
     * @param fileBytes File contents. Type: ByteArray
     * @return True if the bytes start with the ZIP signature. Type: Boolean
     */
    private fun isZipArchive(fileBytes: ByteArray): Boolean {
        return fileBytes.size >= ZIP_SIGNATURE.size && ZIP_SIGNATURE.indices.all { fileBytes[it] == ZIP_SIGNATURE[it] }
    }

    /**
     * @param archiveBytes ZIP archive contents. Type: ByteArray
     * @return Each file entry's text by path, in archive order. Type: Map<String, String>
     */
    private fun readArchiveEntries(archiveBytes: ByteArray): Map<String, String> {
        return zipArchiveReader.readEntries(archiveBytes).mapValues { (_, entryBytes) -> entryBytes.toString(Charsets.UTF_8) }
    }

    /**
     * @param entryPath Path of an archive entry. Type: String
     * @return True if it looks like a score rather than metadata. Type: Boolean
     */
    private fun isScoreEntry(entryPath: String): Boolean {
        return !entryPath.startsWith(META_INF_FOLDER) &&
            (entryPath.endsWith(".xml") || entryPath.endsWith(".musicxml"))
    }

    private companion object {
        const val CONTAINER_ENTRY_PATH = "META-INF/container.xml"
        const val META_INF_FOLDER = "META-INF/"
        val ZIP_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val ROOT_FILE_PATTERN = Regex("<rootfile[^>]*full-path=\"([^\"]+)\"")
    }
}
