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
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.MediaCodecAudioDecoder
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.textrecognition.DocumentPageRenderer
import com.example.wavefret.common.textrecognition.MlKitSymbolRecognizer
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.practice.playalong.LessonAudioStore
import com.example.wavefret.practice.playalong.ListenActivity
import com.example.wavefret.practice.playalong.YouTubeLinkParser
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
import com.example.wavefret.practice.songimport.OnDeviceLessonTranscriber
import com.example.wavefret.practice.songimport.SongFileFormat
import com.example.wavefret.practice.songimport.SongFileFormatDetector
import com.example.wavefret.practice.songimport.RecognizedTabTextBuilder
import com.example.wavefret.practice.songimport.SongFileImporter
import com.example.wavefret.practice.songimport.SongImportOutcome
import com.example.wavefret.practice.songimport.SongImporter
import com.example.wavefret.practice.songsearch.PendingSongTitle
import com.example.wavefret.practice.songsearch.PendingSongTitleStore
import com.example.wavefret.practice.songsearch.SongSearchLinks
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The song library: every built-in exercise and imported song, searchable,
 * filterable (all / my songs / exercises) and sorted by difficulty by
 * default. "Add a song" opens files (Guitar Pro, MusicXML, MIDI, tab
 * text), turns audio into a draft lesson (bass-only audio with YIN, full
 * songs with the basic-pitch model) that opens in the lesson editor, takes
 * a pasted tab, or reads the tab in a PDF or photo.
 * Files and text shared into WaveFret from other apps land here too.
 * Tapping a song opens it in practice.
 *
 * Android framework glue — filtering lives in the tested [SongLibraryQuery],
 * reading in the tested [SongFileImporter] — so it is verified on-device.
 */
