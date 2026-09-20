package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingSessionControllerTest {

    private fun buildController(
        audioRecorder: FakeAudioRecorder = FakeAudioRecorder(),
        directoryPath: String = "/fake/dir"
    ): Pair<RecordingSessionController, FakeAudioRecorder> {
        val controller = RecordingSessionController(
            audioRecorder = audioRecorder,
            fileNamer = RecordingFileNamer(FakeClock(fixedTimeMillis = 123L)),
            directoryProvider = FakeRecordingDirectoryProvider(directoryPath)
        )
        return controller to audioRecorder
    }

    @Test
    fun startNewRecordingDelegatesToAudioRecorderWithPathBuiltFromDirectoryAndFileName() {
        val (controller, audioRecorder) = buildController(directoryPath = "/fake/dir")
        controller.startNewRecording()
        assertEquals("/fake/dir/recording_123.m4a", audioRecorder.lastStartedFilePath)
    }

    @Test
    fun stopCurrentRecordingStopsAndReleasesTheAudioRecorder() {
        val (controller, audioRecorder) = buildController()
        controller.startNewRecording()
        controller.stopCurrentRecording()
        assertEquals(1, audioRecorder.stopRecordingCallCount)
        assertEquals(1, audioRecorder.releaseRecorderCallCount)
    }

    @Test
    fun stopCurrentRecordingReturnsThePathThatWasRecorded() {
        val (controller, _) = buildController(directoryPath = "/fake/dir")
        controller.startNewRecording()
        val result = controller.stopCurrentRecording()
        assertEquals("/fake/dir/recording_123.m4a", result)
    }

    @Test
    fun stopCurrentRecordingReturnsNullWhenNothingWasRecording() {
        val (controller, _) = buildController()
        assertNull(controller.stopCurrentRecording())
    }
}