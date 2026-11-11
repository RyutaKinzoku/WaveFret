package com.example.wavefret.common.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.annotation.StringRes
import com.example.wavefret.R
import com.google.android.material.snackbar.Snackbar

/**
 * Explains why the microphone is needed after the player declined it, and
 * offers a shortcut to the app's system settings, so a denial is
 * recoverable instead of leaving a silently dead screen.
 *
 * Thin framework wrapper, verified on-device.
 *
 * @receiver Activity showing the message. Type: Activity
 * @param anchorView Any view in the activity's layout. Type: View
 * @param explanationResource Why this screen needs the microphone. Type: Int
 * @return Unit
 */
fun Activity.showMicrophonePermissionNeeded(anchorView: View, @StringRes explanationResource: Int) {
    Snackbar.make(anchorView, explanationResource, Snackbar.LENGTH_LONG)
        .setAction(R.string.open_settings) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }
        .show()
}
