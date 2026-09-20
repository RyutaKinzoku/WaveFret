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