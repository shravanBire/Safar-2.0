package com.example.safar20.data.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary Packet Codec for SAFAR 2.0 Wire Protocol.
 * Handles encoding/decoding of 16-byte packed TripPoint structs,
 * 20-byte live telemetry packets, 488-byte history chunks, and CRC16 checksums.
 */
object BinaryPacketCodec {

    const val OPCODE_OPEN_SESSION: Byte = 0x01
    const val OPCODE_LIVE_TELEMETRY: Byte = 0x02
    const val OPCODE_HISTORY_CHUNK: Byte = 0x03
    const val OPCODE_HISTORY_ACK: Byte = 0x04
    const val OPCODE_HEARTBEAT: Byte = 0x05

    const val TRIP_POINT_SIZE = 16
    const val LIVE_TELEMETRY_SIZE = 21 // 1B Opcode + 2B Seq + 16B TripPoint + 2B CRC16
    const val HISTORY_CHUNK_SIZE = 488  // 1B Opcode + 2B ChunkID + 2B TotalChunks + 1B Count + 480B Points + 2B CRC16

    data class ParsedTripPoint(
        val timestamp: Long,        // Unix Epoch Seconds (UTC)
        val latitude: Double,       // Latitude in degrees
        val longitude: Double,      // Longitude in degrees
        val speedKmh: Float,        // Speed in km/h
        val heading: Int,           // Course angle 0..359 degrees
        val isIgnitionOn: Boolean,  // Bit 0 of flags
        val isTripStart: Boolean,   // Bit 1 of flags
        val isTripEnd: Boolean,     // Bit 2 of flags
        val satCount: Int           // Bits 3..7 of flags
    )

    data class LiveTelemetryPacket(
        val sequenceNumber: Int,
        val tripPoint: ParsedTripPoint,
        val isValidCrc: Boolean
    )

    data class HistoryChunkPacket(
        val chunkId: Int,
        val totalChunks: Int,
        val count: Int,
        val tripPoints: List<ParsedTripPoint>,
        val isValidCrc: Boolean
    )

