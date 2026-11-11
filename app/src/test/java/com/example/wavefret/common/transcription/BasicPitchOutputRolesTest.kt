package com.example.wavefret.common.transcription

import org.junit.Assert.assertSame
import org.junit.Test

class BasicPitchOutputRolesTest {

    private val sparse = Array(10) { frame -> FloatArray(88) { if (frame == 0 && it == 5) 0.9f else 0f } }
    private val dense = Array(10) { FloatArray(88) { 0.4f } }

    @Test
    fun theSparserMapIsTheOnsetMap() {
        val outputRoles = BasicPitchOutputRoles.assign(firstMap = sparse, secondMap = dense)

        assertSame(sparse, outputRoles.onsets)
        assertSame(dense, outputRoles.notes)
    }

    @Test
    fun orderDoesNotMatter() {
        val outputRoles = BasicPitchOutputRoles.assign(firstMap = dense, secondMap = sparse)

        assertSame(sparse, outputRoles.onsets)
    }
}
