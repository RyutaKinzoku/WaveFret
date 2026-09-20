package com.example.wavefret.common.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Real implementation of PermissionChecker backed by ContextCompat.checkSelfPermission.
 *
 * @property context Context used to query the permission state. Type: Context
 */
class SystemPermissionChecker(private val context: Context) : PermissionChecker {

    /**
     * @param permission Fully qualified permission string to check. Type: String
     * @return True if context currently holds permission, false otherwise. Type: Boolean
     */
    override fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}