package com.example.wavefret.practice.playalong

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.WavFileWriter
import com.example.wavefret.common.time.ElapsedRealtimeClock
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.example.wavefret.practice.LessonEditorActivity
import com.example.wavefret.practice.LessonMediaSource
import com.example.wavefret.practice.songimport.OnDeviceLessonTranscriber
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Listen mode: WaveFret hears a song through the microphone and turns its
 * bass line into a lesson. With a YouTube link the video plays here (in
 * YouTube's own player) while the microphone listens, and the lesson keeps
 * the video's timing for play-along; otherwise the song plays on another
 * speaker, and what the microphone heard is kept as the lesson's audio.
 *
 * Android framework glue around the tested transcription classes; verified
 * on-device.
 */
class ListenActivity : AppCompatActivity() {

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

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startListening() else showMicrophonePermissionNeeded(rootView, R.string.listen_permission_needed)
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
                    statusTextView.setText(R.string.listen_status_youtube)
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
            else -> startListening()
        }
    }

    /** @return Unit */
    private fun startListening() {
        heardSamples.clear()
        heardSampleCount = 0
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mediaPlayer?.let { player ->
            videoStartMillis = player.positionMillis
            player.play()
        }
        listeningJob = microphoneSource.audioBufferStream()
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
        timerJob?.cancel()
        timerJob = null
        mediaPlayer?.pause()
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
                    val transcribed = OnDeviceLessonTranscriber(this@ListenActivity)
                        .transcribe(defaultTitle, heardAudio, LISTEN_SAMPLE_RATE_HZ, isFullSong)
                    if (videoId != null) {
                        transcribed.withTimingsShiftedBy(videoStartMillis).copy(mediaSource = LessonMediaSource.YouTubeVideo(videoId))
                    } else {
                        val wavBytes = ByteArrayOutputStream().also { WavFileWriter().write(heardAudio, LISTEN_SAMPLE_RATE_HZ, it) }
                        val storedName = LessonAudioStore(appContainer.appStorageDirectoryProvider)
                            .store(LISTENED_AUDIO_FILE_NAME, wavBytes.toByteArray().inputStream())
                        transcribed.copy(mediaSource = LessonMediaSource.StoredAudio(storedName))
                    }
                }
                progressDialog.dismiss()
                startActivity(LessonEditorActivity.intentFor(this@ListenActivity, draftLesson, originalTitle = null))
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

        /**
         * @param context Context starting the screen. Type: Context
         * @param youTubeVideoId Video to play while listening, or null to listen to a speaker. Type: String?
         * @param songTitle Name for the lesson, e.g. from "Find a song", or null for a default. Type: String?
         * @return An intent that opens Listen mode. Type: Intent
         */
        fun intentFor(context: Context, youTubeVideoId: String?, songTitle: String? = null): Intent {
            return Intent(context, ListenActivity::class.java)
                .putExtra(EXTRA_YOUTUBE_VIDEO_ID, youTubeVideoId)
                .putExtra(EXTRA_SONG_TITLE, songTitle)
        }
    }
}
