package com.example.wavefret.practice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.IntentCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.practice.progression.ExercisePickerItem
import com.example.wavefret.practice.progression.ExercisePickerItemsBuilder
import com.example.wavefret.practice.progression.SongLibraryQuery
import com.example.wavefret.practice.progression.SongSortOrder
import com.example.wavefret.practice.progression.SongSource
import com.example.wavefret.practice.songimport.AsciiTabParser
import com.example.wavefret.practice.songimport.FretboardPositionFinder
import com.example.wavefret.practice.songimport.GuitarProFileParser
import com.example.wavefret.practice.songimport.MidiFileParser
import com.example.wavefret.practice.songimport.MusicXmlBassParser
import com.example.wavefret.practice.songimport.MusicXmlFileReader
import com.example.wavefret.practice.songimport.NoteEventsLessonBuilder
import com.example.wavefret.practice.songimport.SongFileFormatDetector
import com.example.wavefret.practice.songimport.SongFileImporter
import com.example.wavefret.practice.songimport.SongImportOutcome
import com.example.wavefret.practice.songimport.SongImporter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The song library: every built-in exercise and imported song, searchable,
 * filterable (all / my songs / exercises) and sorted by difficulty by
 * default. "Add a song" opens files (Guitar Pro, MusicXML, MIDI, tab
 * text), takes a pasted tab, or explains how to convert a PDF or photo.
 * Files and text shared into WaveFret from other apps land here too.
 * Tapping a song opens it in practice.
 *
 * Android framework glue — filtering lives in the tested [SongLibraryQuery],
 * reading in the tested [SongFileImporter] — so it is verified on-device.
 */
class SongLibraryActivity : AppCompatActivity() {

    private lateinit var rootView: ViewGroup
    private lateinit var songsRecyclerView: RecyclerView
    private lateinit var emptyStateView: View
    private lateinit var sortButton: MaterialButton
    private lateinit var addSongButton: ExtendedFloatingActionButton

    private val appContainer = AppContainer(this)
    private val exercisePickerItemsBuilder by lazy { ExercisePickerItemsBuilder(appContainer.exerciseProgression) }
    private val songFileImporter by lazy {
        val tuning = BassTunings.STANDARD
        val fretboardPositionFinder = FretboardPositionFinder(tuning)
        SongFileImporter(
            songFileFormatDetector = SongFileFormatDetector(),
            songImporter = SongImporter(AsciiTabParser(tuning), appContainer.tabTextParser),
            musicXmlFileReader = MusicXmlFileReader(),
            musicXmlBassParser = MusicXmlBassParser(tuning, fretboardPositionFinder),
            midiFileParser = MidiFileParser(),
            guitarProFileParser = GuitarProFileParser(),
            noteEventsLessonBuilder = NoteEventsLessonBuilder(tuning, fretboardPositionFinder)
        )
    }

    private var libraryItems: List<ExercisePickerItem> = emptyList()
    private var songLibraryQuery = SongLibraryQuery()

