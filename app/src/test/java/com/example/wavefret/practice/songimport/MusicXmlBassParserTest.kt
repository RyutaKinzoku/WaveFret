package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MusicXmlBassParserTest {

    private val musicXmlBassParser = MusicXmlBassParser(
        tuning = BassTunings.STANDARD,
        fretboardPositionFinder = FretboardPositionFinder(BassTunings.STANDARD)
    )

    private fun scoreWith(measureContent: String, header: String = "", attributes: String = "") = """
        <?xml version="1.0" encoding="UTF-8"?>
        <score-partwise version="4.0">
          $header
          <part-list><score-part id="P1"><part-name>Bass</part-name></score-part></part-list>
          <part id="P1">
            <measure number="1">
              <attributes>$attributes</attributes>
              $measureContent
            </measure>
          </part>
        </score-partwise>
    """.trimIndent()

    private fun pitchedNote(step: String, octave: Int, alter: Int? = null, extra: String = "") = """
        <note>$extra<pitch><step>$step</step>${alter?.let { "<alter>$it</alter>" } ?: ""}<octave>$octave</octave></pitch>
        <duration>1</duration></note>
    """

    @Test
    fun readsTitleFromWorkTitle() {
        val musicXml = scoreWith(pitchedNote("E", 1), header = "<work><work-title>Groove</work-title></work>")

        assertEquals("Groove", musicXmlBassParser.parse(musicXml).title)
    }

    @Test
    fun fallsBackToMovementTitle() {
        val musicXml = scoreWith(pitchedNote("E", 1), header = "<movement-title>Moving</movement-title>")

        assertEquals("Moving", musicXmlBassParser.parse(musicXml).title)
    }

    @Test
    fun readsTempoFromTheFirstSoundElement() {
        val musicXml = scoreWith("<direction><sound tempo=\"104\"/></direction>" + pitchedNote("E", 1))

        assertEquals(104, musicXmlBassParser.parse(musicXml).tempoBpm)
    }

    @Test
    fun usesTabStringAndFretWhenTheScoreHasThem() {
        val tabNote = """
            <note><pitch><step>D</step><octave>2</octave></pitch><duration>1</duration>
            <notations><technical><string>3</string><fret>5</fret></technical></notations></note>
        """

        // MusicXML numbers strings from the highest (1 = G), so string 3 is the A string
        assertEquals(listOf(TabNote(1, 5)), musicXmlBassParser.parse(scoreWith(tabNote)).notes)
    }

    @Test
    fun placesPlainPitchesOnTheFretboard() {
        val musicXml = scoreWith(pitchedNote("E", 1) + pitchedNote("G", 1) + pitchedNote("B", 1, alter = -1))

        assertEquals(listOf(TabNote(0, 0), TabNote(0, 3), TabNote(0, 6)), musicXmlBassParser.parse(musicXml).notes)
    }

    @Test
    fun appliesTheOctaveTranspositionOfBassParts() {
        val musicXml = scoreWith(
            pitchedNote("E", 2),
            attributes = "<transpose><diatonic>0</diatonic><chromatic>0</chromatic><octave-change>-1</octave-change></transpose>"
        )

        assertEquals(listOf(TabNote(0, 0)), musicXmlBassParser.parse(musicXml).notes)
    }

    @Test
    fun skipsRestsChordTonesAndTiedContinuations() {
        val musicXml = scoreWith(
            pitchedNote("A", 1) +
                "<note><rest/><duration>1</duration></note>" +
                pitchedNote("E", 2, extra = "<chord/>") +
                pitchedNote("A", 1, extra = "<tie type=\"stop\"/>") +
                pitchedNote("D", 2)
        )

        assertEquals(listOf(TabNote(1, 0), TabNote(2, 0)), musicXmlBassParser.parse(musicXml).notes)
    }

    @Test
    fun scoreWithoutNotesIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            musicXmlBassParser.parse(scoreWith("<note><rest/><duration>4</duration></note>"))
        }
    }

    @Test
    fun malformedXmlIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { musicXmlBassParser.parse("<score-partwise><part>") }
    }
}
