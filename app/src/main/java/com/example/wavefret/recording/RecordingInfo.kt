package com.example.wavefret.recording

/**
 * Immutable description of a single recorded take, audio or video.
 *
 * @property filePath Absolute path to the recording file on disk. Type: String
 * @property fileName File name only, without directory path. Type: String
 * @property lastModifiedMillis Last-modified timestamp of the file, in epoch milliseconds. Type: Long
 * @property durationMillis Duration of the recording, in milliseconds. Type: Long
 * @property mediaType Whether the take is audio only or video. Type: RecordingMediaType
 */
data class RecordingInfo(
    val filePath: String,
    val fileName: String,
    val lastModifiedMillis: Long,
    val durationMillis: Long,
    val mediaType: RecordingMediaType = RecordingMediaType.AUDIO
)