    /**
     * Decodes a 16-byte packed TripPoint struct from raw bytes at offset.
     * Uses Little Endian byte order matching ESP32 GCC struct layout.
     */
    fun decodeTripPoint(data: ByteArray, offset: Int = 0): ParsedTripPoint {
        val buffer = ByteBuffer.wrap(data, offset, TRIP_POINT_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        // uint32_t timestamp (Bytes 0..3)
        val timestamp = buffer.int.toLong() and 0xFFFFFFFFL

        // int32_t lat_e7 (Bytes 4..7)
        val latE7 = buffer.int
        val latitude = latE7 / 10_000_000.0

        // int32_t lon_e7 (Bytes 8..11)
        val lonE7 = buffer.int
        val longitude = lonE7 / 10_000_000.0

        // uint16_t speed_decideg (Bytes 12..13) -> Speed in km/h * 10
        val speedDecideg = buffer.short.toInt() and 0xFFFF
        val speedKmh = speedDecideg / 10.0f

        // uint8_t heading_div2 (Byte 14) -> Heading / 2
        val headingDiv2 = buffer.get().toInt() and 0xFF
        val heading = (headingDiv2 * 2) % 360

        // uint8_t flags (Byte 15)
        val flags = buffer.get().toInt() and 0xFF
        val isIgnitionOn = (flags and 0x01) != 0
        val isTripStart = (flags and 0x02) != 0
        val isTripEnd = (flags and 0x04) != 0
        val satCount = (flags shr 3) and 0x1F

        return ParsedTripPoint(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            heading = heading,
            isIgnitionOn = isIgnitionOn,
            isTripStart = isTripStart,
            isTripEnd = isTripEnd,
            satCount = satCount
        )
    }

    /**
     * Encodes a ParsedTripPoint into a 16-byte packed byte array (Little Endian).
     */
    fun encodeTripPoint(point: ParsedTripPoint): ByteArray {
        val buffer = ByteBuffer.allocate(TRIP_POINT_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt((point.timestamp and 0xFFFFFFFFL).toInt())
        buffer.putInt((point.latitude * 10_000_000.0).toInt())
        buffer.putInt((point.longitude * 10_000_000.0).toInt())
        buffer.putShort(((point.speedKmh * 10.0f).toInt() and 0xFFFF).toShort())
        buffer.put((point.heading / 2).toByte())

        var flags = 0
        if (point.isIgnitionOn) flags = flags or 0x01
        if (point.isTripStart) flags = flags or 0x02
        if (point.isTripEnd) flags = flags or 0x04
        flags = flags or ((point.satCount and 0x1F) shl 3)

        buffer.put(flags.toByte())

        return buffer.array()
    }

    /**
     * Decodes 20/21-byte Live Telemetry Packet (0x02).
     */
    fun decodeLiveTelemetry(data: ByteArray): LiveTelemetryPacket? {
        if (data.size < 19) return null

        val offset = if (data[0] == OPCODE_LIVE_TELEMETRY) 1 else 0
        if (data.size < offset + 18) return null

        val buffer = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN)
        val seqNum = buffer.short.toInt() and 0xFFFF

        val tripPoint = decodeTripPoint(data, offset + 2)

        val isValidCrc = if (data.size >= offset + 20) {
            val crcCalculated = computeCrc16(data, 0, data.size - 2)
            val crcReceived = ((data[data.size - 2].toInt() and 0xFF) or ((data[data.size - 1].toInt() and 0xFF) shl 8))
            crcCalculated == crcReceived
        } else true

        return LiveTelemetryPacket(
            sequenceNumber = seqNum,
            tripPoint = tripPoint,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Encodes Live Telemetry Packet (0x02).
     */
    fun encodeLiveTelemetry(sequenceNumber: Int, point: ParsedTripPoint): ByteArray {
        val payload = ByteBuffer.allocate(LIVE_TELEMETRY_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        payload.put(OPCODE_LIVE_TELEMETRY)
        payload.putShort((sequenceNumber and 0xFFFF).toShort())

        val pointBytes = encodeTripPoint(point)
        payload.put(pointBytes)

        val crc = computeCrc16(payload.array(), 0, LIVE_TELEMETRY_SIZE - 2)
        payload.putShort(crc.toShort())

        return payload.array()
    }

    /**
     * Decodes 488-byte History Chunk Packet (0x03).
     */
    fun decodeHistoryChunk(data: ByteArray): HistoryChunkPacket? {
        if (data.size < 6) return null

        val offset = if (data[0] == OPCODE_HISTORY_CHUNK) 1 else 0
        val buffer = ByteBuffer.wrap(data, offset, 5).order(ByteOrder.LITTLE_ENDIAN)

        val chunkId = buffer.short.toInt() and 0xFFFF
        val totalChunks = buffer.short.toInt() and 0xFFFF
        val count = buffer.get().toInt() and 0xFF

        val pointsList = mutableListOf<ParsedTripPoint>()
        var pointOffset = offset + 5

        for (i in 0 until minOf(count, 30)) {
            if (pointOffset + TRIP_POINT_SIZE <= data.size) {
                pointsList.add(decodeTripPoint(data, pointOffset))
                pointOffset += TRIP_POINT_SIZE
            }
        }

        val isValidCrc = if (data.size >= HISTORY_CHUNK_SIZE) {
            val crcCalculated = computeCrc16(data, 0, data.size - 2)
            val crcReceived = ((data[data.size - 2].toInt() and 0xFF) or ((data[data.size - 1].toInt() and 0xFF) shl 8))
            crcCalculated == crcReceived
        } else true

        return HistoryChunkPacket(
            chunkId = chunkId,
            totalChunks = totalChunks,
            count = count,
            tripPoints = pointsList,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Encodes 488-byte History Chunk Packet (0x03).
     */
    fun encodeHistoryChunk(chunkId: Int, totalChunks: Int, points: List<ParsedTripPoint>): ByteArray {
        val buffer = ByteBuffer.allocate(HISTORY_CHUNK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(OPCODE_HISTORY_CHUNK)
        buffer.putShort((chunkId and 0xFFFF).toShort())
        buffer.putShort((totalChunks and 0xFFFF).toShort())

        val count = minOf(points.size, 30)
        buffer.put(count.toByte())

        for (i in 0 until count) {
            buffer.put(encodeTripPoint(points[i]))
        }

        val remainingBytes = 480 - (count * TRIP_POINT_SIZE)
        if (remainingBytes > 0) {
            buffer.put(ByteArray(remainingBytes))
        }

        val crc = computeCrc16(buffer.array(), 0, HISTORY_CHUNK_SIZE - 2)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Calculates CRC16-CCITT (0x1021) polynomial checksum.
     */
    fun computeCrc16(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
            for (j in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
            }
        }
        return crc and 0xFFFF
    }

    /**
     * Verifies if last 2 bytes of payload match calculated CRC16.
     */
    fun verifyCrc16(data: ByteArray): Boolean {
        if (data.size < 2) return false
        val calculated = computeCrc16(data, 0, data.size - 2)
        val received = ((data[data.size - 2].toInt() and 0xFF) or ((data[data.size - 1].toInt() and 0xFF) shl 8))
        return calculated == received
    }
}
