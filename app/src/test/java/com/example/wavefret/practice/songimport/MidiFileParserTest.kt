package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class MidiFileParserTest {

    private val parser = MidiFileParser()

    /** Builds Standard MIDI File bytes for tests. */
    private class MidiFileBuilder(private val ticksPerQuarter: Int = 480) {
        private val tracks = mutableListOf<ByteArray>()

        fun track(build: TrackBuilder.() -> Unit): MidiFileBuilder {
            tracks += TrackBuilder().apply(build).bytes()
            return this
        }

        fun bytes(): ByteArray {
            val output = ByteArrayOutputStream()
            output.write("MThd".toByteArray()); output.write(int32(6))
            output.write(int16(if (tracks.size == 1) 0 else 1)); output.write(int16(tracks.size)); output.write(int16(ticksPerQuarter))
            tracks.forEach { trackBytes ->
                output.write("MTrk".toByteArray()); output.write(int32(trackBytes.size)); output.write(trackBytes)
            }
            return output.toByteArray()
        }
    }

    private class TrackBuilder {
        private val output = ByteArrayOutputStream()
        fun event(deltaTicks: Int, vararg eventBytes: Int) {
            output.write(variableLength(deltaTicks)); eventBytes.forEach { output.write(it) }
        }
        fun name(name: String) = event(0, 0xFF, 0x03, name.length, *name.toByteArray().map { it.toInt() }.toIntArray())
        fun tempo(microsPerQuarter: Int) =
            event(0, 0xFF, 0x51, 3, microsPerQuarter shr 16 and 0xFF, microsPerQuarter shr 8 and 0xFF, microsPerQuarter and 0xFF)
        fun program(channel: Int, program: Int) = event(0, 0xC0 or channel, program)
        fun note(deltaTicks: Int, channel: Int, midi: Int, lengthTicks: Int) {
            event(deltaTicks, 0x90 or channel, midi, 100)
            event(lengthTicks, 0x80 or channel, midi, 0)
        }
        fun bytes(): ByteArray {
            event(0, 0xFF, 0x2F, 0)
            return output.toByteArray()
        }
    }

    @Test
    fun readsNotesWithTimesFromTheTempo() {
        // 120 BPM = 500 000 µs per quarter; quarters at 0, 500 and 1000 ms
        val midiBytes = MidiFileBuilder().track {
            tempo(500_000)
            note(0, 0, 28, 480); note(0, 0, 31, 480); note(0, 0, 33, 480)
        }.bytes()

        val timedScore = parser.parse(midiBytes)

        assertEquals(
            listOf(NoteEvent(28, 0, 500), NoteEvent(31, 500, 500), NoteEvent(33, 1_000, 500)),
            timedScore.noteEvents
        )
        assertEquals(120, timedScore.tempoBpm)
    }

    @Test
    fun noteOnWithZeroVelocityEndsTheNote() {
        val midiBytes = MidiFileBuilder().track {
            tempo(500_000)
            event(0, 0x90, 40, 100)
            event(240, 0x90, 40, 0)
        }.bytes()

        assertEquals(listOf(NoteEvent(40, 0, 250)), parser.parse(midiBytes).noteEvents)
    }

    @Test
    fun runningStatusIsUnderstood() {
        val midiBytes = MidiFileBuilder().track {
            tempo(500_000)
            event(0, 0x90, 40, 100)
            event(480, 40, 0) // running status: note-on velocity 0
            event(0, 43, 100)
            event(480, 43, 0)
        }.bytes()

        assertEquals(listOf(40, 43), parser.parse(midiBytes).noteEvents.map { it.midiNumber })
    }

    @Test
    fun picksTheTrackNamedBass() {
        val midiBytes = MidiFileBuilder()
            .track { name("Song title"); tempo(500_000) }
            .track { name("Piano"); note(0, 0, 30, 480) }
            .track { name("Electric Bass"); note(0, 1, 40, 480) }
            .bytes()

        val timedScore = parser.parse(midiBytes)

        assertEquals(listOf(40), timedScore.noteEvents.map { it.midiNumber })
        assertEquals("Song title", timedScore.title)
    }

    @Test
    fun otherwisePicksTheTrackWithABassProgram() {
        val midiBytes = MidiFileBuilder()
            .track { tempo(500_000) }
            .track { program(0, 0); note(0, 0, 30, 480) }
            .track { program(1, 33); note(0, 1, 45, 480) }
            .bytes()

        assertEquals(listOf(45), parser.parse(midiBytes).noteEvents.map { it.midiNumber })
    }

    @Test
    fun otherwisePicksTheLowestPitchedNonDrumTrack() {
        val midiBytes = MidiFileBuilder()
            .track { tempo(500_000) }
            .track { note(0, 9, 35, 480) } // drums on channel 10
            .track { note(0, 0, 60, 480) }
            .track { note(0, 1, 40, 480) }
            .bytes()

        assertEquals(listOf(40), parser.parse(midiBytes).noteEvents.map { it.midiNumber })
    }

    @Test
    fun defaultsTo120BpmWithoutATempoEvent() {
        val midiBytes = MidiFileBuilder().track { note(0, 0, 40, 960) }.bytes()

        val timedScore = parser.parse(midiBytes)

        assertEquals(120, timedScore.tempoBpm)
        assertEquals(1_000L, timedScore.noteEvents.single().durationMillis)
    }

    @Test
    fun tempoChangesAffectLaterNotes() {
        val midiBytes = MidiFileBuilder().track {
            tempo(500_000)
            note(0, 0, 40, 480)
            event(0, 0xFF, 0x51, 3, 0x0F, 0x42, 0x40) // 1 000 000 µs: 60 BPM
            note(0, 0, 43, 480)
            note(0, 0, 45, 480)
        }.bytes()

        assertEquals(listOf(0L, 500L, 1_500L), parser.parse(midiBytes).noteEvents.map { it.onsetMillis })
    }

    @Test
    fun fileWithoutHeaderIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { parser.parse("not midi".toByteArray()) }
    }

    @Test
    fun fileWithoutNotesIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(MidiFileBuilder().track { tempo(500_000) }.bytes())
        }
    }

    private companion object {
        fun int32(value: Int) = byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())
        fun int16(value: Int) = byteArrayOf((value shr 8).toByte(), value.toByte())
        fun variableLength(value: Int): ByteArray {
            var remaining = value
            val groups = mutableListOf(remaining and 0x7F)
            remaining = remaining shr 7
            while (remaining > 0) {
                groups += (remaining and 0x7F) or 0x80
                remaining = remaining shr 7
            }
            return groups.reversed().map { it.toByte() }.toByteArray()
        }
    }
}
