package com.example.wavefret.tuner

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
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
import com.example.wavefret.common.audio.AdaptiveSilenceGate
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.DecimatingPitchDetector
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.audio.LowPassFilteredPitchDetector
import com.example.wavefret.common.audio.SlidingWindowAudioSource
import com.example.wavefret.common.audio.YinPitchDetector
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Guided tuner. Strings are tuned one at a time: automatically from the
 * lowest string up, or the string the player taps. The needle follows the
 * string about 40 times a second; when it has stayed in tune for a moment
 * a chime plays, the string is marked tuned and skipped from then on, and
 * the tuner moves to the next one. Tapping a tuned string tunes it again;
 * "Start over" or leaving the tuner forgets all marks.
 *
 * Android framework glue — every decision lives in the tested
 * [GuidedTuner] and [TunerDisplayFormatter] — verified on-device.
 */
class TunerActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var tunerGaugeView: TunerGaugeView
    private lateinit var stringNameTextView: TextView
    private lateinit var centsOffsetTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var modeTextView: TextView
    private val stringButtons = mutableListOf<MaterialButton>()

    private val appContainer = AppContainer(this)
    private val tuning = BassTunings.STANDARD
    private val guidedTuner = GuidedTuner(tuning)
    private val tunerDisplayFormatter = TunerDisplayFormatter()
    private val tunerController = TunerController(
        audioSource = SlidingWindowAudioSource(
            AudioRecordPcmSource(samplesPerBufferCount = CAPTURE_SAMPLES), WINDOW_SAMPLES, HOP_SAMPLES
        ),
        signalGate = AdaptiveSilenceGate(),
        pitchDetector = LowPassFilteredPitchDetector(
            lowPassFilter = LowPassFilter(cutoffHz = PITCH_ANALYSIS_CUTOFF_HZ),
            innerPitchDetector = DecimatingPitchDetector(YinPitchDetector())
        )
    )
    private var tuneChimePlayer: TuneChimePlayer? = null
    private var readingCollectionJob: Job? = null

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
        modeTextView = findViewById(R.id.tunerModeTextView)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.tuner_title)
        topAppBar.setSubtitle(R.string.tuner_subtitle)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.inflateMenu(R.menu.menu_tuner)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            val isStartOver = menuItem.itemId == R.id.tunerStartOverMenuItem
            if (isStartOver) {
                guidedTuner.resetAll()
                render(guidedTuner.currentState())
            }
            isStartOver
        }
        rootView.applySystemBarPadding(applyTop = true, applyBottom = true)

        buildStringButtons(findViewById(R.id.tunerStringsRow))
        render(guidedTuner.currentState())
    }

    /**
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tuneChimePlayer = TuneChimePlayer()
        if (appContainer.microphonePermissionManager.needsPermissionRequest()) {
            requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListening()
        }
    }

    /**
     * Releases the microphone and the chime as soon as the tuner is hidden.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        readingCollectionJob?.cancel()
        readingCollectionJob = null
        tuneChimePlayer?.release()
        tuneChimePlayer = null
    }

    /**
     * @param stringsRow Row the string buttons go in. Type: LinearLayout
     * @return Unit
     */
    private fun buildStringButtons(stringsRow: LinearLayout) {
        val layoutInflater = LayoutInflater.from(this)
        tuning.strings.forEachIndexed { stringIndex, bassString ->
            val stringButton = layoutInflater.inflate(R.layout.item_tuner_string, stringsRow, false) as MaterialButton
            stringButton.text = bassString.name
            stringButton.setOnClickListener {
                guidedTuner.select(stringIndex)
                render(guidedTuner.currentState())
            }
            stringsRow.addView(stringButton)
            stringButtons += stringButton
        }
    }

    /**
     * @return Unit
     */
    private fun startListening() {
        if (readingCollectionJob != null) return
        readingCollectionJob = tunerController.pitchReadingStream()
            .onEach { frequencyHz -> render(guidedTuner.onReading(frequencyHz, SystemClock.elapsedRealtime())) }
            .launchIn(lifecycleScope)
    }

    /**
     * @param state What the tuner should show. Type: GuidedTunerState
     * @return Unit
     */
    private fun render(state: GuidedTunerState) {
        state.justTunedStringIndex?.let(::celebrateTuned)
        renderStringButtons(state)
        val targetIndex = state.targetStringIndex
        modeTextView.text = if (state.isAutomatic || targetIndex == null) {
            getString(R.string.tuner_mode_automatic)
        } else {
            getString(R.string.tuner_mode_chosen, tuning.strings[targetIndex].name)
        }
        if (state.justTunedStringIndex != null) return

        if (targetIndex == null) {
            stringNameTextView.text = getString(R.string.tuner_no_note)
            centsOffsetTextView.text = ""
            statusTextView.setText(R.string.tuner_all_tuned)
            statusTextView.setTextColor(colorFor(com.google.android.material.R.attr.colorSecondary))
            tunerGaugeView.showReading(0f, isInTune = true, isActive = false)
            return
        }
        val targetName = tuning.strings[targetIndex].name
        stringNameTextView.text = targetName
        val needleCents = state.needleCents
        val status = state.status
        if (needleCents == null || status == null) {
            centsOffsetTextView.text = ""
            statusTextView.text = getString(R.string.tuner_pluck_string, targetName)
            statusTextView.setTextColor(colorFor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            tunerGaugeView.showReading(0f, isInTune = false, isActive = false)
            return
        }
        val isInTune = status == TuningStatus.IN_TUNE
        centsOffsetTextView.text = tunerDisplayFormatter.centsLabel(needleCents)
        statusTextView.setText(statusTextFor(status))
        statusTextView.setTextColor(
            colorFor(if (isInTune) com.google.android.material.R.attr.colorSecondary else androidx.appcompat.R.attr.colorPrimary)
        )
        tunerGaugeView.showReading(tunerDisplayFormatter.needleRotationDegrees(needleCents), isInTune, isActive = true)
        tunerGaugeView.contentDescription = getString(R.string.tuner_gauge_description, getString(statusTextFor(status)))
    }

    /**
     * Chime, vibration and a message when a string is confirmed in tune.
     *
     * @param tunedStringIndex String just tuned. Type: Int
     * @return Unit
     */
    private fun celebrateTuned(tunedStringIndex: Int) {
        tuneChimePlayer?.play()
        val hapticConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        rootView.performHapticFeedback(hapticConstant)
        stringNameTextView.text = tuning.strings[tunedStringIndex].name
        centsOffsetTextView.text = tunerDisplayFormatter.centsLabel(0.0)
        statusTextView.text = getString(R.string.tuner_string_tuned, tuning.strings[tunedStringIndex].name)
        statusTextView.setTextColor(colorFor(com.google.android.material.R.attr.colorSecondary))
        tunerGaugeView.showReading(0f, isInTune = true, isActive = true)
    }

    /**
     * Tuned strings show a check; the string being tuned is highlighted.
     *
     * @param state Current tuner state. Type: GuidedTunerState
     * @return Unit
     */
    private fun renderStringButtons(state: GuidedTunerState) {
        stringButtons.forEachIndexed { stringIndex, stringButton ->
            val isTuned = stringIndex in state.tunedStringIndices
            val isTarget = stringIndex == state.targetStringIndex
            stringButton.setIconResource(if (isTuned) R.drawable.ic_check_circle else 0)
            val backgroundAttribute = when {
                isTarget -> androidx.appcompat.R.attr.colorPrimary
                isTuned -> com.google.android.material.R.attr.colorSecondaryContainer
                else -> com.google.android.material.R.attr.colorSurfaceContainerHigh
            }
            val contentAttribute = when {
                isTarget -> com.google.android.material.R.attr.colorOnPrimary
                isTuned -> com.google.android.material.R.attr.colorOnSecondaryContainer
                else -> com.google.android.material.R.attr.colorOnSurface
            }
            stringButton.backgroundTintList = android.content.res.ColorStateList.valueOf(colorFor(backgroundAttribute))
            stringButton.setTextColor(colorFor(contentAttribute))
            stringButton.iconTint = android.content.res.ColorStateList.valueOf(colorFor(contentAttribute))
            val stateDescription = getString(
                when {
                    isTuned -> R.string.tuner_string_state_tuned
                    isTarget -> R.string.tuner_string_state_target
                    else -> R.string.tuner_string_state_untuned
                }
            )
            stringButton.contentDescription =
                getString(R.string.tuner_string_button_description, tuning.strings[stringIndex].name, stateDescription)
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

    private companion object {
        /** Bass fundamentals top out around 400 Hz; above ~1 kHz there is only noise. */
        const val PITCH_ANALYSIS_CUTOFF_HZ = 1000.0
        /** Small capture buffers… */
        const val CAPTURE_SAMPLES = 1024
        /** …analysed in 93 ms windows (long enough for low E)… */
        const val WINDOW_SAMPLES = 4096
        /** …every 23 ms, so the needle moves in real time. */
        const val HOP_SAMPLES = 1024
    }
}
