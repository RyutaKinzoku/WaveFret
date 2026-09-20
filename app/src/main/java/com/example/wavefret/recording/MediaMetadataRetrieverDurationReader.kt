package com.example.wavefret.recording

import android.media.MediaMetadataRetriever
import android.util.Log

/** Real AudioDurationReader implementation backed by android.media.MediaMetadataRetriever. */
class MediaMetadataRetrieverDurationReader : AudioDurationReader {

    /**
     * @param filePath Absolute path of the audio file to inspect. Type: String
     * @return Duration of the file in milliseconds, or 0 if it could not be read. Type: Long
     */
    override fun readDurationMillis(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationText?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read duration for $filePath", e)
            0L
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val TAG = "MediaMetadataRetrieverDurationReader"
    }
}