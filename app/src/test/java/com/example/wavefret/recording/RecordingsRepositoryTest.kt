package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecordingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun buildRepository(): RecordingsRepository {
        return RecordingsRepository(FakeRecordingDirectoryProvider(tempFolder.root.absolutePath))
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
}