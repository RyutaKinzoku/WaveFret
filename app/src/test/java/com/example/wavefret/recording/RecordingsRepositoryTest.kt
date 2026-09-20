package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecordingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun buildRepository(durationMillis: Long = 0L): RecordingsRepository {
        return RecordingsRepository(
            directoryProvider = FakeRecordingDirectoryProvider(tempFolder.root.absolutePath),
            audioDurationReader = FakeAudioDurationReader(durationMillis)
        )
    }

    @Test
    fun listRecordingsReturnsEmptyListWhenDirectoryHasNoRecordings() {
        val repository = buildRepository()
        assertTrue(repository.listRecordings().isEmpty())
    }

    @Test
    fun listRecordingsOnlyIncludesM4aFiles() {
        tempFolder.newFile("recording_1.m4a")
        tempFolder.newFile("notes.txt")
        val fileNames = buildRepository().listRecordings().map { it.fileName }
        assertEquals(listOf("recording_1.m4a"), fileNames)
    }

    @Test
    fun listRecordingsAreSortedNewestFirst() {
        tempFolder.newFile("recording_older.m4a").setLastModified(1_000L)
        tempFolder.newFile("recording_newer.m4a").setLastModified(2_000L)
        val fileNames = buildRepository().listRecordings().map { it.fileName }
        assertEquals(listOf("recording_newer.m4a", "recording_older.m4a"), fileNames)
    }

    @Test
    fun listRecordingsIncludesDurationFromAudioDurationReader() {
        tempFolder.newFile("recording_1.m4a")
        val durations = buildRepository(durationMillis = 5_000L).listRecordings().map { it.durationMillis }
        assertEquals(listOf(5_000L), durations)
    }

    @Test
    fun deleteRecordingRemovesTheFileFromDisk() {
        val file = tempFolder.newFile("recording_1.m4a")
        val wasDeleted = buildRepository().deleteRecording(file.absolutePath)
        assertTrue(wasDeleted)
        assertFalse(file.exists())
    }

    @Test
    fun deleteRecordingReturnsFalseWhenFileDoesNotExist() {
        val missingFilePath = File(tempFolder.root, "missing.m4a").absolutePath
        val wasDeleted = buildRepository().deleteRecording(missingFilePath)
        assertFalse(wasDeleted)
    }

    @Test
    fun listRecordingsNoLongerIncludesADeletedFile() {
        val file = tempFolder.newFile("recording_1.m4a")
        val repository = buildRepository()
        repository.deleteRecording(file.absolutePath)
        assertTrue(repository.listRecordings().isEmpty())
    }
}