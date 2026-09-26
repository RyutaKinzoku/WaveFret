package com.example.wavefret.common.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePermissionChecker(private val granted: Boolean) : PermissionChecker {
    override fun isGranted(permission: String): Boolean = granted
}

class MicrophonePermissionManagerTest {

    @Test
    fun needsPermissionRequestReturnsTrueWhenPermissionNotGranted() {
        val manager = MicrophonePermissionManager(FakePermissionChecker(granted = false))
        assertTrue(manager.needsPermissionRequest())
    }

    @Test
    fun needsPermissionRequestReturnsFalseWhenPermissionAlreadyGranted() {
        val manager = MicrophonePermissionManager(FakePermissionChecker(granted = true))
        assertFalse(manager.needsPermissionRequest())
    }
}