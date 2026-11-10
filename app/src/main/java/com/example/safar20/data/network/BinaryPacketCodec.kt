package com.example.safar20.data.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Binary Packet Codec for SAFAR 2.0 Wire Protocol.
 * Handles encoding/decoding of 16-byte packed TripPoint structs,
 * 9-byte OpenSession packets, 21-byte Live Telemetry packets,
 * 488-byte History Chunks, 7-byte History ACK packets, 9-byte Heartbeats,
 * and 10-byte ClearFlash packets.
 */
object BinaryPacketCodec {

    const val OPCODE_OPEN_SESSION: Byte = 0x01
    const val OPCODE_LIVE_TELEMETRY: Byte = 0x02
    const val OPCODE_HISTORY_CHUNK: Byte = 0x03
    const val OPCODE_HISTORY_ACK: Byte = 0x04
    const val OPCODE_HEARTBEAT: Byte = 0x05
    const val OPCODE_CLEAR_FLASH: Byte = 0x06

    const val TRIP_POINT_SIZE = 16
    const val OPEN_SESSION_SIZE = 9     // 1B Opcode + 4B LastSyncedTS + 2B Seq + 2B CRC16
    const val LIVE_TELEMETRY_SIZE = 21 // 1B Opcode + 2B Seq + 16B TripPoint + 2B CRC16
    const val HISTORY_CHUNK_SIZE = 488 // 1B Opcode + 2B ChunkID + 2B TotalChunks + 1B Count + 480B Points + 2B CRC16
    const val HISTORY_ACK_SIZE = 7     // 1B Opcode + 2B ChunkID + 1B Status + 1B Reserved + 2B CRC16
    const val HEARTBEAT_SIZE = 9       // 1B Opcode + 4B Timestamp + 2B Seq + 2B CRC16
    const val CLEAR_FLASH_SIZE = 10    // 1B Opcode + 4B UpToTimestamp + 1B Mode + 2B Seq + 2B CRC16

    data class ParsedTripPoint(
        val timestamp: Long,        // Unix Epoch Seconds (UTC)
        val latitude: Double,       // Latitude in degrees [-90.0..90.0]
        val longitude: Double,      // Longitude in degrees [-180.0..180.0]
        val speedKmh: Float,        // Speed in km/h
        val heading: Int,           // Course angle 0..359 degrees
        val isIgnitionOn: Boolean,  // Bit 0 of flags
        val isTripStart: Boolean,   // Bit 1 of flags
        val isTripEnd: Boolean,     // Bit 2 of flags
        val satCount: Int           // Bits 3..7 of flags
    )

