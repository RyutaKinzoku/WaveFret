package com.example.wavefret.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake used in tests to avoid depending on the real Android permission API. */
class FakePermissionChecker(private val granted: Boolean) : PermissionChecker {
    override fun isGranted(permission: String): Boolean = granted
}

class AudioPermissionManagerTest {

    @Test
    fun `needsPermissionRequest returns true when permission not granted`() {
        val manager = AudioPermissionManager(FakePermissionChecker(granted = false))
        assertTrue(manager.needsPermissionRequest())
    }

    @Test
    fun `needsPermissionRequest returns false when permission already granted`() {
        val manager = AudioPermissionManager(FakePermissionChecker(granted = true))
        assertFalse(manager.needsPermissionRequest())
    }
}