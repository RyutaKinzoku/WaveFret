package com.example.wavefret.permission

import com.example.wavefret.common.permission.PermissionChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePermissionChecker(private val granted: Boolean) : PermissionChecker {
    override fun isGranted(permission: String): Boolean = granted
}

class AudioPermissionManagerTest {

    @Test
    fun needsPermissionRequestReturnsTrueWhenPermissionNotGranted() {
        val manager = AudioPermissionManager(FakePermissionChecker(granted = false))
        assertTrue(manager.needsPermissionRequest())
    }

    @Test
    fun needsPermissionRequestReturnsFalseWhenPermissionAlreadyGranted() {
        val manager = AudioPermissionManager(FakePermissionChecker(granted = true))
        assertFalse(manager.needsPermissionRequest())
    }
}