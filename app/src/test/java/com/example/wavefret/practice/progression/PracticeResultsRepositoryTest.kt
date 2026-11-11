package com.example.wavefret.practice.progression

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PracticeResultsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun repository() = PracticeResultsRepository(object : AppStorageDirectoryProvider {
        override fun baseDirectoryPath(): String = temporaryFolder.root.absolutePath
    })

    @Test
    fun startsEmpty() {
        assertTrue(repository().listResults().isEmpty())
    }

    @Test
    fun recordedResultsAreListedInOrderAcrossInstances() {
        val first = PracticeResult("Open strings", PracticeMode.WAIT, 100, hitCount = 8, missCount = 0, completedAtMillis = 1_000)
        val second = PracticeResult("Riff", PracticeMode.TEMPO, 75, hitCount = 6, missCount = 2, completedAtMillis = 2_000)
        repository().record(first)
        repository().record(second)

        assertEquals(listOf(first, second), repository().listResults())
    }

    @Test
    fun titlesWithTabsAndNewlinesSurviveStorage() {
        val awkward = PracticeResult("Odd\ttitle\nhere", PracticeMode.TEMPO, 50, hitCount = 1, missCount = 1, completedAtMillis = 5)
        repository().record(awkward)

        assertEquals(listOf(awkward), repository().listResults())
    }

    @Test
    fun corruptLinesAreSkipped() {
        val valid = PracticeResult("Riff", PracticeMode.WAIT, 100, hitCount = 4, missCount = 0, completedAtMillis = 9)
        repository().record(valid)
        temporaryFolder.root.resolve("practice_results.tsv").appendText("garbage line\n")

        assertEquals(listOf(valid), repository().listResults())
    }
}