    data class OpenSessionPacket(
        val lastSyncedTimestamp: Long,
        val sequenceNumber: Int,
        val isValidCrc: Boolean
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

    data class HistoryAckPacket(
        val chunkId: Int,
        val status: Int,
        val reserved: Int,
        val isValidCrc: Boolean
    )

    data class HeartbeatPacket(
        val timestamp: Long,
        val sequenceNumber: Int,
        val isValidCrc: Boolean
    )

    data class ClearFlashPacket(
        val upToTimestamp: Long,
        val mode: Int,
        val sequenceNumber: Int,
        val isValidCrc: Boolean
    )

    /**
     * Extracts inner payload from a plain packet by stripping 1B Opcode (at offset 0)
     * and 2B trailing CRC16 (at the last 2 bytes).
     */
    fun getInnerPayload(plainPacket: ByteArray): ByteArray? {
        if (plainPacket.size < 4) return null
        val plainSize = getPlainPacketSize(plainPacket[0])
        if (plainSize > 0 && plainPacket.size < plainSize) return null
        return plainPacket.copyOfRange(1, plainPacket.size - 2)
    }

    /**
     * Reconstructs a plain packet from opcode and inner payload by attaching
     * 1B opcode at index 0 and computing/appending 2B CRC16 at the end.
     */
    fun reconstructPlainPacket(opcode: Byte, innerPayload: ByteArray): ByteArray {
        val result = ByteArray(1 + innerPayload.size + 2)
        result[0] = opcode
        System.arraycopy(innerPayload, 0, result, 1, innerPayload.size)

        val crc = computeCrc16(result, 0, 1 + innerPayload.size)
        val crcPos = 1 + innerPayload.size
        result[crcPos] = (crc and 0xFF).toByte()
        result[crcPos + 1] = ((crc shr 8) and 0xFF).toByte()

        return result
    }

    fun getPlainPacketSize(opcode: Byte): Int {
        return when (opcode) {
            OPCODE_OPEN_SESSION -> OPEN_SESSION_SIZE
            OPCODE_LIVE_TELEMETRY -> LIVE_TELEMETRY_SIZE
            OPCODE_HISTORY_CHUNK -> HISTORY_CHUNK_SIZE
            OPCODE_HISTORY_ACK -> HISTORY_ACK_SIZE
            OPCODE_HEARTBEAT -> HEARTBEAT_SIZE
            OPCODE_CLEAR_FLASH -> CLEAR_FLASH_SIZE
            else -> 0
        }
    }

    /**
     * Decodes a 16-byte packed TripPoint struct from raw bytes at offset.
     */
    fun decodeTripPoint(data: ByteArray, offset: Int = 0): ParsedTripPoint {
        val buffer = ByteBuffer.wrap(data, offset, TRIP_POINT_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        val timestamp = buffer.int.toLong() and 0xFFFFFFFFL

        val latE7 = buffer.int
        val latitude = latE7 / 10_000_000.0

        val lonE7 = buffer.int
        val longitude = lonE7 / 10_000_000.0

        val speedDecideg = buffer.short.toInt() and 0xFFFF
        val speedKmh = speedDecideg / 10.0f

        val headingDiv2 = buffer.get().toInt() and 0xFF
        val heading = (headingDiv2 * 2) % 360

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

        val latClamped = point.latitude.coerceIn(-90.0, 90.0)
        buffer.putInt((latClamped * 10_000_000.0).roundToInt())

        val lonClamped = point.longitude.coerceIn(-180.0, 180.0)
        buffer.putInt((lonClamped * 10_000_000.0).roundToInt())

        val speedClamped = point.speedKmh.coerceAtLeast(0f)
        val speedDecideg = (speedClamped * 10.0f).roundToInt() and 0xFFFF
        buffer.putShort(speedDecideg.toShort())

        val headingMod = point.heading.mod(360)
        buffer.put((headingMod / 2).toByte())

        var flags = 0
        if (point.isIgnitionOn) flags = flags or 0x01
        if (point.isTripStart) flags = flags or 0x02
        if (point.isTripEnd) flags = flags or 0x04
        flags = flags or ((point.satCount and 0x1F) shl 3)

        buffer.put(flags.toByte())

        return buffer.array()
    }

    /**
     * Encodes 9-byte Open Session Packet (0x01).
     */
    fun encodeOpenSession(lastSyncedTimestamp: Long, sequenceNumber: Int): ByteArray {
        val buffer = ByteBuffer.allocate(OPEN_SESSION_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(OPCODE_OPEN_SESSION)
        buffer.putInt((lastSyncedTimestamp and 0xFFFFFFFFL).toInt())
        buffer.putShort((sequenceNumber and 0xFFFF).toShort())

        val crc = computeCrc16(buffer.array(), 0, OPEN_SESSION_SIZE - 2)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Decodes 9-byte Open Session Packet (0x01).
     */
    fun decodeOpenSession(data: ByteArray): OpenSessionPacket? {
        if (data.size < OPEN_SESSION_SIZE || data[0] != OPCODE_OPEN_SESSION) return null

        val buffer = ByteBuffer.wrap(data, 1, OPEN_SESSION_SIZE - 1).order(ByteOrder.LITTLE_ENDIAN)
        val lastSyncedTs = buffer.int.toLong() and 0xFFFFFFFFL
        val seqNum = buffer.short.toInt() and 0xFFFF

        val isValidCrc = verifyCrc16(data)

        return OpenSessionPacket(
            lastSyncedTimestamp = lastSyncedTs,
            sequenceNumber = seqNum,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Encodes 21-byte Live Telemetry Packet (0x02).
     */
    fun encodeLiveTelemetry(sequenceNumber: Int, point: ParsedTripPoint): ByteArray {
        val buffer = ByteBuffer.allocate(LIVE_TELEMETRY_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(OPCODE_LIVE_TELEMETRY)
        buffer.putShort((sequenceNumber and 0xFFFF).toShort())

        val pointBytes = encodeTripPoint(point)
        buffer.put(pointBytes)

        val crc = computeCrc16(buffer.array(), 0, LIVE_TELEMETRY_SIZE - 2)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Decodes 21-byte Live Telemetry Packet (0x02).
     */
    fun decodeLiveTelemetry(data: ByteArray): LiveTelemetryPacket? {
        if (data.size < LIVE_TELEMETRY_SIZE || data[0] != OPCODE_LIVE_TELEMETRY) return null

        val buffer = ByteBuffer.wrap(data, 1, 2).order(ByteOrder.LITTLE_ENDIAN)
        val seqNum = buffer.short.toInt() and 0xFFFF

        val tripPoint = decodeTripPoint(data, offset = 3)
        val isValidCrc = verifyCrc16(data)

        return LiveTelemetryPacket(
            sequenceNumber = seqNum,
            tripPoint = tripPoint,
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
     * Decodes 488-byte History Chunk Packet (0x03).
     */
    fun decodeHistoryChunk(data: ByteArray): HistoryChunkPacket? {
        if (data.size < HISTORY_CHUNK_SIZE || data[0] != OPCODE_HISTORY_CHUNK) return null

        val buffer = ByteBuffer.wrap(data, 1, 5).order(ByteOrder.LITTLE_ENDIAN)
        val chunkId = buffer.short.toInt() and 0xFFFF
        val totalChunks = buffer.short.toInt() and 0xFFFF
        val rawCount = buffer.get().toInt()
        val count = rawCount and 0xFF

        if (rawCount < 0 || count > 30) return null

        val pointsList = ArrayList<ParsedTripPoint>(count)
        var pointOffset = 6

        for (i in 0 until count) {
            pointsList.add(decodeTripPoint(data, pointOffset))
            pointOffset += TRIP_POINT_SIZE
        }

        val isValidCrc = verifyCrc16(data)

        return HistoryChunkPacket(
            chunkId = chunkId,
            totalChunks = totalChunks,
            count = count,
            tripPoints = pointsList,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Encodes 7-byte History ACK Packet (0x04).
     */
    fun encodeHistoryAck(chunkId: Int, status: Int, reserved: Int = 0): ByteArray {
        val buffer = ByteBuffer.allocate(HISTORY_ACK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(OPCODE_HISTORY_ACK)
        buffer.putShort((chunkId and 0xFFFF).toShort())
        buffer.put(status.toByte())
        buffer.put(reserved.toByte())

        val crc = computeCrc16(buffer.array(), 0, HISTORY_ACK_SIZE - 2)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Decodes 7-byte History ACK Packet (0x04).
     */
    fun decodeHistoryAck(data: ByteArray): HistoryAckPacket? {
        if (data.size < HISTORY_ACK_SIZE || data[0] != OPCODE_HISTORY_ACK) return null

        val buffer = ByteBuffer.wrap(data, 1, 4).order(ByteOrder.LITTLE_ENDIAN)
        val chunkId = buffer.short.toInt() and 0xFFFF
        val status = buffer.get().toInt() and 0xFF
        val reserved = buffer.get().toInt() and 0xFF

        val isValidCrc = verifyCrc16(data)

        return HistoryAckPacket(
            chunkId = chunkId,
            status = status,
            reserved = reserved,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Encodes 9-byte Heartbeat Packet (0x05).
     */
    fun encodeHeartbeat(timestamp: Long, sequenceNumber: Int): ByteArray {
        val buffer = ByteBuffer.allocate(HEARTBEAT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(OPCODE_HEARTBEAT)
        buffer.putInt((timestamp and 0xFFFFFFFFL).toInt())
        buffer.putShort((sequenceNumber and 0xFFFF).toShort())

        val crc = computeCrc16(buffer.array(), 0, HEARTBEAT_SIZE - 2)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Decodes 9-byte Heartbeat Packet (0x05).
     */
    fun decodeHeartbeat(data: ByteArray): HeartbeatPacket? {
        if (data.size < HEARTBEAT_SIZE || data[0] != OPCODE_HEARTBEAT) return null

        val buffer = ByteBuffer.wrap(data, 1, HEARTBEAT_SIZE - 1).order(ByteOrder.LITTLE_ENDIAN)
        val timestamp = buffer.int.toLong() and 0xFFFFFFFFL
        val seqNum = buffer.short.toInt() and 0xFFFF

        val isValidCrc = verifyCrc16(data)

        return HeartbeatPacket(
            timestamp = timestamp,
            sequenceNumber = seqNum,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Encodes 10-byte Clear Flash Packet (0x06).
     */
    fun encodeClearFlash(upToTimestamp: Long, mode: Int = 0, sequenceNumber: Int = 1): ByteArray {
        val buffer = ByteBuffer.allocate(CLEAR_FLASH_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(OPCODE_CLEAR_FLASH)
        buffer.putInt((upToTimestamp and 0xFFFFFFFFL).toInt())
        buffer.put((mode and 0xFF).toByte())
        buffer.putShort((sequenceNumber and 0xFFFF).toShort())

        val crc = computeCrc16(buffer.array(), 0, CLEAR_FLASH_SIZE - 2)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Decodes 10-byte Clear Flash Packet (0x06).
     */
    fun decodeClearFlash(data: ByteArray): ClearFlashPacket? {
        if (data.size < CLEAR_FLASH_SIZE || data[0] != OPCODE_CLEAR_FLASH) return null

        val buffer = ByteBuffer.wrap(data, 1, CLEAR_FLASH_SIZE - 1).order(ByteOrder.LITTLE_ENDIAN)
        val upToTs = buffer.int.toLong() and 0xFFFFFFFFL
        val mode = buffer.get().toInt() and 0xFF
        val seqNum = buffer.short.toInt() and 0xFFFF

        val isValidCrc = verifyCrc16(data)

        return ClearFlashPacket(
            upToTimestamp = upToTs,
            mode = mode,
            sequenceNumber = seqNum,
            isValidCrc = isValidCrc
        )
    }

    /**
     * Calculates CRC16-CCITT-FALSE (poly 0x1021, init 0xFFFF, no reflection).
     */
    fun computeCrc16(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
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
     * Verifies if last 2 bytes of payload match calculated CRC16 (stored Little-Endian).
     */
    fun verifyCrc16(data: ByteArray): Boolean {
        if (data.size < 2) return false
        val calculated = computeCrc16(data, 0, data.size - 2)
        val received = ((data[data.size - 2].toInt() and 0xFF) or ((data[data.size - 1].toInt() and 0xFF) shl 8))
        return calculated == received
    }
}
