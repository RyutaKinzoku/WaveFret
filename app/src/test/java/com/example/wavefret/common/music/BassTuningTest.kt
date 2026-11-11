package com.example.wavefret.common.music

import org.junit.Assert.assertThrows
import org.junit.Test

class BassTuningTest {

    @Test
    fun constructorRejectsATuningWithNoStrings() {
        assertThrows(IllegalArgumentException::class.java) {
            BassTuning(name = "Empty", strings = emptyList())
        }
    }
}