    private val openSongFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { songFileUri -> songFileUri?.let(::importSongFile) }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_song_library)
        rootView = findViewById(R.id.songLibraryRoot)
        songsRecyclerView = findViewById(R.id.songLibraryRecyclerView)
        emptyStateView = findViewById(R.id.songLibraryEmptyState)
        sortButton = findViewById(R.id.songLibrarySortButton)
        addSongButton = findViewById(R.id.songLibraryAddSongButton)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.song_library_title)
        topAppBar.setNavigationOnClickListener { finish() }
        findViewById<View>(R.id.songLibraryContent).applySystemBarPadding(applyTop = true, applyBottom = true)
        applyBottomInsetAsMargin(addSongButton)

        songsRecyclerView.layoutManager = LinearLayoutManager(this)
        findViewById<TextInputEditText>(R.id.songLibrarySearchEditText).doAfterTextChanged { searchText ->
            songLibraryQuery = songLibraryQuery.copy(searchText = searchText?.toString().orEmpty())
            renderLibrary()
        }
        findViewById<ChipGroup>(R.id.songLibrarySourceChipGroup).setOnCheckedStateChangeListener { _, checkedIds ->
            songLibraryQuery = songLibraryQuery.copy(source = sourceForChip(checkedIds.firstOrNull()))
            renderLibrary()
        }
        sortButton.setOnClickListener { showSortMenu() }
        addSongButton.setOnClickListener { showAddSongSheet() }

        reloadLibrary()
        if (savedInstanceState == null) handleIncomingIntent(intent)
    }

    /**
     * @param intent The new intent, e.g. a second file shared while open. Type: Intent
     * @return Unit
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Refreshes best scores and mastery after coming back from practice.
     *
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        reloadLibrary()
    }

    /**
     * Imports whatever another app shared with WaveFret, or opens the
     * add-song sheet when asked to.
     *
     * @param incomingIntent Intent that started or re-entered the screen. Type: Intent
     * @return Unit
     */
    private fun handleIncomingIntent(incomingIntent: Intent) {
        when {
            incomingIntent.action == Intent.ACTION_SEND -> {
                val sharedUri = IntentCompat.getParcelableExtra(incomingIntent, Intent.EXTRA_STREAM, Uri::class.java)
                val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    sharedUri != null -> importSongFile(sharedUri)
                    !sharedText.isNullOrBlank() -> showPasteTabDialog(prefilledTab = sharedText)
                }
            }
            incomingIntent.action == Intent.ACTION_VIEW -> incomingIntent.data?.let(::importSongFile)
            incomingIntent.getBooleanExtra(EXTRA_SHOW_ADD_SONG, false) -> showAddSongSheet()
        }
    }

    /**
     * @return Unit
     */
    private fun reloadLibrary() {
        val importedTitles = appContainer.importedExercisesRepository.listExercises().map { it.title }.toSet()
        libraryItems = exercisePickerItemsBuilder.build(
            appContainer.allExercises(), appContainer.practiceResultsRepository.listResults(), importedTitles
        )
        renderLibrary()
    }

    /**
     * @return Unit
     */
    private fun renderLibrary() {
        val visibleItems = songLibraryQuery.apply(libraryItems)
        songsRecyclerView.adapter = ExercisePickerAdapter(
            pickerItems = visibleItems,
            describeExercise = ::describeSong,
            onExerciseChosen = { libraryItem -> startActivity(PracticeActivity.intentFor(this, libraryItem.exercise.title)) },
            onDeleteClicked = { libraryItem -> confirmDeleteSong(libraryItem.exercise) }
        )
        emptyStateView.isVisible = visibleItems.isEmpty()
        val sortLabel = getString(sortLabelFor(songLibraryQuery.sortOrder))
        sortButton.text = sortLabel
        sortButton.contentDescription = getString(R.string.song_library_sort_description, sortLabel)
    }

    /**
     * @param libraryItem Song to describe. Type: ExercisePickerItem
     * @return Secondary line such as "Beginner · 60 BPM · 8 notes · Best 85%". Type: String
     */
    private fun describeSong(libraryItem: ExercisePickerItem): String {
        val detailParts = mutableListOf(
            getString(labelResourceFor(libraryItem.difficultyLevel)),
            getString(R.string.practice_exercise_tempo, libraryItem.exercise.tempoBpm),
            resources.getQuantityString(
                R.plurals.practice_exercise_notes, libraryItem.exercise.noteCount, libraryItem.exercise.noteCount
            )
        )
        libraryItem.bestScorePercent?.let { detailParts += getString(R.string.practice_exercise_best, it) }
        return detailParts.joinToString(DETAIL_SEPARATOR)
    }

    /**
     * @return Unit
     */
    private fun showSortMenu() {
        PopupMenu(this, sortButton).apply {
            SongSortOrder.entries.forEach { sortOrder -> menu.add(0, sortOrder.ordinal, sortOrder.ordinal, sortLabelFor(sortOrder)) }
            setOnMenuItemClickListener { menuItem ->
                songLibraryQuery = songLibraryQuery.copy(sortOrder = SongSortOrder.entries[menuItem.itemId])
                renderLibrary()
                true
            }
            show()
        }
    }

    /**
     * @return Unit
     */
    private fun showAddSongSheet() {
        val addSongSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_song, rootView, false)
        bindAddSongOption(sheetView.findViewById(R.id.addSongOpenFileOption), R.drawable.ic_file_upload,
            R.string.add_song_open_file_title, R.string.add_song_open_file_body) {
            addSongSheet.dismiss()
            openSongFileLauncher.launch(arrayOf("*/*"))
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongPasteTabOption), R.drawable.ic_content_paste,
            R.string.add_song_paste_title, R.string.add_song_paste_body) {
            addSongSheet.dismiss()
            showPasteTabDialog(prefilledTab = null)
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongSheetMusicOption), R.drawable.ic_image,
            R.string.add_song_sheet_music_title, R.string.add_song_sheet_music_body) {
            addSongSheet.dismiss()
            showSheetMusicHelp()
        }
        addSongSheet.setContentView(sheetView)
        addSongSheet.show()
    }

    /**
     * @param optionView One option row in the add-song sheet. Type: View
     * @param iconResource Option icon. Type: Int
     * @param titleResource Option title. Type: Int
     * @param bodyResource Option explanation. Type: Int
     * @param onChosen Called when the row is tapped. Type: () -> Unit
     * @return Unit
     */
    private fun bindAddSongOption(
        optionView: View,
        @DrawableRes iconResource: Int,
        @StringRes titleResource: Int,
        @StringRes bodyResource: Int,
        onChosen: () -> Unit
    ) {
        optionView.findViewById<ImageView>(R.id.addSongOptionIcon).setImageResource(iconResource)
        optionView.findViewById<TextView>(R.id.addSongOptionTitle).setText(titleResource)
        optionView.findViewById<TextView>(R.id.addSongOptionBody).setText(bodyResource)
        optionView.setOnClickListener { onChosen() }
    }

    /**
     * @param prefilledTab Tab text shared from another app, or null. Type: String?
     * @return Unit
     */
    private fun showPasteTabDialog(prefilledTab: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_song, rootView, false)
        val titleEditText = dialogView.findViewById<TextInputEditText>(R.id.importSongTitleEditText)
        val tempoEditText = dialogView.findViewById<TextInputEditText>(R.id.importSongTempoEditText)
        val tabEditText = dialogView.findViewById<TextInputEditText>(R.id.importSongTabEditText)
        prefilledTab?.let(tabEditText::setText)
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_content_paste)
            .setTitle(R.string.add_song_paste_title)
            .setView(dialogView)
            .setPositiveButton(R.string.import_song_confirm) { _, _ ->
                val songImporter = SongImporter(AsciiTabParser(BassTunings.STANDARD), appContainer.tabTextParser)
                try {
                    saveImportedSong(
                        songImporter.import(
                            pastedText = tabEditText.text.toString(),
                            title = titleEditText.text.toString(),
                            tempoBpm = tempoEditText.text.toString().toIntOrNull()?.takeIf { it > 0 }
                        )
                    )
                } catch (unreadableTab: IllegalArgumentException) {
                    showImportFailure(unreadableTab.message.orEmpty())
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Explains how to turn a PDF or photo of sheet music into MusicXML.
     *
     * @return Unit
     */
    private fun showSheetMusicHelp() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_image)
            .setTitle(R.string.sheet_music_help_title)
            .setMessage(R.string.sheet_music_help_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /**
     * Reads a chosen or shared file off the main thread and imports it.
     *
     * @param songFileUri File to import. Type: Uri
     * @return Unit
     */
    private fun importSongFile(songFileUri: Uri) {
        val importingSnackbar = Snackbar.make(rootView, R.string.importing_song, Snackbar.LENGTH_INDEFINITE)
            .setAnchorView(addSongButton)
        importingSnackbar.show()
        lifecycleScope.launch {
            val importOutcome = withContext(Dispatchers.IO) {
                try {
                    val fileName = displayNameOf(songFileUri)
                    val mimeType = contentResolver.getType(songFileUri)
                    val fileBytes = contentResolver.openInputStream(songFileUri)?.use { it.readBytes() }
                        ?: return@withContext SongImportOutcome.Failed(getString(R.string.import_song_unreadable_file))
                    songFileImporter.import(fileName, mimeType, fileBytes, titleOverride = "")
                } catch (unreadableFile: IOException) {
                    SongImportOutcome.Failed(getString(R.string.import_song_unreadable_file))
                } catch (deniedFile: SecurityException) {
                    SongImportOutcome.Failed(getString(R.string.import_song_unreadable_file))
                }
            }
            importingSnackbar.dismiss()
            when (importOutcome) {
                is SongImportOutcome.Imported -> saveImportedSong(importOutcome.exercise)
                SongImportOutcome.SheetMusicImageNotSupported -> showSheetMusicHelp()
                SongImportOutcome.NeedsAudioTranscription -> showImportFailure(getString(R.string.import_song_audio_not_yet))
                is SongImportOutcome.Failed -> showImportFailure(importOutcome.reason)
            }
        }
    }

    /**
     * @param songFileUri File to name. Type: Uri
     * @return The file's display name, or its last path segment. Type: String
     */
    private fun displayNameOf(songFileUri: Uri): String {
        contentResolver.query(songFileUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.let { return it }
        }
        return songFileUri.lastPathSegment.orEmpty()
    }

    /**
     * @param importedSong Lesson to store. Type: Exercise
     * @return Unit
     */
    private fun saveImportedSong(importedSong: Exercise) {
        appContainer.importedExercisesRepository.save(importedSong)
        reloadLibrary()
        Snackbar.make(rootView, getString(R.string.import_song_saved, importedSong.title), Snackbar.LENGTH_LONG)
            .setAnchorView(addSongButton)
            .setAction(R.string.practice_start) { startActivity(PracticeActivity.intentFor(this, importedSong.title)) }
            .show()
    }

    /**
     * @param reason Why the import failed. Type: String
     * @return Unit
     */
    private fun showImportFailure(reason: String) {
        Snackbar.make(rootView, getString(R.string.import_song_failed, reason), Snackbar.LENGTH_LONG)
            .setAnchorView(addSongButton)
            .show()
    }

    /**
     * Asks before removing an imported song; its practice history is kept.
     *
     * @param song Imported song to delete. Type: Exercise
     * @return Unit
     */
    private fun confirmDeleteSong(song: Exercise) {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.delete_song_title)
            .setMessage(getString(R.string.delete_song_message, song.title))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                appContainer.importedExercisesRepository.delete(song.title)
                reloadLibrary()
            }
            .show()
    }

    /**
     * Keeps the floating button above the navigation bar.
     *
     * @param floatingButton Button to lift. Type: View
     * @return Unit
     */
    private fun applyBottomInsetAsMargin(floatingButton: View) {
        val baseMargin = (floatingButton.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(floatingButton) { view, windowInsets ->
            val navigationBarInset = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.ime()
            ).bottom
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = baseMargin + navigationBarInset
            view.requestLayout()
            windowInsets
        }
    }

    /**
     * @param checkedChipId Selected source chip, or null. Type: Int?
     * @return The matching source filter. Type: SongSource
     */
    private fun sourceForChip(checkedChipId: Int?): SongSource = when (checkedChipId) {
        R.id.songLibraryMySongsChip -> SongSource.MY_SONGS
        R.id.songLibraryBuiltInChip -> SongSource.BUILT_IN
        else -> SongSource.ALL
    }

    /**
     * @param sortOrder Order to name. Type: SongSortOrder
     * @return String resource naming it. Type: Int
     */
    @StringRes
    private fun sortLabelFor(sortOrder: SongSortOrder): Int = when (sortOrder) {
        SongSortOrder.EASIEST_FIRST -> R.string.song_library_sort_easiest
        SongSortOrder.HARDEST_FIRST -> R.string.song_library_sort_hardest
        SongSortOrder.TITLE -> R.string.song_library_sort_title
    }

    companion object {
        private const val EXTRA_SHOW_ADD_SONG = "com.example.wavefret.practice.SHOW_ADD_SONG"
        private const val DETAIL_SEPARATOR = " · "

        /**
         * @param context Context starting the screen. Type: Context
         * @param showAddSong Whether to open the add-song sheet straight away. Type: Boolean
         * @return An intent that opens the song library. Type: Intent
         */
        fun intentFor(context: Context, showAddSong: Boolean): Intent {
            return Intent(context, SongLibraryActivity::class.java).putExtra(EXTRA_SHOW_ADD_SONG, showAddSong)
        }
    }
}
