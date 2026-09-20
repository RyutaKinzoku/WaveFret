package com.example.wavefret.recording

import com.example.wavefret.common.time.Clock
import com.example.wavefret.common.time.DateFormatter

/** Fake Clock returning a fixed time, for deterministic file-naming tests. */
class FakeClock(private val fixedTimeMillis: Long) : Clock {
    override fun currentTimeMillis(): Long = fixedTimeMillis
}

/** Fake AudioRecorder that records what it was asked to do, without touching real hardware. */
class FakeAudioRecorder : AudioRecorder {
    var lastStartedFilePath: String? = null
        private set
    var stopRecordingCallCount = 0
        private set
    var releaseRecorderCallCount = 0
        private set

    override fun startRecordingToFile(outputFilePath: String) {
        lastStartedFilePath = outputFilePath
    }

    override fun stopRecording() {
        stopRecordingCallCount++
    }

    override fun releaseRecorder() {
        releaseRecorderCallCount++
    }
}

/** Fake RecordingDirectoryProvider returning a fixed path, avoiding a real Context dependency. */
class FakeRecordingDirectoryProvider(private val fixedPath: String) : RecordingDirectoryProvider {
    override fun recordingsDirectoryPath(): String = fixedPath
}

/** Fake DateFormatter returning a fixed string, for deterministic display-logic tests. */
class FakeDateFormatter(private val fixedOutput: String) : DateFormatter {
    override fun format(epochMillis: Long): String = fixedOutput
}

/** Fake AudioPlayer that records calls and lets tests simulate natural playback completion. */
class FakeAudioPlayer : AudioPlayer {
    var lastPlayedFilePath: String? = null
        private set
    var stopPlaybackCallCount = 0
        private set
    var releasePlayerCallCount = 0
        private set
    private var lastCompletionCallback: (() -> Unit)? = null

    override fun playFile(filePath: String, onCompleted: () -> Unit) {
        lastPlayedFilePath = filePath
        lastCompletionCallback = onCompleted
    }

    override fun stopPlayback() {
        stopPlaybackCallCount++
    }

    override fun releasePlayer() {
        releasePlayerCallCount++
    }

    /** Simulates the platform player reaching the end of the file on its own. */
    fun simulatePlaybackCompleted() {
        lastCompletionCallback?.invoke()
    }
}

/** Fake AudioDurationReader returning a fixed duration for every file, for deterministic tests. */
class FakeAudioDurationReader(private val fixedDurationMillis: Long = 0L) : AudioDurationReader {
    override fun readDurationMillis(filePath: String): Long = fixedDurationMillis
}