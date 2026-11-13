package com.example.wavefret.practice.playalong

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.time.ElapsedRealtimeClock
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.HitConfirmer
import com.example.wavefret.practice.LessonMediaSource
import com.example.wavefret.practice.NoteMatcher
import com.example.wavefret.practice.PracticeDisplayFormatter
import com.example.wavefret.practice.PracticeListeningPipeline
import com.example.wavefret.practice.PracticeProgress
import com.example.wavefret.practice.TabStaffView
import com.example.wavefret.practice.progression.PracticeMode
import com.example.wavefret.practice.progression.PracticeResult
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Play-along: the original song (YouTube or the stored recording) plays
 * above the tab and stops at every note until the player plays it right,
 * as many tries as it takes, then carries on.
 *
 * Android framework glue — the stop-and-wait rules live in the tested
 * [PlayAlongSession] — verified on-device.
 */
class PlayAlongActivity : WaveFretActivity() {

    private lateinit var rootView: View
    private lateinit var currentNoteTextView: TextView
    private lateinit var feedbackTextView: TextView
    private lateinit var tabStaffView: TabStaffView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var noteProgressTextView: TextView
    private lateinit var startPauseButton: MaterialButton

    private val appContainer = AppContainer(this)
    private val practiceDisplayFormatter = PracticeDisplayFormatter()
    private val practiceController = PracticeListeningPipeline.createController(ElapsedRealtimeClock())

