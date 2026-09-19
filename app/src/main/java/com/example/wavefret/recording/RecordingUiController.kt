package com.example.wavefret.recording

/**
 * Holds and transitions the UI state for the recording screen.
 * Single responsibility: decide what state the recording UI should be in,
 * independent of any Android View code, so it can be unit tested directly.
 */
class RecordingUiController {

    private var state: RecordingState = RecordingState.IDLE

    /**
     * @return Current recording state. Type: RecordingState
     */
    fun currentState(): RecordingState = state

    /**
     * Called when the user taps "Record". Transitions to RECORDING
     * only if currently IDLE; has no effect if already recording.
     *
     * @return Unit
     */
    fun onRecordClicked() {
        if (state == RecordingState.IDLE) {
            state = RecordingState.RECORDING
        }
    }

    /**
     * Called when the user taps "Stop". Transitions to IDLE
     * only if currently RECORDING; has no effect if already idle.
     *
     * @return Unit
     */
    fun onStopClicked() {
        if (state == RecordingState.RECORDING) {
            state = RecordingState.IDLE
        }
    }

    /**
     * @return True if the Record button should be enabled. Type: Boolean
     */
    fun isRecordButtonEnabled(): Boolean = state == RecordingState.IDLE

    /**
     * @return True if the Stop button should be enabled. Type: Boolean
     */
    fun isStopButtonEnabled(): Boolean = state == RecordingState.RECORDING

    /**
     * @return Human-readable status text describing the current state. Type: String
     */
    fun statusText(): String = when (state) {
        RecordingState.IDLE -> "Ready to record"
        RecordingState.RECORDING -> "Recording..."
    }

    /**
     * Handles a single toggle button tap: starts recording if idle,
     * stops recording if currently recording.
     *
     * @return Unit
     */
    fun onToggleRecordingClicked() {
        when (state) {
            RecordingState.IDLE -> onRecordClicked()
            RecordingState.RECORDING -> onStopClicked()
        }
    }

    /**
     * @return Label the toggle button should display for the current state. Type: String
     */
    fun toggleRecordingButtonLabel(): String = when (state) {
        RecordingState.IDLE -> "Record"
        RecordingState.RECORDING -> "Stop"
    }
}