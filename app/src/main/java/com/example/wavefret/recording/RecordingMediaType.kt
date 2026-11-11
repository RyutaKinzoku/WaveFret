package com.example.wavefret.recording

/**
 * Whether a take is sound only or sound with video.
 *
 * @property fileExtension Extension the take is saved with. Type: String
 * @property shareMimeType Type announced to other apps when sharing. Type: String
 */
enum class RecordingMediaType(val fileExtension: String, val shareMimeType: String) {
    AUDIO("m4a", "audio/mp4"),
    VIDEO("mp4", "video/mp4");

    companion object {
        /**
         * @param fileName Name of a file in the recordings folder. Type: String
         * @return The take's type, or null if the file isn't a take. Type: RecordingMediaType?
         */
        fun forFileName(fileName: String): RecordingMediaType? {
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return entries.firstOrNull { it.fileExtension == extension }
        }
    }
}
