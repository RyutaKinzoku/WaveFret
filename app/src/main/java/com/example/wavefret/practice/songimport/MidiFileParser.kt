package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.NoteEvent
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Reads a Standard MIDI File (format 0 or 1) and picks out the bass part:
 * the track named "bass", else one using a General MIDI bass program
 * (33-40), else the lowest-pitched non-drum track. Note times follow the
 * file's tempo map, so they line up with the original recording.
 */
class MidiFileParser {

    /**
     * @param midiBytes Full contents of the .mid file. Type: ByteArray
     * @return The bass part with its title and starting tempo. Type: TimedScore
     * @throws IllegalArgumentException If the file isn't MIDI or has no playable notes.
     */
    fun parse(midiBytes: ByteArray): TimedScore {
        val reader = ByteReader(midiBytes)
        require(reader.remaining() >= HEADER_LENGTH && reader.readAscii(4) == "MThd") { "Not a MIDI file" }
        val headerLength = reader.readInt32()
        reader.skip(2)
        val trackCount = reader.readInt16()
        val division = reader.readInt16()
        require(division and SMPTE_DIVISION_FLAG == 0) { "SMPTE-timed MIDI files are not supported" }
        reader.skip(headerLength - HEADER_FIELDS_LENGTH)

        val tracks = mutableListOf<MidiTrack>()
        while (tracks.size < trackCount && reader.remaining() >= CHUNK_HEADER_LENGTH) {
            val chunkType = reader.readAscii(4)
            val chunkLength = reader.readInt32()
            if (chunkType == "MTrk") tracks += readTrack(reader.slice(chunkLength)) else reader.skip(chunkLength)
        }

        val tempoChanges = tracks.flatMap { it.tempoChanges }.sortedBy { it.tick }
        val bassTrack = chooseBassTrack(tracks) ?: throw IllegalArgumentException("The MIDI file has no notes")
        val tickTimer = TickTimer(division, tempoChanges)
        return TimedScore(
            title = tracks.firstOrNull { it.notes.isEmpty() && it.name != null }?.name,
            tempoBpm = (MICROS_PER_MINUTE / (tempoChanges.firstOrNull()?.microsPerQuarter?.toDouble() ?: DEFAULT_MICROS_PER_QUARTER))
                .roundToInt(),
            noteEvents = bassTrack.notes.sortedBy { it.startTick }.map { note ->
                val onsetMillis = tickTimer.millisAt(note.startTick)
                NoteEvent(note.midiNumber, onsetMillis, tickTimer.millisAt(note.endTick) - onsetMillis)
            }
        )
    }

    /**
     * @param reader Reader over one track chunk's bytes. Type: ByteReader
     * @return The track's name, programs, tempo changes and notes. Type: MidiTrack
     */
    private fun readTrack(reader: ByteReader): MidiTrack {
        val midiTrack = MidiTrack()
        val openNoteStartTicks = mutableMapOf<Int, ArrayDeque<Long>>()
        var tick = 0L
        var runningStatus = 0
        while (reader.remaining() > 0) {
            tick += reader.readVariableLength()
            var status = reader.readUnsignedByte()
            if (status < STATUS_BIT) {
                reader.unread()
                status = runningStatus
            }
            when {
                status == META_EVENT -> {
                    runningStatus = 0
                    val metaType = reader.readUnsignedByte()
                    val metaData = reader.readBytes(reader.readVariableLength().toInt())
                    when (metaType) {
                        META_TRACK_NAME -> if (midiTrack.name == null) midiTrack.name = metaData.toString(Charsets.ISO_8859_1).trim()
                        META_TEMPO -> midiTrack.tempoChanges += TempoChange(tick, readTempo(metaData))
                        META_END_OF_TRACK -> return midiTrack
                    }
                }
                status == SYSEX_START || status == SYSEX_ESCAPE -> {
                    runningStatus = 0
                    reader.skip(reader.readVariableLength().toInt())
                }
                else -> {
                    require(status >= STATUS_BIT) { "Corrupt MIDI track: data byte without a status" }
                    runningStatus = status
                    val channel = status and CHANNEL_MASK
                    val firstData = reader.readUnsignedByte()
                    val secondData = if (status and MESSAGE_MASK in ONE_DATA_BYTE_MESSAGES) 0 else reader.readUnsignedByte()
                    val noteKey = channel * NOTES_PER_CHANNEL + firstData
                    when (status and MESSAGE_MASK) {
                        NOTE_ON -> if (secondData > 0) {
                            openNoteStartTicks.getOrPut(noteKey) { ArrayDeque() }.addLast(tick)
                        } else {
                            closeNote(midiTrack, openNoteStartTicks, noteKey, channel, firstData, tick)
                        }
                        NOTE_OFF -> closeNote(midiTrack, openNoteStartTicks, noteKey, channel, firstData, tick)
                        PROGRAM_CHANGE -> midiTrack.programs += firstData
                    }
                }
            }
        }
        return midiTrack
    }

