package com.example.wavefret.practice.playalong

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LessonAudioStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val lessonAudioStore by lazy {
        LessonAudioStore(object : AppStorageDirectoryProvider {
            override fun baseDirectoryPath(): String = temporaryFolder.root.absolutePath
        })
    }

    @Test
    fun storedAudioCanBeFoundAgainByItsName() {
        val storedName = lessonAudioStore.store("Billie Jean.mp3", "audio".toByteArray().inputStream())

        assertArrayEquals("audio".toByteArray(), lessonAudioStore.fileFor(storedName).readBytes())
    }

    @Test
    fun storedNamesKeepTheExtensionAndAreUnique() {
        val firstName = lessonAudioStore.store("song.m4a", byteArrayOf(1).inputStream())
        val secondName = lessonAudioStore.store("song.m4a", byteArrayOf(2).inputStream())

        assertTrue(firstName.endsWith(".m4a"))
        assertFalse(firstName == secondName)
    }

    @Test
    fun namesCannotEscapeTheAudioFolder() {
        val storedName = lessonAudioStore.store("../../evil.mp3", byteArrayOf(1).inputStream())

        assertEquals(temporaryFolder.root.resolve("lesson_audio").canonicalPath, lessonAudioStore.fileFor(storedName).parentFile!!.canonicalPath)
    }

    @Test
    fun deletedAudioIsGone() {
        val storedName = lessonAudioStore.store("song.wav", byteArrayOf(1).inputStream())

        lessonAudioStore.delete(storedName)

        assertFalse(lessonAudioStore.fileFor(storedName).exists())
    }
}
