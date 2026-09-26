package com.example.wavefret.common.permission

import android.Manifest

/**
 * Encapsulates the app's microphone permission policy. Shared by any
 * feature that needs RECORD_AUDIO access (recording, and now the tuner).
 * Single responsibility: decide whether a RECORD_AUDIO request is needed.
 *
 * @property permissionChecker Source of truth for current permission grants. Type: PermissionChecker
 */
class MicrophonePermissionManager(private val permissionChecker: PermissionChecker) {

    /**
     * @return True if RECORD_AUDIO is not yet granted and should be requested,
     *   false if it is already granted. Type: Boolean
     */
    fun needsPermissionRequest(): Boolean {
        return !permissionChecker.isGranted(Manifest.permission.RECORD_AUDIO)
    }
}