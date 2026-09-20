package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingUiControllerTest {

    @Test
    fun initialStateIsIdleWithRecordEnabledAndStopDisabled() {
        val controller = RecordingUiController()
        assertEquals(RecordingState.IDLE, controller.currentState())
        assertTrue(controller.isRecordButtonEnabled())
        assertFalse(controller.isStopButtonEnabled())
    }

    @Test
    fun onRecordClickedTransitionsToRecording() {
        val controller = RecordingUiController()
        controller.onRecordClicked()
        assertEquals(RecordingState.RECORDING, controller.currentState())
        assertFalse(controller.isRecordButtonEnabled())
        assertTrue(controller.isStopButtonEnabled())
    }

    @Test
    fun onStopClickedTransitionsBackToIdle() {
        val controller = RecordingUiController()
        controller.onRecordClicked()
        controller.onStopClicked()
        assertEquals(RecordingState.IDLE, controller.currentState())
        assertTrue(controller.isRecordButtonEnabled())
        assertFalse(controller.isStopButtonEnabled())
    }

    @Test
    fun onRecordClickedWhileAlreadyRecordingHasNoEffect() {
        val controller = RecordingUiController()
        controller.onRecordClicked()
        controller.onRecordClicked()
        assertEquals(RecordingState.RECORDING, controller.currentState())
    }

    @Test
    fun onStopClickedWhileIdleHasNoEffect() {
        val controller = RecordingUiController()
        controller.onStopClicked()
        assertEquals(RecordingState.IDLE, controller.currentState())
    }

    @Test
    fun initialButtonLabelIsRecord() {
        val controller = RecordingUiController()
        assertEquals("Record", controller.toggleRecordingButtonLabel())
    }

    @Test
    fun buttonLabelIsStopWhileRecording() {
        val controller = RecordingUiController()
        controller.onToggleRecordingClicked()
        assertEquals("Stop", controller.toggleRecordingButtonLabel())
    }

    @Test
    fun onToggleRecordingClickedTogglesIdleToRecording() {
        val controller = RecordingUiController()
        controller.onToggleRecordingClicked()
        assertEquals(RecordingState.RECORDING, controller.currentState())
    }

    @Test
    fun onToggleRecordingClickedTogglesRecordingBackToIdle() {
        val controller = RecordingUiController()
        controller.onToggleRecordingClicked()
        controller.onToggleRecordingClicked()
        assertEquals(RecordingState.IDLE, controller.currentState())
    }
}