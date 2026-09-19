package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingUiControllerTest {

    @Test
    fun `initial state is idle with record enabled and stop disabled`() {
        val controller = RecordingUiController()
        assertEquals(RecordingState.IDLE, controller.currentState())
        assertTrue(controller.isRecordButtonEnabled())
        assertFalse(controller.isStopButtonEnabled())
    }

    @Test
    fun `onRecordClicked transitions to recording`() {
        val controller = RecordingUiController()
        controller.onRecordClicked()
        assertEquals(RecordingState.RECORDING, controller.currentState())
        assertFalse(controller.isRecordButtonEnabled())
        assertTrue(controller.isStopButtonEnabled())
    }

    @Test
    fun `onStopClicked transitions back to idle`() {
        val controller = RecordingUiController()
        controller.onRecordClicked()
        controller.onStopClicked()
        assertEquals(RecordingState.IDLE, controller.currentState())
        assertTrue(controller.isRecordButtonEnabled())
        assertFalse(controller.isStopButtonEnabled())
    }

    @Test
    fun `onRecordClicked while already recording has no effect`() {
        val controller = RecordingUiController()
        controller.onRecordClicked()
        controller.onRecordClicked()
        assertEquals(RecordingState.RECORDING, controller.currentState())
    }

    @Test
    fun `onStopClicked while idle has no effect`() {
        val controller = RecordingUiController()
        controller.onStopClicked()
        assertEquals(RecordingState.IDLE, controller.currentState())
    }

    @Test
    fun `initial button label is Record`() {
        val controller = RecordingUiController()
        assertEquals("Record", controller.buttonLabel())
    }

    @Test
    fun `button label is Stop while recording`() {
        val controller = RecordingUiController()
        controller.onButtonClicked()
        assertEquals("Stop", controller.buttonLabel())
    }

    @Test
    fun `onButtonClicked toggles idle to recording`() {
        val controller = RecordingUiController()
        controller.onButtonClicked()
        assertEquals(RecordingState.RECORDING, controller.currentState())
    }

    @Test
    fun `onButtonClicked toggles recording back to idle`() {
        val controller = RecordingUiController()
        controller.onButtonClicked()
        controller.onButtonClicked()
        assertEquals(RecordingState.IDLE, controller.currentState())
    }
}