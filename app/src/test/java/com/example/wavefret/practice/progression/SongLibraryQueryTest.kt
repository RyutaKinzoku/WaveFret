package com.example.wavefret.practice.progression

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Test

class SongLibraryQueryTest {

    private fun item(title: String, score: Int, isImported: Boolean) = ExercisePickerItem(
        exercise = Exercise(title, BassTunings.STANDARD, 80, listOf(TabNote(0, 0))),
        difficultyLevel = DifficultyLevel.forScore(score),
        bestScorePercent = null,
        isMastered = false,
        isRecommended = false,
        difficultyScore = score,
        isImported = isImported
    )

    private val library = listOf(
        item("Open strings", 5, isImported = false),
        item("Another One Bites the Dust", 40, isImported = true),
        item("Billie Jean", 25, isImported = true),
        item("Octave jumps", 60, isImported = false)
    )

    @Test
    fun defaultQueryListsEverythingEasiestFirst() {
        val titles = SongLibraryQuery().apply(library).map { it.exercise.title }

        assertEquals(listOf("Open strings", "Billie Jean", "Another One Bites the Dust", "Octave jumps"), titles)
    }

    @Test
    fun hardestFirstReversesTheDifficultyOrder() {
        val titles = SongLibraryQuery(sortOrder = SongSortOrder.HARDEST_FIRST).apply(library).map { it.exercise.title }

        assertEquals("Octave jumps", titles.first())
    }

    @Test
    fun titleOrderIsAlphabeticalIgnoringCase() {
        val titles = SongLibraryQuery(sortOrder = SongSortOrder.TITLE).apply(library).map { it.exercise.title }

        assertEquals(listOf("Another One Bites the Dust", "Billie Jean", "Octave jumps", "Open strings"), titles)
    }

    @Test
    fun mySongsShowsOnlyImportedSongs() {
        val titles = SongLibraryQuery(source = SongSource.MY_SONGS).apply(library).map { it.exercise.title }

        assertEquals(listOf("Billie Jean", "Another One Bites the Dust"), titles)
    }

    @Test
    fun builtInShowsOnlyShippedExercises() {
        val titles = SongLibraryQuery(source = SongSource.BUILT_IN).apply(library).map { it.exercise.title }

        assertEquals(listOf("Open strings", "Octave jumps"), titles)
    }

    @Test
    fun searchMatchesAnyPartOfTheTitleIgnoringCaseAndSpaces() {
        val titles = SongLibraryQuery(searchText = "  bites ").apply(library).map { it.exercise.title }

        assertEquals(listOf("Another One Bites the Dust"), titles)
    }
}
