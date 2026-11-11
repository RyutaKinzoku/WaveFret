package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.common.storage.ZipArchiveReader
import org.w3c.dom.Element
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Reads a Guitar Pro 7 or later file (`.gp`: a zip holding
 * `Content/score.gpif` XML) and extracts the bass part: the track named
 * "bass", else the one tuned lowest. Notes keep their timing from the
 * rhythm values; ties continue a note, rests advance time, and repeats are
 * played straight through once. Older formats (.gp3-.gp5, .gpx) are binary
 * and not supported.
 *
 * @property safeXmlDocumentParser Parses the score XML safely. Type: SafeXmlDocumentParser
 * @property zipArchiveReader Opens the .gp archive, tolerating its unusual zip headers. Type: ZipArchiveReader
 */
class GuitarProFileParser(
    private val safeXmlDocumentParser: SafeXmlDocumentParser = SafeXmlDocumentParser(),
    private val zipArchiveReader: ZipArchiveReader = ZipArchiveReader()
) {

    /**
     * @param guitarProBytes Full contents of the .gp file. Type: ByteArray
     * @return The bass part with its title and starting tempo. Type: TimedScore
     * @throws IllegalArgumentException If the file has no score or no bass notes.
     */
    fun parse(guitarProBytes: ByteArray): TimedScore {
        val gpifText = readScoreEntry(guitarProBytes)
        val root = safeXmlDocumentParser.parse(gpifText).documentElement
        val tempoBpm = readTempoBpm(root)
        val millisPerQuarter = MILLIS_PER_MINUTE / tempoBpm

        val tracks = root.childElement("Tracks")?.descendantElements("Track").orEmpty()
        require(tracks.isNotEmpty()) { "The Guitar Pro file has no tracks" }
        val bassTrackIndex = chooseBassTrackIndex(tracks)
        val tuningPitches = tracks[bassTrackIndex].descendantText("Pitches")
            ?.split(WHITESPACE)?.mapNotNull { it.toIntOrNull() }.orEmpty()

        val barsById = elementsById(root, "Bars", "Bar")
        val voicesById = elementsById(root, "Voices", "Voice")
        val beatsById = elementsById(root, "Beats", "Beat")
        val notesById = elementsById(root, "Notes", "Note")
        val rhythmQuartersById = elementsById(root, "Rhythms", "Rhythm").mapValues { (_, rhythm) -> quartersOf(rhythm) }

        val noteEvents = mutableListOf<NoteEvent>()
        var barStartQuarters = 0.0
        for (masterBar in root.childElement("MasterBars")?.descendantElements("MasterBar").orEmpty()) {
            val barId = masterBar.descendantText("Bars")?.split(WHITESPACE)?.getOrNull(bassTrackIndex)
            val firstVoiceId = barsById[barId]?.descendantText("Voices")?.split(WHITESPACE)?.firstOrNull { it != NO_VOICE }
            var beatStartQuarters = barStartQuarters
            for (beatId in voicesById[firstVoiceId]?.descendantText("Beats")?.split(WHITESPACE).orEmpty()) {
                val beat = beatsById[beatId] ?: continue
                val beatQuarters = rhythmQuartersById[beat.childElement("Rhythm")?.getAttribute("ref")] ?: 1.0
                val pluckedPitches = beat.descendantText("Notes")?.split(WHITESPACE).orEmpty()
                    .mapNotNull { noteId -> notesById[noteId] }
                    .filterNot(::isTieContinuation)
                    .mapNotNull { note -> pitchOf(note, tuningPitches) }
                pluckedPitches.minOrNull()?.let { lowestPitch ->
                    noteEvents += NoteEvent(
                        midiNumber = lowestPitch,
                        onsetMillis = (beatStartQuarters * millisPerQuarter).roundToLong(),
                        durationMillis = (beatQuarters * millisPerQuarter).roundToLong()
                    )
                }
                beatStartQuarters += beatQuarters
            }
            barStartQuarters += barLengthQuarters(masterBar.descendantText("Time"))
        }
        require(noteEvents.isNotEmpty()) { "The bass track has no notes" }

        return TimedScore(
            title = root.childElement("Score")?.descendantText("Title")?.takeIf { it.isNotBlank() },
            tempoBpm = tempoBpm,
            noteEvents = noteEvents
        )
    }

    /**
     * @param guitarProBytes Zip archive bytes. Type: ByteArray
     * @return Text of the archive's score.gpif entry. Type: String
     */
    private fun readScoreEntry(guitarProBytes: ByteArray): String {
        val scoreBytes = zipArchiveReader.readEntries(guitarProBytes).entries
            .firstOrNull { (entryPath, _) -> entryPath.endsWith(SCORE_ENTRY_NAME) }?.value
            ?: throw IllegalArgumentException("Not a Guitar Pro 7+ file: no score inside")
        return scoreBytes.toString(Charsets.UTF_8)
    }

    /**
     * @param root The GPIF root element. Type: Element
     * @return Starting tempo from the first tempo automation, or 120. Type: Int
     */
    private fun readTempoBpm(root: Element): Int {
        val tempoAutomation = root.childElement("MasterTrack")?.descendantElements("Automation").orEmpty()
            .firstOrNull { it.descendantText("Type") == "Tempo" }
        return tempoAutomation?.descendantText("Value")?.split(WHITESPACE)?.firstOrNull()
            ?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 } ?: DEFAULT_TEMPO_BPM
    }

    /**
     * @param tracks Track elements, in file order. Type: List<Element>
     * @return Index of the track named "bass", else the lowest-tuned one. Type: Int
     */
    private fun chooseBassTrackIndex(tracks: List<Element>): Int {
        val namedBassIndex = tracks.indexOfFirst { it.descendantText("Name")?.contains("bass", ignoreCase = true) == true }
        if (namedBassIndex >= 0) return namedBassIndex
        return tracks.indices.minBy { trackIndex ->
            tracks[trackIndex].descendantText("Pitches")?.split(WHITESPACE)?.mapNotNull { it.toIntOrNull() }?.minOrNull()
                ?: Int.MAX_VALUE
        }
    }

    /**
     * @param root The GPIF root element. Type: Element
     * @param containerTag Tag of the container, e.g. "Beats". Type: String
     * @param itemTag Tag of the items, e.g. "Beat". Type: String
     * @return Items with an id attribute, by id. Type: Map<String, Element>
     */
    private fun elementsById(root: Element, containerTag: String, itemTag: String): Map<String, Element> {
        val container = root.childElement(containerTag) ?: return emptyMap()
        return container.descendantElements(itemTag).filter { it.hasAttribute("id") }.associateBy { it.getAttribute("id") }
    }

    /**
     * @param rhythm A Rhythm element. Type: Element
     * @return Its length in quarter notes, with dots and tuplets applied. Type: Double
     */
    private fun quartersOf(rhythm: Element): Double {
        val baseQuarters = NOTE_VALUE_QUARTERS[rhythm.descendantText("NoteValue")] ?: 1.0
        val dotCount = rhythm.childElement("AugmentationDot")?.getAttribute("count")?.toIntOrNull() ?: 0
        val dottedQuarters = baseQuarters * (2 - 0.5.pow(dotCount))
        val tuplet = rhythm.childElement("PrimaryTuplet") ?: return dottedQuarters
        val tupletNotes = tuplet.getAttribute("num").toDoubleOrNull() ?: return dottedQuarters
        val tupletSpace = tuplet.getAttribute("den").toDoubleOrNull() ?: return dottedQuarters
        return dottedQuarters * tupletSpace / tupletNotes
    }

    /**
     * @param note A Note element. Type: Element
     * @return True if it only continues a tied note. Type: Boolean
     */
    private fun isTieContinuation(note: Element): Boolean = note.childElement("Tie")?.getAttribute("destination") == "true"

    /**
     * @param note A Note element. Type: Element
     * @param tuningPitches Open-string MIDI pitches, lowest string first. Type: List<Int>
     * @return The note's MIDI pitch, or null if it can't be worked out. Type: Int?
     */
    private fun pitchOf(note: Element, tuningPitches: List<Int>): Int? {
        val properties = note.descendantElements("Property").associateBy { it.getAttribute("name") }
        properties["Midi"]?.descendantText("Number")?.toIntOrNull()?.let { return it }
        val stringIndex = properties["String"]?.descendantText("String")?.toIntOrNull() ?: return null
        val fret = properties["Fret"]?.descendantText("Fret")?.toIntOrNull() ?: return null
        return tuningPitches.getOrNull(stringIndex)?.plus(fret)
    }

    /**
     * @param timeSignature Text such as "4/4" or "6/8", or null. Type: String?
     * @return The bar's length in quarter notes (4 if unknown). Type: Double
     */
    private fun barLengthQuarters(timeSignature: String?): Double {
        val beatCount = timeSignature?.substringBefore('/')?.toDoubleOrNull() ?: return COMMON_TIME_QUARTERS
        val beatUnit = timeSignature.substringAfter('/').toDoubleOrNull() ?: return COMMON_TIME_QUARTERS
        return beatCount * QUARTERS_PER_WHOLE / beatUnit
    }

    private companion object {
        const val SCORE_ENTRY_NAME = "score.gpif"
        const val NO_VOICE = "-1"
        const val DEFAULT_TEMPO_BPM = 120
        const val MILLIS_PER_MINUTE = 60_000.0
        const val COMMON_TIME_QUARTERS = 4.0
        const val QUARTERS_PER_WHOLE = 4.0
        val WHITESPACE = Regex("\\s+")
        val NOTE_VALUE_QUARTERS = mapOf(
            "Whole" to 4.0, "Half" to 2.0, "Quarter" to 1.0, "Eighth" to 0.5,
            "16th" to 0.25, "32nd" to 0.125, "64th" to 0.0625, "128th" to 0.03125
        )
    }
}
