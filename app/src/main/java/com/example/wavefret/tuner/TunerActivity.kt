package com.example.wavefret.tuner

import android.Manifest
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.audio.LowPassFilteredPitchDetector
import com.example.wavefret.common.audio.SilenceGate
import com.example.wavefret.common.audio.YinPitchDetector
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Live tuner: a gauge with an in-tune zone, the detected string
 * highlighted among E A D G, and plain-language instructions ("too low,
 * tighten the string"). Listens only while visible and keeps the screen on.
 *
 * Android framework glue — formatting lives in the tested
 * [TunerDisplayFormatter] and hold/lock behaviour in the tested
 * [TunerDisplayStabilizer] — so it is verified manually on-device.
 */
class TunerActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var tunerGaugeView: TunerGaugeView
    private lateinit var stringNameTextView: TextView
    private lateinit var centsOffsetTextView: TextView
    private lateinit var statusTextView: TextView
    private val stringBadgesByName = mutableMapOf<String, TextView>()

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
    ) { isGranted ->
        if (isGranted) startListening() else showMicrophonePermissionNeeded(rootView, R.string.tuner_permission_needed)
    }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tuner)
        rootView = findViewById(R.id.tunerRoot)
        tunerGaugeView = findViewById(R.id.tunerGaugeView)
        stringNameTextView = findViewById(R.id.tunerStringNameTextView)
        centsOffsetTextView = findViewById(R.id.tunerCentsOffsetTextView)
        statusTextView = findViewById(R.id.tunerStatusTextView)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.tuner_title)
        topAppBar.setSubtitle(R.string.tuner_subtitle)
        topAppBar.setNavigationOnClickListener { finish() }
        rootView.applySystemBarPadding(applyTop = true, applyBottom = true)

        buildStringBadges(findViewById(R.id.tunerStringsRow))
        renderDisplayState(TunerDisplayState.Listening)
    }

    /**
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (appContainer.microphonePermissionManager.needsPermissionRequest()) {
            requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListening()
        }
    }

    /**
     * Releases the microphone as soon as the tuner is no longer visible.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tuningResultCollectionJob?.cancel()
        tuningResultCollectionJob = null
    }

    /**
     * Creates one round badge per string of the tuning, low to high.
     *
     * @param stringsRow Row the badges go in. Type: LinearLayout
     * @return Unit
     */
    private fun buildStringBadges(stringsRow: LinearLayout) {
        val layoutInflater = LayoutInflater.from(this)
        BassTunings.STANDARD.strings.forEach { bassString ->
            val stringBadge = layoutInflater.inflate(R.layout.item_tuner_string, stringsRow, false) as TextView
            stringBadge.text = bassString.name
            stringsRow.addView(stringBadge)
            stringBadgesByName[bassString.name] = stringBadge
        }
    }

    /**
     * Starts collecting tuning results until paused. Each raw reading is
     * formatted, then stabilized so the display holds briefly and only
     * switches strings after several agreeing readings.
     *
     * @return Unit
     */
    private fun startListening() {
        if (tuningResultCollectionJob != null) return
        tuningResultCollectionJob = tunerController.tuningResultStream()
            .map(tunerDisplayFormatter::format)
            .map { displayStabilizer.stabilize(it, SystemClock.elapsedRealtime()) }
            .onEach(::renderDisplayState)
            .launchIn(lifecycleScope)
    }

    /**
     * @param displayState What the screen should currently show. Type: TunerDisplayState
     * @return Unit
     */
    private fun renderDisplayState(displayState: TunerDisplayState) {
        when (displayState) {
            is TunerDisplayState.Listening -> {
                stringNameTextView.setText(R.string.tuner_no_note)
                centsOffsetTextView.text = ""
                statusTextView.setText(R.string.tuner_listening_prompt)
                statusTextView.setTextColor(colorFor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                tunerGaugeView.showReading(0f, isInTune = false, isActive = false)
                highlightString(null)
            }
            is TunerDisplayState.Detected -> {
                val isInTune = displayState.status == TuningStatus.IN_TUNE
                stringNameTextView.text = displayState.stringName
                centsOffsetTextView.text = displayState.centsOffsetLabel
                statusTextView.setText(statusTextFor(displayState.status))
                statusTextView.setTextColor(
                    colorFor(
                        if (isInTune) com.google.android.material.R.attr.colorSecondary
                        else androidx.appcompat.R.attr.colorPrimary
                    )
                )
                tunerGaugeView.showReading(displayState.needleRotationDegrees, isInTune, isActive = true)
                tunerGaugeView.contentDescription = getString(
                    R.string.tuner_gauge_description, getString(statusTextFor(displayState.status))
                )
                highlightString(displayState.stringName)
            }
        }
    }

    /**
     * @param detectedStringName Name of the string to highlight, or null for none. Type: String?
     * @return Unit
     */
    private fun highlightString(detectedStringName: String?) {
        stringBadgesByName.forEach { (stringName, stringBadge) ->
            val isDetected = stringName == detectedStringName
            stringBadge.isSelected = isDetected
            stringBadge.setTextColor(
                colorFor(
                    if (isDetected) com.google.android.material.R.attr.colorOnPrimary
                    else com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
        }
    }

    /**
     * @param status Whether the string is flat, in tune or sharp. Type: TuningStatus
     * @return String resource telling the player what to do. Type: Int
     */
    private fun statusTextFor(status: TuningStatus): Int = when (status) {
        TuningStatus.IN_TUNE -> R.string.tuner_status_in_tune
        TuningStatus.FLAT -> R.string.tuner_status_flat
        TuningStatus.SHARP -> R.string.tuner_status_sharp
    }

    /**
     * @param colorAttribute Theme colour role. Type: Int
     * @return The resolved colour. Type: Int
     */
    private fun colorFor(colorAttribute: Int): Int = MaterialColors.getColor(rootView, colorAttribute)

    /**
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
