package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.TabTextParser
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the first part of an uncompressed MusicXML (`score-partwise`) file
 * as a bass line. Notes written with tab (`<technical><string>/<fret>`)
 * keep their position; plain pitches are placed on the fretboard by
 * [FretboardPositionFinder]. Rests, extra chord tones and tied
 * continuations are skipped, since practice follows one plucked note at a
 * time. A `<transpose>` (bass parts are usually written an octave above
 * sounding pitch) is applied.
 *
 * @property tuning Tuning the bass line is played in. Type: BassTuning
 * @property fretboardPositionFinder Places pitches that come without tab. Type: FretboardPositionFinder
 */
class MusicXmlBassParser(
    private val tuning: BassTuning,
    private val fretboardPositionFinder: FretboardPositionFinder
) {

    /**
     * @param musicXmlText Full MusicXML document text. Type: String
     * @return The bass line as an exercise. Type: Exercise
     * @throws IllegalArgumentException If the XML is malformed or holds no playable notes.
     */
    fun parse(musicXmlText: String): Exercise {
        val scoreDocument = parseDocument(musicXmlText)
        val firstPart = scoreDocument.getElementsByTagName("part").item(0) as? Element
            ?: throw IllegalArgumentException("MusicXML has no part")
        val semitoneTransposition = semitoneTranspositionOf(firstPart)

        val notes = mutableListOf<TabNote>()
        val noteElements = firstPart.getElementsByTagName("note")
        for (noteElementIndex in 0 until noteElements.length) {
            val noteElement = noteElements.item(noteElementIndex) as Element
            if (!isPluckedNote(noteElement)) continue
            notes += tabNoteFor(noteElement, semitoneTransposition, notes.lastOrNull())
        }
        require(notes.isNotEmpty()) { "MusicXML part contains no playable notes" }

        return Exercise(
            title = titleOf(scoreDocument),
            tuning = tuning,
            tempoBpm = tempoOf(scoreDocument),
            notes = notes
        )
    }

    /**
     * Parses XML without fetching external DTDs or expanding entities.
     * Leading whitespace and a byte-order mark are dropped first, since an
     * XML declaration must be the very first thing in a document.
     *
     * @param musicXmlText Document text. Type: String
     * @return The parsed DOM document. Type: Document
     */
    private fun parseDocument(musicXmlText: String): Document {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            isExpandEntityReferences = false
            isNamespaceAware = false
            runCatching { setFeature(LOAD_EXTERNAL_DTD_FEATURE, false) }
        }
        val documentText = musicXmlText.trimStart(BYTE_ORDER_MARK, ' ', '\t', '\r', '\n')
        return try {
            documentBuilderFactory.newDocumentBuilder().parse(InputSource(StringReader(documentText)))
        } catch (malformedXml: SAXException) {
            throw IllegalArgumentException("Not valid MusicXML: ${malformedXml.message}", malformedXml)
        }
    }

    /**
     * @param noteElement A `<note>` element. Type: Element
     * @return True for a newly plucked note: not a rest, chord tone or tie continuation. Type: Boolean
     */
    private fun isPluckedNote(noteElement: Element): Boolean {
        if (childElement(noteElement, "rest") != null) return false
        if (childElement(noteElement, "chord") != null) return false
        val tieElements = noteElement.getElementsByTagName("tie")
        val isTieContinuation = (0 until tieElements.length).any { tieIndex ->
            (tieElements.item(tieIndex) as Element).getAttribute("type") == "stop"
        }
        return !isTieContinuation
    }

    /**
     * @param noteElement A plucked `<note>` element. Type: Element
     * @param semitoneTransposition Written-to-sounding offset, in semitones. Type: Int
     * @param previousNote The previous note's position, or null. Type: TabNote?
     * @return Where to play the note. Type: TabNote
     */
    private fun tabNoteFor(noteElement: Element, semitoneTransposition: Int, previousNote: TabNote?): TabNote {
        val stringNumber = descendantText(noteElement, "string")?.toIntOrNull()
        val fret = descendantText(noteElement, "fret")?.toIntOrNull()
        if (stringNumber != null && fret != null && stringNumber in 1..tuning.strings.size) {
            return TabNote(stringIndex = tuning.strings.size - stringNumber, fret = fret)
        }
        val pitchElement = childElement(noteElement, "pitch")
            ?: throw IllegalArgumentException("A note has neither tab nor pitch")
        return fretboardPositionFinder.positionFor(midiNumberOf(pitchElement) + semitoneTransposition, previousNote)
    }

    /**
     * @param pitchElement A `<pitch>` element. Type: Element
     * @return Its MIDI note number. Type: Int
     */
    private fun midiNumberOf(pitchElement: Element): Int {
        val step = descendantText(pitchElement, "step")
        val stepSemitone = STEP_SEMITONES[step] ?: throw IllegalArgumentException("Unknown pitch step '$step'")
        val alter = descendantText(pitchElement, "alter")?.toDoubleOrNull()?.toInt() ?: 0
        val octave = descendantText(pitchElement, "octave")?.toIntOrNull()
            ?: throw IllegalArgumentException("Pitch without octave")
        return (octave + 1) * SEMITONES_PER_OCTAVE + stepSemitone + alter
    }

    /**
     * @param partElement The `<part>` being read. Type: Element
     * @return Semitones to add to written pitches to get sounding pitches. Type: Int
     */
    private fun semitoneTranspositionOf(partElement: Element): Int {
        val transposeElement = partElement.getElementsByTagName("transpose").item(0) as? Element ?: return 0
        val chromatic = descendantText(transposeElement, "chromatic")?.toIntOrNull() ?: 0
        val octaveChange = descendantText(transposeElement, "octave-change")?.toIntOrNull() ?: 0
        return chromatic + octaveChange * SEMITONES_PER_OCTAVE
    }

    /**
     * @param scoreDocument Whole score. Type: Document
     * @return Work title, else movement title, else the default. Type: String
     */
    private fun titleOf(scoreDocument: Document): String {
        val root = scoreDocument.documentElement
        return (descendantText(root, "work-title") ?: descendantText(root, "movement-title"))
            ?.takeIf { it.isNotBlank() } ?: SongImporter.DEFAULT_IMPORTED_TITLE
    }

    /**
     * @param scoreDocument Whole score. Type: Document
     * @return Tempo of the first `<sound tempo>`, else the default tempo. Type: Int
     */
    private fun tempoOf(scoreDocument: Document): Int {
        val soundElements = scoreDocument.getElementsByTagName("sound")
        for (soundIndex in 0 until soundElements.length) {
            val tempo = (soundElements.item(soundIndex) as Element).getAttribute("tempo").toDoubleOrNull()
            if (tempo != null && tempo > 0) return tempo.toInt().coerceAtLeast(1)
        }
        return TabTextParser.DEFAULT_TEMPO_BPM
    }

    /**
     * @param parent Element to look in. Type: Element
     * @param tagName Tag of the direct child wanted. Type: String
     * @return The first direct child with that tag, or null. Type: Element?
     */
    private fun childElement(parent: Element, tagName: String): Element? {
        val children = parent.childNodes
        return (0 until children.length).map { children.item(it) }
            .firstOrNull { it is Element && it.tagName == tagName } as? Element
    }

    /**
     * @param parent Element to look in. Type: Element
     * @param tagName Tag of the descendant wanted. Type: String
     * @return Trimmed text of the first descendant with that tag, or null. Type: String?
     */
    private fun descendantText(parent: Element, tagName: String): String? {
        return parent.getElementsByTagName(tagName).item(0)?.textContent?.trim()
    }

    private companion object {
        const val SEMITONES_PER_OCTAVE = 12
        const val BYTE_ORDER_MARK = '\uFEFF'
        const val LOAD_EXTERNAL_DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd"
        val STEP_SEMITONES = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)
    }
}
