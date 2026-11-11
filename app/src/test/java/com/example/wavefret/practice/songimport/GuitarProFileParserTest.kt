package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GuitarProFileParserTest {

    private val parser = GuitarProFileParser()

    /** One test track: its name, tuning and the beat contents of its single bar. */
    private data class TestTrack(val name: String, val pitches: String, val beats: List<String>)

    /**
     * Builds a one-bar .gp file. Each beat string is "rhythmId:noteIds" with
     * notes given in [notesXml]; an empty noteIds is a rest.
     */
    private fun guitarProFile(
        tracks: List<TestTrack>,
        notesXml: String,
        rhythmsXml: String = "<Rhythm id=\"0\"><NoteValue>Quarter</NoteValue></Rhythm>",
        tempo: String = "120 2",
        title: String = "My Song",
        bars: Int = 1
    ): ByteArray {
        val beatXml = StringBuilder()
        val voiceXml = StringBuilder()
        val barXml = StringBuilder()
        var beatId = 0
        tracks.forEachIndexed { trackIndex, track ->
            val beatIds = track.beats.map { beat ->
                val (rhythmId, noteIds) = beat.split(":")
                val notesElement = if (noteIds.isBlank()) "" else "<Notes>$noteIds</Notes>"
                beatXml.append("<Beat id=\"$beatId\"><Rhythm ref=\"$rhythmId\" />$notesElement</Beat>")
                beatId++
            }
            voiceXml.append("<Voice id=\"$trackIndex\"><Beats>${beatIds.joinToString(" ")}</Beats></Voice>")
            barXml.append("<Bar id=\"$trackIndex\"><Voices>$trackIndex -1 -1 -1</Voices></Bar>")
        }
        val masterBars = (1..bars).joinToString("") {
            "<MasterBar><Time>4/4</Time><Bars>${tracks.indices.joinToString(" ")}</Bars></MasterBar>"
        }
        val tracksXml = tracks.mapIndexed { index, track ->
            "<Track id=\"$index\"><Name><![CDATA[${track.name}]]></Name><Staves><Staff><Properties>" +
                "<Property name=\"Tuning\"><Pitches>${track.pitches}</Pitches></Property></Properties></Staff></Staves></Track>"
        }.joinToString("")
        val gpif = """<?xml version="1.0" encoding="utf-8"?>
            <GPIF><Score><Title><![CDATA[$title]]></Title></Score>
            <MasterTrack><Automations><Automation><Type>Tempo</Type><Bar>0</Bar><Value>$tempo</Value></Automation></Automations></MasterTrack>
            <Tracks>$tracksXml</Tracks><MasterBars>$masterBars</MasterBars><Bars>$barXml</Bars><Voices>$voiceXml</Voices>
            <Beats>$beatXml</Beats><Notes>$notesXml</Notes><Rhythms>$rhythmsXml</Rhythms></GPIF>"""
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zip ->
            zip.putNextEntry(ZipEntry("Content/score.gpif")); zip.write(gpif.toByteArray()); zip.closeEntry()
        }
        return zipBytes.toByteArray()
    }

    private fun note(id: Int, string: Int, fret: Int, extra: String = "") =
        "<Note id=\"$id\">$extra<Properties><Property name=\"Fret\"><Fret>$fret</Fret></Property>" +
            "<Property name=\"String\"><String>$string</String></Property></Properties></Note>"

    private val bassTuning = "28 33 38 43"

    @Test
    fun readsTitleTempoAndNotesFromTheTab() {
        val gpBytes = guitarProFile(
            tracks = listOf(TestTrack("Bass", bassTuning, listOf("0:0", "0:1", "0:2", "0:3"))),
            notesXml = note(0, 0, 0) + note(1, 0, 3) + note(2, 1, 0) + note(3, 1, 2)
        )

        val timedScore = parser.parse(gpBytes)

        assertEquals("My Song", timedScore.title)
        assertEquals(120, timedScore.tempoBpm)
        assertEquals(
            listOf(NoteEvent(28, 0, 500), NoteEvent(31, 500, 500), NoteEvent(33, 1_000, 500), NoteEvent(35, 1_500, 500)),
            timedScore.noteEvents
        )
    }

    @Test
    fun restsAdvanceTimeWithoutNotes() {
        val gpBytes = guitarProFile(
            tracks = listOf(TestTrack("Bass", bassTuning, listOf("0:", "0:0"))),
            notesXml = note(0, 0, 0)
        )

        assertEquals(listOf(500L), parser.parse(gpBytes).noteEvents.map { it.onsetMillis })
    }

    @Test
    fun dottedAndTupletRhythmsChangeDurations() {
        val gpBytes = guitarProFile(
            tracks = listOf(TestTrack("Bass", bassTuning, listOf("1:0", "2:1"))),
            notesXml = note(0, 0, 0) + note(1, 0, 2),
            rhythmsXml = "<Rhythm id=\"1\"><NoteValue>Quarter</NoteValue><AugmentationDot count=\"1\" /></Rhythm>" +
                "<Rhythm id=\"2\"><NoteValue>Eighth</NoteValue><PrimaryTuplet num=\"3\" den=\"2\" /></Rhythm>"
        )

        val noteEvents = parser.parse(gpBytes).noteEvents

        assertEquals(listOf(750L, 167L), noteEvents.map { it.durationMillis })
        assertEquals(750L, noteEvents[1].onsetMillis)
    }

    @Test
    fun tiedContinuationsAreNotNewNotes() {
        val gpBytes = guitarProFile(
            tracks = listOf(TestTrack("Bass", bassTuning, listOf("0:0", "0:1"))),
            notesXml = note(0, 0, 5, "<Tie origin=\"true\" destination=\"false\" />") +
                note(1, 0, 5, "<Tie origin=\"false\" destination=\"true\" />")
        )

        assertEquals(1, parser.parse(gpBytes).noteEvents.size)
    }

    @Test
    fun nextBarStartsAfterAFullFourFourBar() {
        val gpBytes = guitarProFile(
            tracks = listOf(TestTrack("Bass", bassTuning, listOf("0:0"))),
            notesXml = note(0, 0, 0),
            bars = 2
        )

        assertEquals(listOf(0L, 2_000L), parser.parse(gpBytes).noteEvents.map { it.onsetMillis })
    }

    @Test
    fun picksTheBassTrackByItsLowTuning() {
        val gpBytes = guitarProFile(
            tracks = listOf(
                TestTrack("Rhythm", "40 45 50 55 59 64", listOf("0:0")),
                TestTrack("Low end", bassTuning, listOf("0:1"))
            ),
            notesXml = note(0, 5, 0) + note(1, 2, 2)
        )

        assertEquals(listOf(40), parser.parse(gpBytes).noteEvents.map { it.midiNumber })
    }

    @Test
    fun usesTheTempoFromTheTempoAutomation() {
        val gpBytes = guitarProFile(
            tracks = listOf(TestTrack("Bass", bassTuning, listOf("0:0"))),
            notesXml = note(0, 0, 0),
            tempo = "90 2"
        )

        val timedScore = parser.parse(gpBytes)

        assertEquals(90, timedScore.tempoBpm)
        assertEquals(667L, timedScore.noteEvents.single().durationMillis)
    }

    @Test
    fun archiveWithoutAScoreIsRejected() {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zip -> zip.putNextEntry(ZipEntry("readme.txt")); zip.closeEntry() }

        assertThrows(IllegalArgumentException::class.java) { parser.parse(zipBytes.toByteArray()) }
    }
}
