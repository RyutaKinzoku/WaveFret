package com.example.wavefret.practice.songimport

/** Kinds of file a player can share into WaveFret to make a lesson. */
enum class SongFileFormat {
    /** MusicXML, plain (.musicxml/.xml) or compressed (.mxl). */
    MUSIC_XML,

    /** Guitar Pro 7 or later (.gp). */
    GUITAR_PRO,

    /** Standard MIDI file (.mid/.midi). */
    MIDI,

    /** Recorded or downloaded audio, transcribed into notes. */
    AUDIO,

    /** ASCII tab or WaveFret tab text. */
    TAB_TEXT,

    /** PDF or photo of sheet music: needs converting to MusicXML first. */
    SHEET_MUSIC_IMAGE,

    /** Anything else, including Guitar Pro 3-6 files. */
    UNSUPPORTED
}

/**
 * Works out what a shared file is from its name, MIME type and first bytes,
 * trusting content over names where the content is unambiguous.
 */
class SongFileFormatDetector {

    /**
     * @param fileName Display name of the file, possibly without extension. Type: String
     * @param mimeType Type reported by the sharing app, or null. Type: String?
     * @param leadingBytes The first bytes of the file (a few hundred are enough). Type: ByteArray
     * @return The detected format. Type: SongFileFormat
     */
    fun detect(fileName: String, mimeType: String?, leadingBytes: ByteArray): SongFileFormat {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val normalizedMimeType = mimeType?.lowercase().orEmpty()
        return when {
            startsWith(leadingBytes, MIDI_SIGNATURE) -> SongFileFormat.MIDI
            startsWith(leadingBytes, ZIP_SIGNATURE) && extension == "gp" -> SongFileFormat.GUITAR_PRO
            startsWith(leadingBytes, ZIP_SIGNATURE) && extension == "mxl" -> SongFileFormat.MUSIC_XML
            extension in OLD_GUITAR_PRO_EXTENSIONS -> SongFileFormat.UNSUPPORTED
            normalizedMimeType.startsWith("audio/") || extension in AUDIO_EXTENSIONS -> SongFileFormat.AUDIO
            normalizedMimeType == "application/pdf" || normalizedMimeType.startsWith("image/") ||
                extension in SHEET_MUSIC_IMAGE_EXTENSIONS -> SongFileFormat.SHEET_MUSIC_IMAGE
            looksLikeMusicXml(leadingBytes) || extension in MUSIC_XML_EXTENSIONS -> SongFileFormat.MUSIC_XML
            normalizedMimeType.startsWith("text/") || extension in TEXT_EXTENSIONS -> SongFileFormat.TAB_TEXT
            else -> SongFileFormat.UNSUPPORTED
        }
    }

    /**
     * @param leadingBytes First bytes of the file. Type: ByteArray
     * @return True if the text mentions a MusicXML root element. Type: Boolean
     */
    private fun looksLikeMusicXml(leadingBytes: ByteArray): Boolean {
        val leadingText = leadingBytes.toString(Charsets.UTF_8)
        return "<score-partwise" in leadingText || "<score-timewise" in leadingText
    }

    /**
     * @param bytes Bytes to check. Type: ByteArray
     * @param signature Expected prefix. Type: ByteArray
     * @return True if bytes begin with signature. Type: Boolean
     */
    private fun startsWith(bytes: ByteArray, signature: ByteArray): Boolean {
        return bytes.size >= signature.size && signature.indices.all { bytes[it] == signature[it] }
    }

    private companion object {
        val MIDI_SIGNATURE = "MThd".toByteArray(Charsets.US_ASCII)
        val ZIP_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val OLD_GUITAR_PRO_EXTENSIONS = setOf("gp3", "gp4", "gp5", "gpx")
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "oga", "opus", "flac", "3gp", "amr", "webm")
        val SHEET_MUSIC_IMAGE_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png", "webp", "heic")
        val MUSIC_XML_EXTENSIONS = setOf("musicxml", "xml", "mxl")
        val TEXT_EXTENSIONS = setOf("txt", "tab")
    }
}
