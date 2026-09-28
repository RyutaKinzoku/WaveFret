package com.example.wavefret.tuner

import android.Manifest
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.audio.LowPassFilteredPitchDetector
import com.example.wavefret.common.audio.YinPitchDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Shows a live tuner: listens to the microphone, identifies the
 * closest bass string, and updates a needle and cents readout in real
 * time while the screen is visible.
 *
 * This class is Android framework glue — lifecycle wiring and view
 * updates with no meaningful branching logic of its own — so per this
 * project's convention it's verified manually on-device rather than in
 * JUnit; the actual formatting decisions live in the tested
 * [TunerDisplayFormatter], and the hold/lock behavior in the tested
 * [TunerDisplayStabilizer].
 */
class TunerActivity : AppCompatActivity() {

    private lateinit var tunerStringNameTextView: TextView
    private lateinit var tunerCentsOffsetTextView: TextView
    private lateinit var tunerNeedleImageView: ImageView

    private val appContainer = AppContainer(this)
    private val tunerController = buildTunerController()
    private val tunerDisplayFormatter = TunerDisplayFormatter()

    private val displayStabilizer = TunerDisplayStabilizer(
        holdDurationMillis = DISPLAY_HOLD_DURATION_MILLIS,
        stringChangeConfirmationCount = STRING_CHANGE_CONFIRMATION_COUNT
    )

    private var tuningResultCollectionJob: Job? = null

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) startListening() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuner)
        tunerStringNameTextView = findViewById(R.id.tunerStringNameTextView)
        tunerCentsOffsetTextView = findViewById(R.id.tunerCentsOffsetTextView)
        tunerNeedleImageView = findViewById(R.id.tunerNeedleImageView)
    }

    override fun onResume() {
        super.onResume()
        if (appContainer.microphonePermissionManager.needsPermissionRequest()) {
            requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        tuningResultCollectionJob?.cancel()
        tuningResultCollectionJob = null
    }

    /**
     * Starts collecting tuning results and rendering them until paused.
     * Each raw reading is formatted, then passed through the stabilizer
     * so the screen holds a reading briefly and only switches strings
     * after several consecutive agreeing readings — otherwise a fast,
     * noisy stream of readings would be unreadable while tuning.
     */
    private fun startListening() {
        tuningResultCollectionJob = tunerController.tuningResultStream()
            .map(tunerDisplayFormatter::format)
            .map { displayStabilizer.stabilize(it, SystemClock.elapsedRealtime()) }
            .onEach(::renderDisplayState)
            .launchIn(lifecycleScope)
    }

    /**
     * Updates the on-screen needle, string name, and cents readout.
     *
     * @param displayState What the screen should currently show. Type: TunerDisplayState
     */
    private fun renderDisplayState(displayState: TunerDisplayState) {
        when (displayState) {
            is TunerDisplayState.Listening -> {
                tunerStringNameTextView.text = getString(R.string.tuner_listening_prompt)
                tunerCentsOffsetTextView.text = ""
                tunerNeedleImageView.rotation = 0f
            }
            is TunerDisplayState.Detected -> {
                tunerStringNameTextView.text = displayState.stringName
                tunerCentsOffsetTextView.text = displayState.centsOffsetLabel
                tunerCentsOffsetTextView.setTextColor(colorForStatus(displayState.status))
                tunerNeedleImageView.rotation = displayState.needleRotationDegrees
            }
        }
    }

    /**
     * Picks the readout color for a tuning status.
     *
     * @param status Whether the detected pitch is flat, in tune, or sharp. Type: TuningStatus
     * @return The resolved color for that status. Type: Int
     */
    private fun colorForStatus(status: TuningStatus): Int {
        val colorRes = if (status == TuningStatus.IN_TUNE) {
            R.color.tuning_status_in_tune
        } else {
            R.color.tuning_status_out_of_tune
        }
        return ContextCompat.getColor(this, colorRes)
    }

    /**
     * Assembles the tuning pipeline for this screen.
     *
     * @return A controller wired to the real microphone and standard bass tuning. Type: TunerController
     */
    private fun buildTunerController(): TunerController {
        return TunerController(
            audioSource = AudioRecordPcmSource(),
            silenceGate = SilenceGate(),
            pitchDetector = LowPassFilteredPitchDetector(
                lowPassFilter = LowPassFilter(cutoffHz = PITCH_ANALYSIS_CUTOFF_HZ),
                innerPitchDetector = YinPitchDetector()
            ),
            pitchSmoother = PitchSmoother(),
            tuningEvaluator = TuningEvaluator(BassTunings.STANDARD)
        )
    }

    private companion object {
        /** Bass fundamentals top out around 400 Hz; above ~1 kHz there is only noise. */
        const val PITCH_ANALYSIS_CUTOFF_HZ = 1000.0
        const val DISPLAY_HOLD_DURATION_MILLIS = 4000L
        const val STRING_CHANGE_CONFIRMATION_COUNT = 3
    }
}