    /**
     * Ends the earliest still-sounding note with this pitch on this channel.
     *
     * @param midiTrack Track receiving the note. Type: MidiTrack
     * @param openNoteStartTicks Start ticks of sounding notes, by channel and pitch. Type: MutableMap<Int, ArrayDeque<Long>>
     * @param noteKey Channel and pitch combined. Type: Int
     * @param channel MIDI channel, 0-15. Type: Int
     * @param midiNumber Pitch. Type: Int
     * @param tick When the note ends. Type: Long
     * @return Unit
     */
    private fun closeNote(
        midiTrack: MidiTrack,
        openNoteStartTicks: MutableMap<Int, ArrayDeque<Long>>,
        noteKey: Int,
        channel: Int,
        midiNumber: Int,
        tick: Long
    ) {
        val startTick = openNoteStartTicks[noteKey]?.removeFirstOrNull() ?: return
        if (channel != DRUM_CHANNEL) midiTrack.notes += MidiNote(midiNumber, startTick, tick)
    }

    /**
     * @param tracks All tracks in the file. Type: List<MidiTrack>
     * @return The track most likely to be the bass part, or null if none has notes. Type: MidiTrack?
     */
    private fun chooseBassTrack(tracks: List<MidiTrack>): MidiTrack? {
        val tracksWithNotes = tracks.filter { it.notes.isNotEmpty() }
        return tracksWithNotes.firstOrNull { it.name?.contains("bass", ignoreCase = true) == true }
            ?: tracksWithNotes.firstOrNull { track -> track.programs.any { it in BASS_PROGRAMS } }
            ?: tracksWithNotes.minByOrNull { track -> track.notes.map { it.midiNumber }.average() }
    }

    /**
     * @param tempoData The three bytes of a tempo meta event. Type: ByteArray
     * @return Microseconds per quarter note. Type: Int
     */
    private fun readTempo(tempoData: ByteArray): Int {
        require(tempoData.size == TEMPO_DATA_LENGTH) { "Corrupt MIDI tempo event" }
        return tempoData.fold(0) { microsPerQuarter, byte -> (microsPerQuarter shl 8) or (byte.toInt() and BYTE_MASK) }
    }

    /** One track's useful contents. */
    private class MidiTrack {
        var name: String? = null
        val programs = mutableSetOf<Int>()
        val tempoChanges = mutableListOf<TempoChange>()
        val notes = mutableListOf<MidiNote>()
    }

    /**
     * @property midiNumber Pitch. Type: Int
     * @property startTick When it starts. Type: Long
     * @property endTick When it ends. Type: Long
     */
    private data class MidiNote(val midiNumber: Int, val startTick: Long, val endTick: Long)

    /**
     * @property tick When the tempo changes. Type: Long
     * @property microsPerQuarter New tempo. Type: Int
     */
    private data class TempoChange(val tick: Long, val microsPerQuarter: Int)

    /**
     * Converts ticks to milliseconds across tempo changes.
     *
     * @param ticksPerQuarter The file's time division. Type: Int
     * @param tempoChanges Tempo changes sorted by tick. Type: List<TempoChange>
     */
    private class TickTimer(private val ticksPerQuarter: Int, private val tempoChanges: List<TempoChange>) {

