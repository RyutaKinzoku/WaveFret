package com.example.wavefret.practice.playalong

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.PcmAudioSource
import com.example.wavefret.common.audio.PlaybackCapturePcmSource
import com.example.wavefret.common.audio.PlaybackCaptureService
import com.example.wavefret.common.imaging.ScreenFrameSource
import com.example.wavefret.common.imaging.toLuminanceImage
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.textrecognition.MlKitImageSymbolRecognizer
import com.example.wavefret.common.textrecognition.MlKitSymbolRecognizer
import com.example.wavefret.common.audio.WavFileWriter
import com.example.wavefret.common.time.ElapsedRealtimeClock
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.example.wavefret.practice.LessonEditorActivity
import com.example.wavefret.practice.LessonMediaSource
import com.example.wavefret.practice.songimport.FretboardPositionFinder
import com.example.wavefret.practice.songimport.LessonSynchronizer
import com.example.wavefret.practice.songimport.OnDeviceLessonTranscriber
import com.example.wavefret.practice.songimport.scan.ScoreImageReader
import com.example.wavefret.practice.songimport.scan.VideoFrameTabReader
import com.example.wavefret.practice.songimport.scan.VideoTabCollector
import com.example.wavefret.practice.songimport.scan.VideoTabLessonBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Listen mode: WaveFret hears a song and turns its bass line into a
 * lesson. With a YouTube link the video plays here (in YouTube's own
 * player) and, on Android 10 and later, WaveFret hears the video's sound
 * directly (after the player approves Android's capture prompt), because
 * phone speakers can't play deep bass notes; the microphone is the
 * fallback. While hearing the video directly it also reads the tab the
 * video shows, frame by frame; when enough tab is read, the lesson takes
 * its notes from that tab and its timing from the sound. The lesson keeps
 * the video's timing for play-along, and the video's sound and pictures
 * are never saved. Opened for an existing lesson, it only adds the
 * video's timing to that lesson's notes. Without a video the song plays on another
 * speaker, and what the microphone heard is kept as the lesson's audio.
 *
 * Android framework glue around the tested transcription classes; verified
 * on-device.
 */
class ListenActivity : WaveFretActivity() {

    private lateinit var rootView: ViewGroup
    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var toggleButton: MaterialButton

    private val appContainer = AppContainer(this)
    private val elapsedRealtimeClock = ElapsedRealtimeClock()
    private val microphoneSource = AudioRecordPcmSource(sampleRateHz = LISTEN_SAMPLE_RATE_HZ)
    private var youTubeVideoId: String? = null
    private var mediaPlayer: PlayAlongMediaPlayer? = null
    private var listeningJob: Job? = null
    private var timerJob: Job? = null
    private val heardSamples = ArrayList<FloatArray>()
    private var heardSampleCount = 0
    private var videoStartMillis = 0L
    private var frameReadingJob: Job? = null
    private val videoTabCollector = VideoTabCollector()

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) beginListening() else showMicrophonePermissionNeeded(rootView, R.string.listen_permission_needed)
    }

    private val captureApprovalLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { approval ->
        val approvalData = approval.data
        if (approval.resultCode == RESULT_OK && approvalData != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startHearingThePhone(approval.resultCode, approvalData)
        } else {
            useMicrophoneInstead()
        }
    }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listen)
        rootView = findViewById(R.id.listenRoot)
        timerTextView = findViewById(R.id.listenTimerTextView)
        statusTextView = findViewById(R.id.listenStatusTextView)
        toggleButton = findViewById(R.id.listenToggleButton)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.listen_title)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.applySystemBarPadding(applyTop = true, applyBottom = false)
        findViewById<View>(R.id.listenBottomBar).applySystemBarPadding(applyTop = false, applyBottom = true)

        youTubeVideoId = intent.getStringExtra(EXTRA_YOUTUBE_VIDEO_ID)
        val videoId = youTubeVideoId
        if (videoId != null) {
            findViewById<View>(R.id.songMediaVideoFrame).isVisible = true
            findViewById<View>(R.id.songMediaAudioTextView).isVisible = false
            toggleButton.isEnabled = false
            statusTextView.setText(R.string.play_along_loading)
            mediaPlayer = YouTubePlayAlongPlayer(
                findViewById(R.id.songMediaWebView), videoId,
                onReady = {
                    toggleButton.isEnabled = true
                    val lessonToSync = syncLessonTitle()
                    if (lessonToSync == null) statusTextView.setText(R.string.listen_status_youtube)
                    else statusTextView.text = getString(R.string.listen_status_sync, lessonToSync)
                },
                onError = {
                    toggleButton.isEnabled = false
                    statusTextView.setText(R.string.play_along_media_error)
                }
            )
        } else {
            findViewById<TextView>(R.id.songMediaAudioTextView).setText(R.string.listen_speaker_panel)
            statusTextView.setText(R.string.listen_status_speaker)
        }
        toggleButton.setOnClickListener { onToggleClicked() }
    }

    /**
     * Stops listening (discarding nothing heard so far) when the screen is left.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        stopListening()
    }

    /** @return Unit */
    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    /** @return Unit */
    private fun onToggleClicked() {
        when {
            listeningJob != null -> {
                stopListening()
                makeLesson()
            }
            appContainer.microphonePermissionManager.needsPermissionRequest() ->
                requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            else -> beginListening()
        }
    }

    /**
     * With a video on Android 10+, explains and asks to hear the video
     * directly; otherwise listens through the microphone.
     *
     * @return Unit
     */
    private fun beginListening() {
        if (youTubeVideoId == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startListening(microphoneSource)
            return
        }
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_music_note)
            .setTitle(R.string.listen_direct_title)
            .setMessage(R.string.listen_direct_message)
            .setPositiveButton(R.string.listen_direct_continue) { _, _ ->
                val projectionManager = getSystemService(MediaProjectionManager::class.java)
                val approvalIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                } else {
                    projectionManager.createScreenCaptureIntent()
                }
                captureApprovalLauncher.launch(approvalIntent)
            }
            .setNegativeButton(R.string.listen_direct_use_microphone) { _, _ -> startListening(microphoneSource) }
            .show()
    }

    /**
     * Starts the capture service with the player's approval and listens to
     * the phone's playback once it is ready.
     *
     * @param approvalResultCode Result code of the system prompt. Type: Int
     * @param approvalData Data of the system prompt. Type: Intent
     * @return Unit
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startHearingThePhone(approvalResultCode: Int, approvalData: Intent) {
        PlaybackCaptureService.start(this, approvalResultCode, approvalData)
        lifecycleScope.launch {
            val mediaProjection = withTimeoutOrNull(CAPTURE_START_TIMEOUT_MILLIS) {
                PlaybackCaptureService.activeProjection.filterNotNull().first()
            }
            if (mediaProjection == null) {
                useMicrophoneInstead()
            } else {
                startListening(PlaybackCapturePcmSource(mediaProjection, LISTEN_SAMPLE_RATE_HZ))
                if (youTubeVideoId != null && syncLessonTitle() == null) startReadingVideoFrames(mediaProjection)
            }
        }
    }

    /**
     * Reads the tab shown in the video about once a second while listening.
     *
     * @param mediaProjection The approved capture session. Type: MediaProjection
     * @return Unit
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startReadingVideoFrames(mediaProjection: MediaProjection) {
        val videoFrameView = findViewById<View>(R.id.songMediaVideoFrame)
        val screenBounds = screenBounds()
        frameReadingJob = lifecycleScope.launch(Dispatchers.Default) {
            val screenFrameSource = ScreenFrameSource(
                mediaProjection, screenBounds.width(), screenBounds.height(), resources.displayMetrics.densityDpi
            )
            try {
                MlKitSymbolRecognizer().use { symbolRecognizer ->
                    val videoFrameTabReader = VideoFrameTabReader(
                        ScoreImageReader(BassTunings.STANDARD, MlKitImageSymbolRecognizer(symbolRecognizer))
                    )
                    var framesRead = 0
                    while (isActive) {
                        delay(FRAME_INTERVAL_MILLIS)
                        val (videoRegion, videoPositionMillis) = withContext(Dispatchers.Main) {
                            regionOnScreen(videoFrameView) to (mediaPlayer?.positionMillis ?: 0L)
                        }
                        val frame = screenFrameSource.latestFrame(videoRegion) ?: continue
                        val frameColumns = try {
                            videoFrameTabReader.read(frame.toLuminanceImage())
                        } finally {
                            frame.recycle()
                        }
                        val collectedCount = synchronized(videoTabCollector) {
                            videoTabCollector.onFrame(frameColumns, videoPositionMillis)
                            videoTabCollector.columns.size
                        }
                        framesRead++
                        if (collectedCount > 0) withContext(Dispatchers.Main) {
                            statusTextView.text = resources.getQuantityString(R.plurals.listen_tab_notes_read, collectedCount, collectedCount)
                        } else if (framesRead == FRAMES_BEFORE_NO_TAB_NOTICE) withContext(Dispatchers.Main) {
                            statusTextView.setText(R.string.listen_no_tab_in_video)
                        }
                    }
                }
            } finally {
                screenFrameSource.close()
            }
        }
    }

    /**
     * @param view A view on screen. Type: View
     * @return Its area in screen pixels. Type: Rect
     */
    private fun regionOnScreen(view: View): Rect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    /**
     * @return The whole screen's size in pixels. Type: Rect
     */
    private fun screenBounds(): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return windowManager.maximumWindowMetrics.bounds
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /** @return The title of the lesson to add the video's timing to, or null when making a new lesson. Type: String? */
    private fun syncLessonTitle(): String? = intent.getStringExtra(EXTRA_SYNC_LESSON_TITLE)

    /**
     * @return Unit
     */
    private fun useMicrophoneInstead() {
        Snackbar.make(rootView, R.string.listen_direct_declined, Snackbar.LENGTH_LONG).setAnchorView(toggleButton).show()
        startListening(microphoneSource)
    }

    /**
     * @param audioSource Where the song is heard from. Type: PcmAudioSource
     * @return Unit
     */
    private fun startListening(audioSource: PcmAudioSource) {
        heardSamples.clear()
        heardSampleCount = 0
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mediaPlayer?.let { player ->
            videoStartMillis = player.positionMillis
            player.play()
        }
        listeningJob = audioSource.audioBufferStream()
            .onEach { audioBuffer ->
                if (heardSampleCount < MAXIMUM_LISTEN_SAMPLES) {
                    heardSamples += audioBuffer
                    heardSampleCount += audioBuffer.size
                }
            }
            .launchIn(lifecycleScope)
        val startedAtMillis = elapsedRealtimeClock.currentTimeMillis()
        timerJob = lifecycleScope.launch {
            while (isActive) {
                timerTextView.text = appContainer.durationFormatter.format(elapsedRealtimeClock.currentTimeMillis() - startedAtMillis)
                delay(TIMER_REFRESH_MILLIS)
            }
        }
        statusTextView.setText(R.string.listen_status_listening)
        toggleButton.setText(R.string.listen_stop)
        toggleButton.setIconResource(R.drawable.ic_stop)
    }

    /** @return Unit */
    private fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        frameReadingJob?.cancel()
        frameReadingJob = null
        timerJob?.cancel()
        timerJob = null
        mediaPlayer?.pause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) PlaybackCaptureService.stop(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        toggleButton.setText(R.string.listen_start)
        toggleButton.setIconResource(R.drawable.ic_mic)
    }

    /**
     * Asks what was playing (only for speaker listening: a YouTube video is
     * always a full song), then transcribes.
     *
     * @return Unit
     */
    private fun makeLesson() {
        if (heardSampleCount < MINIMUM_LISTEN_SAMPLES) {
            Snackbar.make(rootView, R.string.listen_too_short, Snackbar.LENGTH_LONG).setAnchorView(toggleButton).show()
            return
        }
        val heardAudio = FloatArray(heardSampleCount).also { joined ->
            var offset = 0
            heardSamples.forEach { chunk ->
                chunk.copyInto(joined, offset)
                offset += chunk.size
            }
        }
        if (youTubeVideoId != null) {
            transcribe(heardAudio, isFullSong = true)
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.audio_kind_title)
            .setItems(arrayOf(getString(R.string.audio_kind_bass_only), getString(R.string.audio_kind_full_song))) { _, choice ->
                transcribe(heardAudio, isFullSong = choice == FULL_SONG_CHOICE)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Transcribes off the main thread behind a cancellable progress dialog,
     * attaches the song for play-along, and opens the draft in the editor.
     *
     * @param heardAudio Everything the microphone heard. Type: FloatArray
     * @param isFullSong True to use basic-pitch, false for YIN. Type: Boolean
     * @return Unit
     */
    private fun transcribe(heardAudio: FloatArray, isFullSong: Boolean) {
        val progressView = layoutInflater.inflate(R.layout.dialog_transcribing, rootView, false)
        progressView.findViewById<TextView>(R.id.transcribingStepTextView).setText(R.string.transcribing_detecting)
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.transcribing_title)
            .setView(progressView)
            .setCancelable(false)
            .create()
        val videoId = youTubeVideoId
        val transcriptionJob = lifecycleScope.launch {
            try {
                val draftLesson = withContext(Dispatchers.Default) {
                    val defaultTitle = intent.getStringExtra(EXTRA_SONG_TITLE)
                        ?: getString(if (videoId != null) R.string.youtube_lesson_title else R.string.listen_lesson_title)
                    val transcriber = OnDeviceLessonTranscriber(this@ListenActivity)
                    if (videoId != null) {
                        val video = LessonMediaSource.YouTubeVideo(videoId)
                        val heardNotes = transcriber.transcribeNotes(heardAudio, LISTEN_SAMPLE_RATE_HZ, isFullSong)
                            .map { it.copy(onsetMillis = it.onsetMillis + videoStartMillis) }
                        val lessonToSync = syncLessonTitle()?.let { title ->
                            appContainer.importedExercisesRepository.listExercises().firstOrNull { it.title == title }
                        }
                        val videoTabLesson = synchronized(videoTabCollector) {
                            VideoTabLessonBuilder(BassTunings.STANDARD, MINIMUM_VIDEO_TAB_NOTES)
                                .build(defaultTitle, videoTabCollector, heardNotes, video)
                        }
                        when {
                            lessonToSync != null -> LessonSynchronizer(FretboardPositionFinder(BassTunings.STANDARD))
                                .synchronize(lessonToSync, heardNotes, video)
                            videoTabLesson != null -> videoTabLesson
                            else -> transcriber.transcribe(defaultTitle, heardAudio, LISTEN_SAMPLE_RATE_HZ, isFullSong)
                                .withTimingsShiftedBy(videoStartMillis).copy(mediaSource = video)
                        }
                    } else {
                        val transcribed = transcriber.transcribe(defaultTitle, heardAudio, LISTEN_SAMPLE_RATE_HZ, isFullSong)
                        val wavBytes = ByteArrayOutputStream().also { WavFileWriter().write(heardAudio, LISTEN_SAMPLE_RATE_HZ, it) }
                        val storedName = LessonAudioStore(appContainer.appStorageDirectoryProvider)
                            .store(LISTENED_AUDIO_FILE_NAME, wavBytes.toByteArray().inputStream())
                        transcribed.copy(mediaSource = LessonMediaSource.StoredAudio(storedName))
                    }
                }
                progressDialog.dismiss()
                startActivity(LessonEditorActivity.intentFor(this@ListenActivity, draftLesson, originalTitle = syncLessonTitle()))
                finish()
            } catch (transcriptionFailure: IllegalArgumentException) {
                progressDialog.dismiss()
                Snackbar.make(rootView, getString(R.string.transcription_failed, transcriptionFailure.message), Snackbar.LENGTH_LONG)
                    .setAnchorView(toggleButton)
                    .show()
            }
        }
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ -> transcriptionJob.cancel() }
        progressDialog.show()
    }

    companion object {
        private const val EXTRA_YOUTUBE_VIDEO_ID = "com.example.wavefret.practice.playalong.YOUTUBE_VIDEO_ID"
        private const val EXTRA_SONG_TITLE = "com.example.wavefret.practice.playalong.SONG_TITLE"
        private const val LISTEN_SAMPLE_RATE_HZ = 22_050
        private const val MAXIMUM_LISTEN_SAMPLES = LISTEN_SAMPLE_RATE_HZ * 6 * 60
        private const val MINIMUM_LISTEN_SAMPLES = LISTEN_SAMPLE_RATE_HZ * 3
        private const val TIMER_REFRESH_MILLIS = 250L
        private const val FULL_SONG_CHOICE = 1
        private const val LISTENED_AUDIO_FILE_NAME = "listened.wav"
        private const val CAPTURE_START_TIMEOUT_MILLIS = 3_000L
        private const val FRAMES_BEFORE_NO_TAB_NOTICE = 15
        private const val FRAME_INTERVAL_MILLIS = 1_000L
        private const val MINIMUM_VIDEO_TAB_NOTES = 8
        private const val EXTRA_SYNC_LESSON_TITLE = "com.example.wavefret.practice.playalong.SYNC_LESSON_TITLE"

        /**
         * @param context Context starting the screen. Type: Context
         * @param youTubeVideoId Video to play while listening, or null to listen to a speaker. Type: String?
         * @param songTitle Name for the lesson, e.g. from "Find a song", or null for a default. Type: String?
         * @param syncLessonTitle Existing lesson to give this video's timing to, or null to make a new lesson. Type: String?
         * @return An intent that opens Listen mode. Type: Intent
         */
        fun intentFor(context: Context, youTubeVideoId: String?, songTitle: String? = null, syncLessonTitle: String? = null): Intent {
            return Intent(context, ListenActivity::class.java)
                .putExtra(EXTRA_YOUTUBE_VIDEO_ID, youTubeVideoId)
                .putExtra(EXTRA_SONG_TITLE, songTitle ?: syncLessonTitle)
                .putExtra(EXTRA_SYNC_LESSON_TITLE, syncLessonTitle)
        }
    }
}
