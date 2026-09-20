package com.example.wavefret.permission

import android.Manifest
import com.example.wavefret.common.permission.PermissionChecker

/**
 * Encapsulates the app's audio-recording permission policy.
 * Single responsibility: decide whether a RECORD_AUDIO request is needed.
 *
 * @property permissionChecker Source of truth for current permission grants. Type: PermissionChecker
 */
class AudioPermissionManager(private val permissionChecker: PermissionChecker) {

    /**
     * Determines whether the app must prompt the user for RECORD_AUDIO access.
     *
     * @return True if RECORD_AUDIO is not yet granted and should be requested,
     *   false if it is already granted. Type: Boolean
     */
    fun needsPermissionRequest(): Boolean {
        return !permissionChecker.isGranted(Manifest.permission.RECORD_AUDIO)
    }
}