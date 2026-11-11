package com.example.wavefret.practice.songimport

import com.example.wavefret.practice.Exercise

/** What happened when a shared file was imported. */
sealed class SongImportOutcome {

    /**
     * The file became a lesson.
     *
     * @property exercise The new lesson. Type: Exercise
     */
    data class Imported(val exercise: Exercise) : SongImportOutcome()

    /** The file is audio; its notes must be detected by transcription. */
    data object NeedsAudioTranscription : SongImportOutcome()

    /** The file is a PDF or photo of sheet music, which must be converted to MusicXML first. */
    data object SheetMusicImageNotSupported : SongImportOutcome()

    /**
     * The file could not be read.
     *
     * @property reason Why, in words the player can act on. Type: String
     */
    data class Failed(val reason: String) : SongImportOutcome()
}

/**
 * Turns any file the player shares into a lesson, routing it to the right
 * reader by its detected format: tab text, MusicXML (plain or .mxl),
 * Guitar Pro 7+, or MIDI. Audio and sheet-music images are reported back
 * so the screen can transcribe or explain instead of failing.
 *
 * @property songFileFormatDetector Works out what the file is. Type: SongFileFormatDetector
 * @property songImporter Reads ASCII tabs and WaveFret tab text. Type: SongImporter
 * @property musicXmlFileReader Unpacks .mxl archives. Type: MusicXmlFileReader
 * @property musicXmlBassParser Reads MusicXML scores. Type: MusicXmlBassParser
 * @property midiFileParser Reads MIDI files. Type: MidiFileParser
 * @property guitarProFileParser Reads Guitar Pro 7+ files. Type: GuitarProFileParser
 * @property noteEventsLessonBuilder Turns timed notes into a lesson. Type: NoteEventsLessonBuilder
 */
class SongFileImporter(
    private val songFileFormatDetector: SongFileFormatDetector,
    private val songImporter: SongImporter,
    private val musicXmlFileReader: MusicXmlFileReader,
    private val musicXmlBassParser: MusicXmlBassParser,
    private val midiFileParser: MidiFileParser,
    private val guitarProFileParser: GuitarProFileParser,
    private val noteEventsLessonBuilder: NoteEventsLessonBuilder
) {

    /**
     * @param fileName Display name of the shared file. Type: String
     * @param mimeType Type reported by the sharing app, or null. Type: String?
     * @param fileBytes Full contents of the file. Type: ByteArray
     * @param titleOverride Title typed by the player, or blank to use the file's. Type: String
     * @return What happened. Type: SongImportOutcome
     */
    fun import(fileName: String, mimeType: String?, fileBytes: ByteArray, titleOverride: String): SongImportOutcome {
        val nameWithoutExtension = fileName.substringBeforeLast('.').trim()
        return try {
            when (songFileFormatDetector.detect(fileName, mimeType, fileBytes)) {
                SongFileFormat.AUDIO -> SongImportOutcome.NeedsAudioTranscription
                SongFileFormat.SHEET_MUSIC_IMAGE -> SongImportOutcome.SheetMusicImageNotSupported
                SongFileFormat.UNSUPPORTED -> SongImportOutcome.Failed(
                    if (fileName.substringAfterLast('.').lowercase() in OLD_GUITAR_PRO_EXTENSIONS) OLD_GUITAR_PRO_REASON
                    else UNSUPPORTED_REASON
                )
                SongFileFormat.TAB_TEXT -> SongImportOutcome.Imported(
                    songImporter.import(fileBytes.toString(Charsets.UTF_8), titleOverride.ifBlank { nameWithoutExtension }, null)
                )
                SongFileFormat.MUSIC_XML -> SongImportOutcome.Imported(
                    withTitle(musicXmlBassParser.parse(musicXmlFileReader.readMusicXmlText(fileBytes)), titleOverride, nameWithoutExtension)
                )
                SongFileFormat.MIDI -> importTimedScore(midiFileParser.parse(fileBytes), titleOverride, nameWithoutExtension)
                SongFileFormat.GUITAR_PRO -> importTimedScore(guitarProFileParser.parse(fileBytes), titleOverride, nameWithoutExtension)
            }
        } catch (unreadableFile: IllegalArgumentException) {
            SongImportOutcome.Failed(unreadableFile.message ?: UNSUPPORTED_REASON)
        }
    }

    /**
     * @param timedScore Bass part read from the file. Type: TimedScore
     * @param titleOverride Title typed by the player, possibly blank. Type: String
     * @param fileTitle File name without extension. Type: String
     * @return The imported lesson. Type: SongImportOutcome
     */
    private fun importTimedScore(timedScore: TimedScore, titleOverride: String, fileTitle: String): SongImportOutcome {
        val title = titleOverride.trim().ifEmpty { fileTitle.ifEmpty { timedScore.title ?: SongImporter.DEFAULT_IMPORTED_TITLE } }
        return SongImportOutcome.Imported(noteEventsLessonBuilder.build(title, timedScore.tempoBpm, timedScore.noteEvents))
    }

    /**
     * @param exercise Lesson read from a score. Type: Exercise
     * @param titleOverride Title typed by the player, possibly blank. Type: String
     * @param fileTitle File name without extension. Type: String
     * @return The lesson with the best available title. Type: Exercise
     */
    private fun withTitle(exercise: Exercise, titleOverride: String, fileTitle: String): Exercise {
        val scoreTitle = exercise.title.takeUnless { it == SongImporter.DEFAULT_IMPORTED_TITLE }
        return exercise.copy(title = titleOverride.trim().ifEmpty { scoreTitle ?: fileTitle.ifEmpty { exercise.title } })
    }

    companion object {
        /** Why Guitar Pro 3-6 files can't be read. Type: String */
        const val OLD_GUITAR_PRO_REASON = "Guitar Pro 3-6 files aren't supported. Save it as .gp (Guitar Pro 7+) or MusicXML."

        private const val UNSUPPORTED_REASON = "This file type isn't supported"
        private val OLD_GUITAR_PRO_EXTENSIONS = setOf("gp3", "gp4", "gp5", "gpx")
    }
}
