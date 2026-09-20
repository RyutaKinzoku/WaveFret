package com.example.wavefret.recording

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FakeAppStorageDirectoryProvider(private val fixedPath: String) : AppStorageDirectoryProvider {
    override fun baseDirectoryPath(): String = fixedPath
}

class RecordingsFolderProviderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun recordingsDirectoryPathAppendsRecordingsSubfolderToBaseDirectory() {
        val provider = RecordingsFolderProvider(FakeAppStorageDirectoryProvider(tempFolder.root.absolutePath))
        val expectedPath = File(tempFolder.root, "recordings").absolutePath
        assertEquals(expectedPath, provider.recordingsDirectoryPath())
    }

    @Test
    fun recordingsDirectoryPathCreatesTheFolderIfItDoesNotExist() {
        val provider = RecordingsFolderProvider(FakeAppStorageDirectoryProvider(tempFolder.root.absolutePath))
        val path = provider.recordingsDirectoryPath()
        assertTrue(File(path).exists())
    }
}