class SongLibraryActivity : WaveFretActivity() {

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
                val sharedYouTubeVideoId = sharedText?.let(YouTubeLinkParser()::videoIdFrom)
                when {
                    sharedUri != null -> importSongFile(sharedUri)
                    sharedYouTubeVideoId != null -> startActivity(
                        ListenActivity.intentFor(this, sharedYouTubeVideoId, songTitle = takeSearchedSongTitle())
                    )
                    !sharedText.isNullOrBlank() -> showPasteTabDialog(prefilledTab = sharedText, prefilledTitle = takeSearchedSongTitle())
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
            onDeleteClicked = { libraryItem -> confirmDeleteSong(libraryItem.exercise) },
            onEditClicked = { libraryItem ->
                startActivity(LessonEditorActivity.intentFor(this, libraryItem.exercise, libraryItem.exercise.title))
            }
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
        bindAddSongOption(sheetView.findViewById(R.id.addSongFindOption), R.drawable.ic_library_music,
            R.string.add_song_find_title, R.string.add_song_find_body) {
            addSongSheet.dismiss()
            showFindSongDialog()
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongOpenFileOption), R.drawable.ic_file_upload,
            R.string.add_song_open_file_title, R.string.add_song_open_file_body) {
            addSongSheet.dismiss()
            openSongFileLauncher.launch(arrayOf("*/*"))
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongAudioOption), R.drawable.ic_music_note,
            R.string.add_song_audio_title, R.string.add_song_audio_body) {
            addSongSheet.dismiss()
            openSongFileLauncher.launch(arrayOf("audio/*", "video/mp4"))
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongYouTubeOption), R.drawable.ic_play,
            R.string.add_song_youtube_title, R.string.add_song_youtube_body) {
            addSongSheet.dismiss()
            showYouTubeLinkDialog()
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongListenOption), R.drawable.ic_mic,
            R.string.add_song_listen_title, R.string.add_song_listen_body) {
            addSongSheet.dismiss()
            startActivity(ListenActivity.intentFor(this, youTubeVideoId = null))
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongPasteTabOption), R.drawable.ic_content_paste,
            R.string.add_song_paste_title, R.string.add_song_paste_body) {
            addSongSheet.dismiss()
            showPasteTabDialog(prefilledTab = null)
        }
        bindAddSongOption(sheetView.findViewById(R.id.addSongSheetMusicOption), R.drawable.ic_image,
            R.string.add_song_sheet_music_title, R.string.add_song_sheet_music_body) {
            addSongSheet.dismiss()
            openSongFileLauncher.launch(arrayOf("image/*", "application/pdf"))
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
    private fun showPasteTabDialog(prefilledTab: String?, prefilledTitle: String? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_song, rootView, false)
        val titleEditText = dialogView.findViewById<TextInputEditText>(R.id.importSongTitleEditText)
        val tempoEditText = dialogView.findViewById<TextInputEditText>(R.id.importSongTempoEditText)
        val tabEditText = dialogView.findViewById<TextInputEditText>(R.id.importSongTabEditText)
        prefilledTab?.let(tabEditText::setText)
        prefilledTitle?.let(titleEditText::setText)
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
     * Asks for a song name and opens a YouTube search for bass covers or a
     * web search for bass tabs; the name is remembered so what the player
     * shares back is named after the song.
     *
     * @return Unit
     */
    private fun showFindSongDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_find_song, rootView, false)
        val nameLayout = dialogView.findViewById<TextInputLayout>(R.id.findSongNameLayout)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.findSongNameEditText)
        val findSongDialog = MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_library_music)
            .setTitle(R.string.add_song_find_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        /** Opens one kind of search for the typed song. */
        fun search(searchUrlFor: (String) -> String) {
            val songName = nameEditText.text?.toString().orEmpty().trim()
            if (songName.isEmpty()) {
                nameLayout.error = getString(R.string.find_song_name_required)
                return
            }
            PendingSongTitleStore(this).save(PendingSongTitle(songName, appContainer.clock.currentTimeMillis()))
            findSongDialog.dismiss()
            try {
                startActivity(Intent(Intent.ACTION_VIEW, searchUrlFor(songName).toUri()))
            } catch (noBrowser: android.content.ActivityNotFoundException) {
                showImportFailure(getString(R.string.find_song_no_app))
            }
        }
        dialogView.findViewById<View>(R.id.findSongYouTubeButton).setOnClickListener {
            search(SongSearchLinks()::youTubeBassCoverSearch)
        }
        dialogView.findViewById<View>(R.id.findSongTabButton).setOnClickListener {
            search(SongSearchLinks()::webBassTabSearch)
        }
        findSongDialog.show()
    }

    /** @return The song name from a recent "Find a song" search, or null. Type: String? */
    private fun takeSearchedSongTitle(): String? =
        PendingSongTitleStore(this).takeFreshTitle(appContainer.clock.currentTimeMillis())

    /**
     * Asks for a YouTube link and opens Listen mode with that video.
     *
     * @return Unit
     */
    private fun showYouTubeLinkDialog() {
        val linkLayout = layoutInflater.inflate(R.layout.dialog_youtube_link, rootView, false) as TextInputLayout
        val linkEditText = requireNotNull(linkLayout.editText)
        val linkDialog = MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_play)
            .setTitle(R.string.add_song_youtube_title)
            .setView(linkLayout)
            .setPositiveButton(R.string.next, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        linkDialog.setOnShowListener {
            linkDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val videoId = YouTubeLinkParser().videoIdFrom(linkEditText.text.toString())
                if (videoId == null) {
                    linkLayout.error = getString(R.string.youtube_link_invalid)
                } else {
                    linkDialog.dismiss()
                    startActivity(ListenActivity.intentFor(this, videoId))
                }
            }
        }
        linkDialog.show()
    }

    /**
     * Reads the tab in a photo or PDF with on-device text recognition, then
     * opens the paste dialog with what was read so the player can check it,
     * fix it and name it.
     *
     * @param documentUri Photo or PDF to read. Type: Uri
     * @param fileName Its display name, used as the suggested title. Type: String
     * @param mimeType Its type, if known. Type: String?
     * @return Unit
     */
    private fun readTabFromDocument(documentUri: Uri, fileName: String, mimeType: String?) {
        val progressView = layoutInflater.inflate(R.layout.dialog_transcribing, rootView, false)
        progressView.findViewById<TextView>(R.id.transcribingStepTextView).setText(R.string.reading_tab_step)
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reading_tab_title)
            .setView(progressView)
            .setCancelable(false)
            .create()
        val isPdf = mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)
        val readingJob = lifecycleScope.launch {
            val recognizedTabText = try {
                withContext(Dispatchers.Default) {
                    val pages = mutableListOf<List<RecognizedSymbol>>()
                    MlKitSymbolRecognizer().use { symbolRecognizer ->
                        DocumentPageRenderer(contentResolver).forEachPage(documentUri, isPdf) { pageBitmap ->
                            pages += symbolRecognizer.recognize(pageBitmap)
                        }
                    }
                    RecognizedTabTextBuilder().build(pages)
                }
            } catch (unreadableDocument: IOException) {
                null
            } catch (deniedDocument: SecurityException) {
                null
            } catch (recognitionFailure: com.google.mlkit.common.MlKitException) {
                null
            }
            progressDialog.dismiss()
            val suggestedTitle = takeSearchedSongTitle() ?: fileName.substringBeforeLast('.')
            when {
                recognizedTabText == null -> showImportFailure(getString(R.string.import_song_unreadable_file))
                containsReadableTab(recognizedTabText) -> showPasteTabDialog(recognizedTabText, suggestedTitle)
                else -> showNoTabFoundDialog(recognizedTabText, suggestedTitle)
            }
        }
        progressDialog.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ ->
            readingJob.cancel()
        }
        progressDialog.show()
    }

    /**
     * @param tabText Text that may contain a bass tab. Type: String
     * @return True if at least one note can be read from it. Type: Boolean
     */
    private fun containsReadableTab(tabText: String): Boolean =
        runCatching { AsciiTabParser(BassTunings.STANDARD).parseNotes(tabText) }.isSuccess

    /**
     * Explains that no tab was found (for example, the picture is sheet
     * music on a staff) and offers to edit what was read anyway.
     *
     * @param recognizedText Everything that was read from the picture. Type: String
     * @param suggestedTitle Title to prefill. Type: String
     * @return Unit
     */
    private fun showNoTabFoundDialog(recognizedText: String, suggestedTitle: String) {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_image)
            .setTitle(R.string.no_tab_found_title)
            .setMessage(R.string.no_tab_found_message)
            .setNegativeButton(R.string.ok, null)
        if (recognizedText.isNotBlank()) {
            dialogBuilder.setPositiveButton(R.string.no_tab_found_edit) { _, _ -> showPasteTabDialog(recognizedText, suggestedTitle) }
        }
        dialogBuilder.show()
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
            val fileName = withContext(Dispatchers.IO) { runCatching { displayNameOf(songFileUri) }.getOrDefault("") }
            val mimeType = contentResolver.getType(songFileUri)
            val leadingBytes = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openInputStream(songFileUri)?.use { inputStream ->
                    val leadingBuffer = ByteArray(LEADING_BYTES_COUNT)
                    leadingBuffer.copyOf(maxOf(0, inputStream.read(leadingBuffer)))
                } }.getOrNull()
            } ?: ByteArray(0)
            if (SongFileFormatDetector().detect(fileName, mimeType, leadingBytes) == SongFileFormat.AUDIO) {
                importingSnackbar.dismiss()
                askWhatIsInTheAudio(songFileUri, fileName)
                return@launch
            }
            val importOutcome = withContext(Dispatchers.IO) {
                try {
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
                SongImportOutcome.NeedsTextRecognition -> readTabFromDocument(songFileUri, fileName, mimeType)
                SongImportOutcome.NeedsAudioTranscription -> askWhatIsInTheAudio(songFileUri, fileName)
                is SongImportOutcome.Failed -> showImportFailure(importOutcome.reason)
            }
        }
    }

    /**
     * Asks whether the audio is bass only or a full song, which decides the
     * note detector, then transcribes it.
     *
     * @param audioUri Audio file to transcribe. Type: Uri
     * @param fileName Its display name, used as the lesson title. Type: String
     * @return Unit
     */
    private fun askWhatIsInTheAudio(audioUri: Uri, fileName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.audio_kind_title)
            .setItems(arrayOf(getString(R.string.audio_kind_bass_only), getString(R.string.audio_kind_full_song))) { _, choice ->
                transcribeAudio(audioUri, fileName.substringBeforeLast('.').ifBlank { getString(R.string.add_song_audio_title) },
                    isFullSong = choice == FULL_SONG_CHOICE)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Decodes and transcribes audio off the main thread behind a cancellable
     * progress dialog, then opens the draft lesson in the editor.
     *
     * @param audioUri Audio file to transcribe. Type: Uri
     * @param title Draft lesson title. Type: String
     * @param isFullSong True to use basic-pitch (full mixes), false for YIN (bass only). Type: Boolean
     * @return Unit
     */
    private fun transcribeAudio(audioUri: Uri, title: String, isFullSong: Boolean) {
        val progressView = layoutInflater.inflate(R.layout.dialog_transcribing, rootView, false)
        val stepTextView = progressView.findViewById<TextView>(R.id.transcribingStepTextView)
        stepTextView.setText(R.string.transcribing_decoding)
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.transcribing_title)
            .setView(progressView)
            .setCancelable(false)
            .create()
        val transcriptionJob = lifecycleScope.launch {
            try {
                val decodedAudio = withContext(Dispatchers.IO) { MediaCodecAudioDecoder(this@SongLibraryActivity).decode(audioUri) }
                stepTextView.setText(R.string.transcribing_detecting)
                val draftLesson = withContext(Dispatchers.Default) {
                    OnDeviceLessonTranscriber(this@SongLibraryActivity)
                        .transcribe(title, decodedAudio.samples, decodedAudio.sampleRateHz, isFullSong)
                }
                val storedAudioName = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(audioUri)?.use { audioStream ->
                        LessonAudioStore(appContainer.appStorageDirectoryProvider).store(displayNameOf(audioUri), audioStream)
                    }
                }
                val playableDraft = storedAudioName?.let { draftLesson.copy(mediaSource = LessonMediaSource.StoredAudio(it)) } ?: draftLesson
                progressDialog.dismiss()
                startActivity(LessonEditorActivity.intentFor(this@SongLibraryActivity, playableDraft, originalTitle = null))
            } catch (transcriptionFailure: IllegalArgumentException) {
                progressDialog.dismiss()
                showTranscriptionFailure(transcriptionFailure.message.orEmpty())
            } catch (securityFailure: SecurityException) {
                progressDialog.dismiss()
                showTranscriptionFailure(getString(R.string.import_song_unreadable_file))
            }
        }
        progressDialog.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ ->
            transcriptionJob.cancel()
        }
        progressDialog.show()
    }

    /**
     * @param reason Why transcription failed. Type: String
     * @return Unit
     */
    private fun showTranscriptionFailure(reason: String) {
        Snackbar.make(rootView, getString(R.string.transcription_failed, reason), Snackbar.LENGTH_LONG)
            .setAnchorView(addSongButton)
            .show()
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
                (song.mediaSource as? LessonMediaSource.StoredAudio)?.let { storedAudio ->
                    LessonAudioStore(appContainer.appStorageDirectoryProvider).delete(storedAudio.fileName)
                }
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
        private const val LEADING_BYTES_COUNT = 512
        private const val FULL_SONG_CHOICE = 1

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
