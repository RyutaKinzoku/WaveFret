package com.example.wavefret.common.permission

/**
 * Abstraction over Android's permission-checking API, so permission logic
 * can be unit tested on the JVM without an Android framework dependency.
 */
interface PermissionChecker {
    /**
     * Checks whether a given Android permission is currently granted.
     *
     * @param permission Fully qualified permission string (e.g. android.Manifest.permission.RECORD_AUDIO). Type: String
     * @return True if the permission is granted, false otherwise. Type: Boolean
     */
    fun isGranted(permission: String): Boolean
}