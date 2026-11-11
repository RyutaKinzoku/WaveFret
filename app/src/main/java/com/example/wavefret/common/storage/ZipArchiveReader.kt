package com.example.wavefret.common.storage

import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Reads every file in a zip archive held in memory, trusting the central
 * directory (the archive's authoritative index) rather than per-entry
 * local headers. `java.util.zip.ZipInputStream` reads local headers and
 * rejects archives, such as Guitar Pro's, whose data descriptors carry
 * wrong sizes; this reader accepts them. Supports stored and deflated
 * entries; ZIP64 archives are not supported.
 */
class ZipArchiveReader {

    /**
     * @param archiveBytes Full contents of the zip archive. Type: ByteArray
     * @return Each file's bytes by its path inside the archive, in index order. Type: Map<String, ByteArray>
     * @throws IllegalArgumentException If the bytes are not a readable zip archive.
     */
    fun readEntries(archiveBytes: ByteArray): Map<String, ByteArray> {
        val endRecordOffset = findEndOfCentralDirectory(archiveBytes)
        val entryCount = readUInt16(archiveBytes, endRecordOffset + END_ENTRY_COUNT_OFFSET)
        var headerOffset = readInt32(archiveBytes, endRecordOffset + END_DIRECTORY_START_OFFSET)
        val entries = linkedMapOf<String, ByteArray>()
        repeat(entryCount) {
            require(readInt32(archiveBytes, headerOffset) == CENTRAL_HEADER_SIGNATURE) { "Corrupt zip directory" }
            val compressionMethod = readUInt16(archiveBytes, headerOffset + CENTRAL_METHOD_OFFSET)
            val compressedSize = readInt32(archiveBytes, headerOffset + CENTRAL_COMPRESSED_SIZE_OFFSET)
            val uncompressedSize = readInt32(archiveBytes, headerOffset + CENTRAL_UNCOMPRESSED_SIZE_OFFSET)
            val nameLength = readUInt16(archiveBytes, headerOffset + CENTRAL_NAME_LENGTH_OFFSET)
            val extraLength = readUInt16(archiveBytes, headerOffset + CENTRAL_EXTRA_LENGTH_OFFSET)
            val commentLength = readUInt16(archiveBytes, headerOffset + CENTRAL_COMMENT_LENGTH_OFFSET)
            val localHeaderOffset = readInt32(archiveBytes, headerOffset + CENTRAL_LOCAL_OFFSET_OFFSET)
            val entryName = String(
                archiveBytes, headerOffset + CENTRAL_HEADER_LENGTH, nameLength, Charsets.UTF_8
            )
            if (!entryName.endsWith('/')) {
                val dataOffset = localDataOffset(archiveBytes, localHeaderOffset)
                require(dataOffset + compressedSize <= archiveBytes.size) { "Zip entry $entryName is truncated" }
                val compressedBytes = archiveBytes.copyOfRange(dataOffset, dataOffset + compressedSize)
                entries[entryName] = when (compressionMethod) {
                    STORED -> compressedBytes
                    DEFLATED -> inflate(compressedBytes, uncompressedSize)
                    else -> throw IllegalArgumentException("Unsupported zip compression method $compressionMethod")
                }
            }
            headerOffset += CENTRAL_HEADER_LENGTH + nameLength + extraLength + commentLength
        }
        return entries
    }

    /**
     * @param archiveBytes Archive bytes. Type: ByteArray
     * @return Offset of the end-of-central-directory record. Type: Int
     */
    private fun findEndOfCentralDirectory(archiveBytes: ByteArray): Int {
        val lastPossibleOffset = archiveBytes.size - END_RECORD_LENGTH
        val firstPossibleOffset = maxOf(0, lastPossibleOffset - MAX_COMMENT_LENGTH)
        for (offset in lastPossibleOffset downTo firstPossibleOffset) {
            if (readInt32(archiveBytes, offset) == END_RECORD_SIGNATURE) return offset
        }
        throw IllegalArgumentException("Not a zip archive")
    }

    /**
     * @param archiveBytes Archive bytes. Type: ByteArray
     * @param localHeaderOffset Where the entry's local header starts. Type: Int
     * @return Where the entry's data starts. Type: Int
     */
    private fun localDataOffset(archiveBytes: ByteArray, localHeaderOffset: Int): Int {
        require(readInt32(archiveBytes, localHeaderOffset) == LOCAL_HEADER_SIGNATURE) { "Corrupt zip entry header" }
        val nameLength = readUInt16(archiveBytes, localHeaderOffset + LOCAL_NAME_LENGTH_OFFSET)
        val extraLength = readUInt16(archiveBytes, localHeaderOffset + LOCAL_EXTRA_LENGTH_OFFSET)
        return localHeaderOffset + LOCAL_HEADER_LENGTH + nameLength + extraLength
    }

    /**
     * @param compressedBytes Raw deflate data. Type: ByteArray
     * @param expectedSize Size recorded in the central directory. Type: Int
     * @return The inflated bytes. Type: ByteArray
     */
    private fun inflate(compressedBytes: ByteArray, expectedSize: Int): ByteArray {
        val inflater = Inflater(true)
        try {
            inflater.setInput(compressedBytes)
            val inflatedBytes = ByteArray(expectedSize)
            var inflatedCount = 0
            while (inflatedCount < expectedSize && !inflater.finished()) {
                val chunkCount = inflater.inflate(inflatedBytes, inflatedCount, expectedSize - inflatedCount)
                if (chunkCount == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                inflatedCount += chunkCount
            }
            return inflatedBytes.copyOf(inflatedCount)
        } catch (corruptData: DataFormatException) {
            throw IllegalArgumentException("Corrupt compressed data in zip", corruptData)
        } finally {
            inflater.end()
        }
    }

    /**
     * @param bytes Bytes to read. Type: ByteArray
     * @param offset Where the little-endian value starts. Type: Int
     * @return The unsigned 16-bit value. Type: Int
     */
    private fun readUInt16(bytes: ByteArray, offset: Int): Int {
        require(offset >= 0 && offset + 2 <= bytes.size) { "Truncated zip archive" }
        return (bytes[offset].toInt() and BYTE_MASK) or ((bytes[offset + 1].toInt() and BYTE_MASK) shl 8)
    }

    /**
     * @param bytes Bytes to read. Type: ByteArray
     * @param offset Where the little-endian value starts. Type: Int
     * @return The 32-bit value. Type: Int
     */
    private fun readInt32(bytes: ByteArray, offset: Int): Int = readUInt16(bytes, offset) or (readUInt16(bytes, offset + 2) shl 16)

    private companion object {
        const val END_RECORD_SIGNATURE = 0x06054B50
        const val CENTRAL_HEADER_SIGNATURE = 0x02014B50
        const val LOCAL_HEADER_SIGNATURE = 0x04034B50
        const val END_RECORD_LENGTH = 22
        const val MAX_COMMENT_LENGTH = 0xFFFF
        const val END_ENTRY_COUNT_OFFSET = 10
        const val END_DIRECTORY_START_OFFSET = 16
        const val CENTRAL_HEADER_LENGTH = 46
        const val CENTRAL_METHOD_OFFSET = 10
        const val CENTRAL_COMPRESSED_SIZE_OFFSET = 20
        const val CENTRAL_UNCOMPRESSED_SIZE_OFFSET = 24
        const val CENTRAL_NAME_LENGTH_OFFSET = 28
        const val CENTRAL_EXTRA_LENGTH_OFFSET = 30
        const val CENTRAL_COMMENT_LENGTH_OFFSET = 32
        const val CENTRAL_LOCAL_OFFSET_OFFSET = 42
        const val LOCAL_HEADER_LENGTH = 30
        const val LOCAL_NAME_LENGTH_OFFSET = 26
        const val LOCAL_EXTRA_LENGTH_OFFSET = 28
        const val STORED = 0
        const val DEFLATED = 8
        const val BYTE_MASK = 0xFF
    }
}
