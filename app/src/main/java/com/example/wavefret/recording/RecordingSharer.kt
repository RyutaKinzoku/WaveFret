package com.example.wavefret.recording

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.wavefret.R
import java.io.File

/**
 * Opens Android's share sheet for a take, so the player can send it with
 * any app they choose (messaging, email, cloud). Only the chosen file is
 * exposed, read-only, through a FileProvider; nothing is sent until the
 * player picks an app and a recipient there.
 *
 * Thin framework wrapper, verified on-device.
 *
 * @param context Context that starts the share sheet. Type: Context
 */
class RecordingSharer(private val context: Context) {

    /**
     * @param recording Take to share. Type: RecordingInfo
     * @return Unit
     */
    fun share(recording: RecordingInfo) {
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", File(recording.filePath))
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = recording.mediaType.shareMimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri(recording.fileName, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.recording_share_chooser_title)))
    }

    companion object {
        /** Appended to the package name to form the FileProvider authority. Type: String */
        const val AUTHORITY_SUFFIX = ".recordings"
    }
}
