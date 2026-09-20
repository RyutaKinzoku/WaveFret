package com.example.wavefret.recording

/**
 * Immutable description of a single recorded audio file.
 *
 * @property filePath Absolute path to the recording file on disk. Type: String
 * @property fileName File name only, without directory path. Type: String
 * @property lastModifiedMillis Last-modified timestamp of the file, in epoch milliseconds. Type: Long
 */
data class RecordingInfo(
    val filePath: String,
    val fileName: String,
    val lastModifiedMillis: Long
)