package com.example.wavefret.common.audio

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.wavefret.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service Android requires while an app hears the phone's
 * playback. It turns the player's approval into a [MediaProjection] and
 * shares it through [activeProjection] until it is stopped.
 *
 * Android framework glue, verified on-device.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null

    /**
     * @param intent Holds the approval result. Type: Intent?
     * @return Null: this service is never bound. Type: IBinder?
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * @param intent Holds the approval result from the system dialog. Type: Intent?
     * @param flags Start flags, unused. Type: Int
     * @param startId Start id, unused. Type: Int
     * @return Not sticky: a lost approval can't be restored. Type: Int
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
        val approvalData = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_APPROVAL_DATA, Intent::class.java) }
        val approvalResultCode = intent?.getIntExtra(EXTRA_APPROVAL_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        if (approvalData == null || approvalResultCode != Activity.RESULT_OK) {
            stopSelf()
            return START_NOT_STICKY
        }
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = projectionManager.getMediaProjection(approvalResultCode, approvalData)?.also { projection ->
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            }, Handler(Looper.getMainLooper()))
        }
        mutableActiveProjection.value = mediaProjection
        if (mediaProjection == null) stopSelf()
        return START_NOT_STICKY
    }

    /**
     * @return Unit
     */
    override fun onDestroy() {
        mutableActiveProjection.value = null
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
    }

    /**
     * @return The ongoing notification Android shows while playback is heard. Type: android.app.Notification
     */
    private fun buildNotification(): android.app.Notification {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.playback_capture_channel), NotificationManager.IMPORTANCE_LOW)
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(getString(R.string.playback_capture_notification_title))
            .setContentText(getString(R.string.playback_capture_notification_body))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "playback_capture"
        private const val NOTIFICATION_ID = 7
        private const val EXTRA_APPROVAL_RESULT_CODE = "com.example.wavefret.extra.APPROVAL_RESULT_CODE"
        private const val EXTRA_APPROVAL_DATA = "com.example.wavefret.extra.APPROVAL_DATA"

        private val mutableActiveProjection = MutableStateFlow<MediaProjection?>(null)

        /** The approved capture session while the service runs, otherwise null. Type: StateFlow<MediaProjection?> */
        val activeProjection: StateFlow<MediaProjection?> = mutableActiveProjection.asStateFlow()

        /**
         * @param context Context starting the service. Type: Context
         * @param approvalResultCode Result code from the system's capture dialog. Type: Int
         * @param approvalData Data from the system's capture dialog. Type: Intent
         * @return Unit
         */
        fun start(context: Context, approvalResultCode: Int, approvalData: Intent) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PlaybackCaptureService::class.java)
                    .putExtra(EXTRA_APPROVAL_RESULT_CODE, approvalResultCode)
                    .putExtra(EXTRA_APPROVAL_DATA, approvalData)
            )
        }

        /**
         * @param context Context stopping the service. Type: Context
         * @return Unit
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackCaptureService::class.java))
        }
    }
}