    private lateinit var lesson: Exercise
    private lateinit var playAlongSession: PlayAlongSession
    private var mediaPlayer: PlayAlongMediaPlayer? = null
    private var listeningJob: Job? = null
    private var syncJob: Job? = null
    private var hasRecordedCompletion = false

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startPlaying() else showMicrophonePermissionNeeded(rootView, R.string.practice_permission_needed)
    }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play_along)
        rootView = findViewById(R.id.playAlongRoot)
        currentNoteTextView = findViewById(R.id.playAlongCurrentNoteTextView)
        feedbackTextView = findViewById(R.id.playAlongFeedbackTextView)
        tabStaffView = findViewById(R.id.playAlongTabStaffView)
        progressIndicator = findViewById(R.id.playAlongProgressIndicator)
        noteProgressTextView = findViewById(R.id.playAlongNoteProgressTextView)
        startPauseButton = findViewById(R.id.playAlongStartButton)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.applySystemBarPadding(applyTop = true, applyBottom = false)
        findViewById<View>(R.id.playAlongBottomBar).applySystemBarPadding(applyTop = false, applyBottom = true)

        val foundLesson = appContainer.allExercises().lastOrNull { it.title == intent.getStringExtra(EXTRA_LESSON_TITLE) }
        if (foundLesson?.noteOnsetsMillis == null || foundLesson.mediaSource == null) {
            feedbackTextView.setText(R.string.play_along_unavailable)
            return
        }
        lesson = foundLesson
        topAppBar.title = lesson.title
        playAlongSession = PlayAlongSession(lesson, NoteMatcher(), HitConfirmer())
        feedbackTextView.setText(R.string.play_along_loading)
        mediaPlayer = createMediaPlayer(requireNotNull(lesson.mediaSource))
        startPauseButton.setOnClickListener { onStartPauseClicked() }
        findViewById<MaterialButton>(R.id.playAlongRestartButton).setOnClickListener { restartSong() }
        renderProgress(playAlongSession.currentProgress())
    }

    /**
     * Stops listening and pauses the song when the screen is left.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        if (::playAlongSession.isInitialized) stopPlaying()
    }

    /**
     * @return Unit
     */
    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    /**
     * @param mediaSource Where the song comes from. Type: LessonMediaSource
     * @return A player for it. Type: PlayAlongMediaPlayer
     */
    private fun createMediaPlayer(mediaSource: LessonMediaSource): PlayAlongMediaPlayer {
        val webView = findViewById<WebView>(R.id.songMediaWebView)
        val audioPanel = findViewById<View>(R.id.songMediaAudioTextView)
        return when (mediaSource) {
            is LessonMediaSource.YouTubeVideo -> {
                findViewById<View>(R.id.songMediaVideoFrame).isVisible = true
                audioPanel.isVisible = false
                YouTubePlayAlongPlayer(webView, mediaSource.videoId, onReady = ::onMediaReady, onError = { onMediaError() })
            }
            is LessonMediaSource.StoredAudio -> {
                findViewById<TextView>(R.id.songMediaAudioTextView).setText(R.string.play_along_original_recording)
                AudioFilePlayAlongPlayer(
                    LessonAudioStore(appContainer.appStorageDirectoryProvider).fileFor(mediaSource.fileName),
                    onReady = ::onMediaReady,
                    onError = ::onMediaError
                )
            }
        }
    }

    /** @return Unit */
    private fun onMediaReady() {
        startPauseButton.isEnabled = true
        feedbackTextView.setText(R.string.play_along_ready)
    }

    /** @return Unit */
    private fun onMediaError() {
        startPauseButton.isEnabled = false
        stopPlaying()
        feedbackTextView.setText(R.string.play_along_media_error)
    }

    /** @return Unit */
    private fun onStartPauseClicked() {
        when {
            listeningJob != null -> stopPlaying()
            appContainer.microphonePermissionManager.needsPermissionRequest() ->
                requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            else -> startPlaying()
        }
    }

    /**
     * Starts the song, the microphone, and the loop that stops the song at
     * each note and resumes it once the note is played.
     *
     * @return Unit
     */
    private fun startPlaying() {
        val player = mediaPlayer ?: return
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startPauseButton.setText(R.string.play_along_pause)
        startPauseButton.setIconResource(R.drawable.ic_stop)
        listeningJob = practiceController.progressStream(playAlongSession)
            .onEach { progress ->
                followSession(player)
                renderProgress(progress)
            }
            .launchIn(lifecycleScope)
        syncJob = lifecycleScope.launch {
            while (isActive) {
                playAlongSession.onMediaPosition(player.positionMillis)
                followSession(player)
                renderProgress(playAlongSession.currentProgress())
                delay(SYNC_INTERVAL_MILLIS)
            }
        }
        if (playAlongSession.shouldMediaPlay) player.play()
    }

    /**
     * Plays or pauses the song to match what the session wants.
     *
     * @param player The song. Type: PlayAlongMediaPlayer
     * @return Unit
     */
    private fun followSession(player: PlayAlongMediaPlayer) {
        if (playAlongSession.shouldMediaPlay && !player.isPlaying) player.play()
        if (!playAlongSession.shouldMediaPlay && player.isPlaying) player.pause()
    }

    /** @return Unit */
    private fun stopPlaying() {
        listeningJob?.cancel()
        listeningJob = null
        syncJob?.cancel()
        syncJob = null
        mediaPlayer?.pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startPauseButton.setText(R.string.play_along_start)
        startPauseButton.setIconResource(R.drawable.ic_play)
    }

    /** @return Unit */
    private fun restartSong() {
        if (!::playAlongSession.isInitialized) return
        mediaPlayer?.seekTo(0)
        playAlongSession.restart()
        hasRecordedCompletion = false
        renderProgress(playAlongSession.currentProgress())
    }

    /**
     * @param progress Latest progress. Type: PracticeProgress
     * @return Unit
     */
    private fun renderProgress(progress: PracticeProgress) {
        val displayState = practiceDisplayFormatter.format(lesson, progress)
        tabStaffView.render(lesson, displayState)
        progressIndicator.max = progress.noteCount
        progressIndicator.setProgressCompat(progress.hitCount, true)
        noteProgressTextView.text = getString(R.string.practice_note_progress, displayState.currentNoteNumber, displayState.noteCount)
        val stringName = displayState.currentStringName
        val fret = displayState.currentFret
        currentNoteTextView.text = when {
            stringName == null || fret == null -> getString(R.string.summary_score, PERFECT_SCORE)
            fret == 0 -> getString(R.string.practice_current_note_open, stringName)
            else -> getString(R.string.practice_current_note, stringName, fret)
        }
        if (listeningJob == null && !progress.isComplete) return
        val feedbackResource = when {
            progress.isComplete -> R.string.play_along_complete
            playAlongSession.isWaitingForNote && progress.lastReading == com.example.wavefret.practice.PitchReading.NOT_MATCHING ->
                R.string.practice_feedback_wrong_note
            playAlongSession.isWaitingForNote -> R.string.play_along_listening_for_note
            else -> R.string.play_along_song_playing
        }
        feedbackTextView.setText(feedbackResource)
        feedbackTextView.setTextColor(
            MaterialColors.getColor(
                rootView,
                when (feedbackResource) {
                    R.string.practice_feedback_wrong_note -> androidx.appcompat.R.attr.colorError
                    R.string.play_along_complete -> com.google.android.material.R.attr.colorSecondary
                    else -> com.google.android.material.R.attr.colorOnSurfaceVariant
                }
            )
        )
        if (progress.isComplete && !hasRecordedCompletion) recordCompletion(progress)
    }

    /**
     * @param progress Final progress. Type: PracticeProgress
     * @return Unit
     */
    private fun recordCompletion(progress: PracticeProgress) {
        hasRecordedCompletion = true
        appContainer.practiceResultsRepository.record(
            PracticeResult(
                exerciseTitle = lesson.title,
                practiceMode = PracticeMode.WAIT,
                scorePercent = PERFECT_SCORE,
                hitCount = progress.hitCount,
                missCount = 0,
                completedAtMillis = appContainer.clock.currentTimeMillis()
            )
        )
        Snackbar.make(rootView, R.string.play_along_complete, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.playAlongBottomBar)
            .show()
    }

    companion object {
        private const val EXTRA_LESSON_TITLE = "com.example.wavefret.practice.playalong.LESSON_TITLE"
        private const val SYNC_INTERVAL_MILLIS = 40L
        private const val PERFECT_SCORE = 100

        /**
         * @param context Context starting the screen. Type: Context
         * @param lessonTitle Timed lesson with a media source. Type: String
         * @return An intent that opens play-along. Type: Intent
         */
        fun intentFor(context: Context, lessonTitle: String): Intent {
            return Intent(context, PlayAlongActivity::class.java).putExtra(EXTRA_LESSON_TITLE, lessonTitle)
        }
    }
}