        /**
         * @param tick Position in ticks. Type: Long
         * @return Time from the start, in milliseconds. Type: Long
         */
        fun millisAt(tick: Long): Long {
            var elapsedMicros = 0.0
            var segmentStartTick = 0L
            var microsPerQuarter = DEFAULT_MICROS_PER_QUARTER
            for (tempoChange in tempoChanges) {
                if (tempoChange.tick >= tick) break
                elapsedMicros += (tempoChange.tick - segmentStartTick) * microsPerQuarter / ticksPerQuarter
                segmentStartTick = tempoChange.tick
                microsPerQuarter = tempoChange.microsPerQuarter.toDouble()
            }
            elapsedMicros += (tick - segmentStartTick) * microsPerQuarter / ticksPerQuarter
            return (elapsedMicros / MICROS_PER_MILLI).roundToLong()
        }
    }

    /**
     * Big-endian reader over a byte array, as MIDI files are laid out.
     *
     * @param bytes Bytes to read. Type: ByteArray
     * @param start First index to read. Type: Int
     * @param end Index after the last byte. Type: Int
     */
    private class ByteReader(private val bytes: ByteArray, start: Int = 0, private val end: Int = bytes.size) {
        private var position = start

        fun remaining(): Int = end - position

        fun readUnsignedByte(): Int {
            require(position < end) { "The MIDI file ends unexpectedly" }
            return bytes[position++].toInt() and BYTE_MASK
        }

        fun unread() {
            position--
        }

        fun skip(byteCount: Int) {
            require(byteCount >= 0 && position + byteCount <= end) { "The MIDI file ends unexpectedly" }
            position += byteCount
        }

        fun readBytes(byteCount: Int): ByteArray {
            require(byteCount >= 0 && position + byteCount <= end) { "The MIDI file ends unexpectedly" }
            return bytes.copyOfRange(position, position + byteCount).also { position += byteCount }
        }

        fun readAscii(byteCount: Int): String = readBytes(byteCount).toString(Charsets.US_ASCII)

        fun readInt16(): Int = (readUnsignedByte() shl 8) or readUnsignedByte()

        fun readInt32(): Int = (readInt16() shl 16) or readInt16()

        fun readVariableLength(): Long {
            var value = 0L
            repeat(MAX_VARIABLE_LENGTH_BYTES) {
                val byte = readUnsignedByte()
                value = (value shl 7) or (byte and VARIABLE_LENGTH_VALUE_MASK).toLong()
                if (byte and STATUS_BIT == 0) return value
            }
            throw IllegalArgumentException("Corrupt MIDI variable-length number")
        }

        fun slice(byteCount: Int): ByteReader {
            require(byteCount >= 0 && position + byteCount <= end) { "The MIDI file ends unexpectedly" }
            return ByteReader(bytes, position, position + byteCount).also { position += byteCount }
        }
    }

    private companion object {
        const val HEADER_LENGTH = 14
        const val HEADER_FIELDS_LENGTH = 6
        const val CHUNK_HEADER_LENGTH = 8
        const val SMPTE_DIVISION_FLAG = 0x8000
        const val STATUS_BIT = 0x80
        const val META_EVENT = 0xFF
        const val SYSEX_START = 0xF0
        const val SYSEX_ESCAPE = 0xF7
        const val META_TRACK_NAME = 0x03
        const val META_TEMPO = 0x51
        const val META_END_OF_TRACK = 0x2F
        const val MESSAGE_MASK = 0xF0
        const val CHANNEL_MASK = 0x0F
        const val NOTE_OFF = 0x80
        const val NOTE_ON = 0x90
        const val PROGRAM_CHANGE = 0xC0
        val ONE_DATA_BYTE_MESSAGES = setOf(0xC0, 0xD0)
        const val NOTES_PER_CHANNEL = 128
        const val DRUM_CHANNEL = 9
        val BASS_PROGRAMS = 32..39
        const val TEMPO_DATA_LENGTH = 3
        const val BYTE_MASK = 0xFF
        const val VARIABLE_LENGTH_VALUE_MASK = 0x7F
        const val MAX_VARIABLE_LENGTH_BYTES = 4
        const val DEFAULT_MICROS_PER_QUARTER = 500_000.0
        const val MICROS_PER_MINUTE = 60_000_000.0
        const val MICROS_PER_MILLI = 1_000.0
    }
}
