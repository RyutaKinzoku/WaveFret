package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.TabTextParser
import com.example.wavefret.practice.TabTextWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportedExercisesRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun repository() = ImportedExercisesRepository(
        appStorageDirectoryProvider = object : AppStorageDirectoryProvider {
            override fun baseDirectoryPath(): String = temporaryFolder.root.absolutePath
        },
        tabTextWriter = TabTextWriter(),
        tabTextParser = TabTextParser(BassTunings.STANDARD)
    )

    private fun exerciseTitled(title: String) = Exercise(
        title = title, tuning = BassTunings.STANDARD, tempoBpm = 90, notes = listOf(TabNote(0, 0), TabNote(1, 2))
    )

    @Test
    fun startsEmpty() {
        assertTrue(repository().listExercises().isEmpty())
    }

    @Test
    fun savedExerciseIsListedAgain() {
        repository().save(exerciseTitled("Riff"))

        assertEquals(listOf(exerciseTitled("Riff")), repository().listExercises())
    }

    @Test
    fun exercisesAreListedAlphabeticallyByTitle() {
        val importedExercisesRepository = repository()
        importedExercisesRepository.save(exerciseTitled("Walking line"))
        importedExercisesRepository.save(exerciseTitled("Another one"))

        assertEquals(listOf("Another one", "Walking line"), importedExercisesRepository.listExercises().map { it.title })
    }

    @Test
    fun savingTheSameTitleReplacesTheExercise() {
        val importedExercisesRepository = repository()
        importedExercisesRepository.save(exerciseTitled("Riff"))
        importedExercisesRepository.save(exerciseTitled("Riff").copy(tempoBpm = 120))

        assertEquals(listOf(120), importedExercisesRepository.listExercises().map { it.tempoBpm })
    }

    @Test
    fun titlesWithUnsafeFileCharactersCanBeSaved() {
        repository().save(exerciseTitled("AC/DC: Back in Black?"))

        assertEquals(listOf("AC/DC: Back in Black?"), repository().listExercises().map { it.title })
    }

    @Test
    fun deletedExerciseIsNoLongerListed() {
        val importedExercisesRepository = repository()
        importedExercisesRepository.save(exerciseTitled("Riff"))

        importedExercisesRepository.delete("Riff")

        assertTrue(importedExercisesRepository.listExercises().isEmpty())
    }

    @Test
    fun unreadableFilesAreSkipped() {
        val importedExercisesRepository = repository()
        importedExercisesRepository.save(exerciseTitled("Good"))
        temporaryFolder.root.resolve("imported_tabs/broken.wavefret.txt").writeText("not a tab at all ???")

        assertEquals(listOf("Good"), importedExercisesRepository.listExercises().map { it.title })
    }
}
