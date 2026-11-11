package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.TabTextParser
import org.w3c.dom.Document
import org.w3c.dom.Element

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
 * @property safeXmlDocumentParser Parses the score XML safely. Type: SafeXmlDocumentParser
 */
class MusicXmlBassParser(
    private val tuning: BassTuning,
    private val fretboardPositionFinder: FretboardPositionFinder,
    private val safeXmlDocumentParser: SafeXmlDocumentParser = SafeXmlDocumentParser()
) {

    /**
     * @param musicXmlText Full MusicXML document text. Type: String
     * @return The bass line as an exercise. Type: Exercise
     * @throws IllegalArgumentException If the XML is malformed or holds no playable notes.
     */
    fun parse(musicXmlText: String): Exercise {
        val scoreDocument = safeXmlDocumentParser.parse(musicXmlText)
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
     * @param noteElement A `<note>` element. Type: Element
     * @return True for a newly plucked note: not a rest, chord tone or tie continuation. Type: Boolean
     */
    private fun isPluckedNote(noteElement: Element): Boolean {
        if (noteElement.childElement("rest") != null) return false
        if (noteElement.childElement("chord") != null) return false
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
        val stringNumber = noteElement.descendantText("string")?.toIntOrNull()
        val fret = noteElement.descendantText("fret")?.toIntOrNull()
        if (stringNumber != null && fret != null && stringNumber in 1..tuning.strings.size) {
            return TabNote(stringIndex = tuning.strings.size - stringNumber, fret = fret)
        }
        val pitchElement = noteElement.childElement("pitch")
            ?: throw IllegalArgumentException("A note has neither tab nor pitch")
        return fretboardPositionFinder.positionFor(midiNumberOf(pitchElement) + semitoneTransposition, previousNote)
    }

    /**
     * @param pitchElement A `<pitch>` element. Type: Element
     * @return Its MIDI note number. Type: Int
     */
    private fun midiNumberOf(pitchElement: Element): Int {
        val step = pitchElement.descendantText("step")
        val stepSemitone = STEP_SEMITONES[step] ?: throw IllegalArgumentException("Unknown pitch step '$step'")
        val alter = pitchElement.descendantText("alter")?.toDoubleOrNull()?.toInt() ?: 0
        val octave = pitchElement.descendantText("octave")?.toIntOrNull()
            ?: throw IllegalArgumentException("Pitch without octave")
        return (octave + 1) * SEMITONES_PER_OCTAVE + stepSemitone + alter
    }

    /**
     * @param partElement The `<part>` being read. Type: Element
     * @return Semitones to add to written pitches to get sounding pitches. Type: Int
     */
    private fun semitoneTranspositionOf(partElement: Element): Int {
        val transposeElement = partElement.getElementsByTagName("transpose").item(0) as? Element ?: return 0
        val chromatic = transposeElement.descendantText("chromatic")?.toIntOrNull() ?: 0
        val octaveChange = transposeElement.descendantText("octave-change")?.toIntOrNull() ?: 0
        return chromatic + octaveChange * SEMITONES_PER_OCTAVE
    }

    /**
     * @param scoreDocument Whole score. Type: Document
     * @return Work title, else movement title, else the default. Type: String
     */
    private fun titleOf(scoreDocument: Document): String {
        val root = scoreDocument.documentElement
        return (root.descendantText("work-title") ?: root.descendantText("movement-title"))
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

    private companion object {
        const val SEMITONES_PER_OCTAVE = 12
        val STEP_SEMITONES = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)
    }
}
