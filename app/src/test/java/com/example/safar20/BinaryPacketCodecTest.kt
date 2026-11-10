package com.example.safar20

import com.example.safar20.data.network.BinaryPacketCodec
import com.example.safar20.data.network.BinaryPacketCodec.ParsedTripPoint
import com.example.safar20.data.network.CryptoEngine
import com.example.safar20.data.network.MqttTelemetrySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryPacketCodecTest {

    @Test
    fun testGoldenCrc16Vector() {
        // Standard CRC16-CCITT-FALSE test vector "123456789" -> 0x29B1
        val testInput = "123456789".toByteArray(Charsets.US_ASCII)
        val calculatedCrc = BinaryPacketCodec.computeCrc16(testInput)
        assertEquals(0x29B1, calculatedCrc)
    }

    @Test
    fun testGoldenOpenSessionVector() {
        // Construct a known Golden OpenSession Packet (9 bytes)
        // Opcode: 0x01, LastSyncedTS: 1700000000 (0x6554F100), Seq: 42 (0x002A)
        val lastSyncedTs = 1700000000L
        val seqNum = 42

        val encoded = BinaryPacketCodec.encodeOpenSession(lastSyncedTs, seqNum)
        assertEquals(9, encoded.size)
        assertEquals(BinaryPacketCodec.OPCODE_OPEN_SESSION, encoded[0])

        // Verify buffer layout manually
        val buffer = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x01.toByte(), buffer.get())
        assertEquals(1700000000L, buffer.int.toLong() and 0xFFFFFFFFL)
        assertEquals(42, buffer.short.toInt() and 0xFFFF)

        val decoded = BinaryPacketCodec.decodeOpenSession(encoded)
        assertNotNull(decoded)
        assertEquals(1700000000L, decoded!!.lastSyncedTimestamp)
        assertEquals(42, decoded.sequenceNumber)
        assertTrue(decoded.isValidCrc)
    }

    @Test
    fun testTripPointAndLiveTelemetryRoundtripAndRounding() {
        // Point with fractional digits testing roundToInt()
        val originalPoint = ParsedTripPoint(
            timestamp = 1710000000L,
            latitude = 12.34567899,
            longitude = 77.12345674,
            speedKmh = 65.44f,
            heading = 180,
            isIgnitionOn = true,
            isTripStart = false,
            isTripEnd = true,
            satCount = 12
        )

        val seqNum = 100
        val encodedLive = BinaryPacketCodec.encodeLiveTelemetry(seqNum, originalPoint)
        assertEquals(21, encodedLive.size)
        assertEquals(BinaryPacketCodec.OPCODE_LIVE_TELEMETRY, encodedLive[0])

        val decodedLive = BinaryPacketCodec.decodeLiveTelemetry(encodedLive)
        assertNotNull(decodedLive)
        assertTrue(decodedLive!!.isValidCrc)
        assertEquals(seqNum, decodedLive.sequenceNumber)

        val decodedPoint = decodedLive.tripPoint
        assertEquals(1710000000L, decodedPoint.timestamp)
        // 12.34567899 * 10_000_000 rounded = 123456790 -> 12.345679
        assertEquals(12.3456790, decodedPoint.latitude, 1e-7)
        // 77.12345674 * 10_000_000 rounded = 771234567 -> 77.1234567
        assertEquals(77.1234567, decodedPoint.longitude, 1e-7)
        // 65.44f * 10 rounded = 654 -> 65.4f
        assertEquals(65.4f, decodedPoint.speedKmh, 1e-4f)
        assertEquals(180, decodedPoint.heading)
        assertTrue(decodedPoint.isIgnitionOn)
        assertFalse(decodedPoint.isTripStart)
        assertTrue(decodedPoint.isTripEnd)
        assertEquals(12, decodedPoint.satCount)
    }

    @Test
    fun testNegativeCoordinatesAndBoundaryValues() {
        // Southern & Western hemisphere (Sydney: ~ -33.8688123, -151.2092955)
        val southernPoint = ParsedTripPoint(
            timestamp = 1710000000L,
            latitude = -33.8688123,
            longitude = -151.2092955,
            speedKmh = 120.0f,
            heading = 270,
            isIgnitionOn = true,
            isTripStart = true,
            isTripEnd = false,
            satCount = 15
        )

        val encodedSouthern = BinaryPacketCodec.encodeTripPoint(southernPoint)
        val decodedSouthern = BinaryPacketCodec.decodeTripPoint(encodedSouthern)

        assertEquals(-33.8688123, decodedSouthern.latitude, 1e-7)
        assertEquals(-151.2092955, decodedSouthern.longitude, 1e-7)
        assertEquals(120.0f, decodedSouthern.speedKmh, 1e-4f)
        assertEquals(270, decodedSouthern.heading)

        // Extreme Boundaries (-90.0, 180.0, 0.0)
        val boundaryPoint = ParsedTripPoint(
            timestamp = 0L,
            latitude = -90.0,
            longitude = 180.0,
            speedKmh = 0.0f,
            heading = 0,
            isIgnitionOn = false,
            isTripStart = false,
            isTripEnd = false,
            satCount = 0
        )

        val encodedBoundary = BinaryPacketCodec.encodeTripPoint(boundaryPoint)
        val decodedBoundary = BinaryPacketCodec.decodeTripPoint(encodedBoundary)

        assertEquals(-90.0, decodedBoundary.latitude, 1e-7)
        assertEquals(180.0, decodedBoundary.longitude, 1e-7)
        assertEquals(0.0f, decodedBoundary.speedKmh, 1e-4f)

        // Range Coercion Check
        val outOfBoundsPoint = ParsedTripPoint(
            timestamp = 100L,
            latitude = -95.0,     // Should coerce to -90.0
            longitude = 185.0,    // Should coerce to 180.0
            speedKmh = -10.0f,    // Should coerce to 0.0f
            heading = 370,        // 370 mod 360 = 10
            isIgnitionOn = true,
            isTripStart = false,
            isTripEnd = false,
            satCount = 35        // 35 and 0x1F = 3
        )

        val encodedCoerced = BinaryPacketCodec.encodeTripPoint(outOfBoundsPoint)
        val decodedCoerced = BinaryPacketCodec.decodeTripPoint(encodedCoerced)

        assertEquals(-90.0, decodedCoerced.latitude, 1e-7)
        assertEquals(180.0, decodedCoerced.longitude, 1e-7)
        assertEquals(0.0f, decodedCoerced.speedKmh, 1e-4f)
        assertEquals(10, decodedCoerced.heading)
        assertEquals(3, decodedCoerced.satCount)
    }

    @Test
    fun testTruncatedPacketsReturnNull() {
        val validLive = BinaryPacketCodec.encodeLiveTelemetry(
            1,
            ParsedTripPoint(1710000000L, 12.0, 77.0, 50f, 90, true, false, false, 8)
        )

        // Truncated live telemetry packets (19B and 20B)
        assertNull(BinaryPacketCodec.decodeLiveTelemetry(validLive.copyOf(19)))
        assertNull(BinaryPacketCodec.decodeLiveTelemetry(validLive.copyOf(20)))

        // Truncated open session (8B instead of 9B)
        val validOpenSession = BinaryPacketCodec.encodeOpenSession(1700000000L, 1)
        assertNull(BinaryPacketCodec.decodeOpenSession(validOpenSession.copyOf(8)))

        // Truncated history chunk (100B and 487B instead of 488B)
        val validChunk = BinaryPacketCodec.encodeHistoryChunk(1, 1, emptyList())
        assertNull(BinaryPacketCodec.decodeHistoryChunk(validChunk.copyOf(100)))
        assertNull(BinaryPacketCodec.decodeHistoryChunk(validChunk.copyOf(487)))

        // Truncated history ack (6B instead of 7B)
        val validAck = BinaryPacketCodec.encodeHistoryAck(1, 0)
        assertNull(BinaryPacketCodec.decodeHistoryAck(validAck.copyOf(6)))

        // Truncated heartbeat (8B instead of 9B)
        val validHeartbeat = BinaryPacketCodec.encodeHeartbeat(1700000000L, 1)
        assertNull(BinaryPacketCodec.decodeHeartbeat(validHeartbeat.copyOf(8)))

        // Wrong Opcode returns null
        val wrongOpcodeLive = validLive.copyOf()
        wrongOpcodeLive[0] = 0x99.toByte()
        assertNull(BinaryPacketCodec.decodeLiveTelemetry(wrongOpcodeLive))
    }

    @Test
    fun testBadCrcFlippedPayload() {
        val point = ParsedTripPoint(1710000000L, 12.34, 77.56, 40.0f, 180, true, false, false, 10)
        val validPacket = BinaryPacketCodec.encodeLiveTelemetry(5, point)

        val decodedValid = BinaryPacketCodec.decodeLiveTelemetry(validPacket)
        assertNotNull(decodedValid)
        assertTrue(decodedValid!!.isValidCrc)
        assertTrue(BinaryPacketCodec.verifyCrc16(validPacket))

        // Corrupt 1 bit in payload (byte 5)
        val corruptedPacket = validPacket.copyOf()
        corruptedPacket[5] = (corruptedPacket[5].toInt() xor 0x01).toByte()

        val decodedCorrupted = BinaryPacketCodec.decodeLiveTelemetry(corruptedPacket)
        assertNotNull(decodedCorrupted)
        assertFalse(decodedCorrupted!!.isValidCrc)
        assertFalse(BinaryPacketCodec.verifyCrc16(corruptedPacket))
    }

    @Test
    fun testHistoryChunkRoundtripAndCountValidation() {
        val points = (1..5).map { i ->
            ParsedTripPoint(
                timestamp = 1710000000L + i,
                latitude = 12.0 + i * 0.01,
                longitude = 77.0 + i * 0.01,
                speedKmh = 30f + i,
                heading = i * 10,
                isIgnitionOn = true,
                isTripStart = (i == 1),
                isTripEnd = (i == 5),
                satCount = 10
            )
        }

        val encodedChunk = BinaryPacketCodec.encodeHistoryChunk(chunkId = 3, totalChunks = 10, points = points)
        assertEquals(488, encodedChunk.size)
        assertEquals(BinaryPacketCodec.OPCODE_HISTORY_CHUNK, encodedChunk[0])

        val decodedChunk = BinaryPacketCodec.decodeHistoryChunk(encodedChunk)
        assertNotNull(decodedChunk)
        assertEquals(3, decodedChunk!!.chunkId)
        assertEquals(10, decodedChunk.totalChunks)
        assertEquals(5, decodedChunk.count)
        assertEquals(5, decodedChunk.tripPoints.size)
        assertTrue(decodedChunk.isValidCrc)

        // Verify point 1
        assertEquals(12.01, decodedChunk.tripPoints[0].latitude, 1e-7)
        assertTrue(decodedChunk.tripPoints[0].isTripStart)
        // Verify point 5
        assertEquals(12.05, decodedChunk.tripPoints[4].latitude, 1e-7)
        assertTrue(decodedChunk.tripPoints[4].isTripEnd)

        // Test rejecting count > 30 (e.g. count = 31 or count = 255)
        val badCount31 = encodedChunk.copyOf()
        badCount31[5] = 31.toByte() // Byte 5 is Count
        assertNull(BinaryPacketCodec.decodeHistoryChunk(badCount31))

        val badCount255 = encodedChunk.copyOf()
        badCount255[5] = 255.toByte() // 0xFF
        assertNull(BinaryPacketCodec.decodeHistoryChunk(badCount255))
    }

    @Test
    fun testOpenSessionAndHistoryAckRoundtrip() {
        // OpenSession (9B)
        val openSessionBytes = BinaryPacketCodec.encodeOpenSession(
            lastSyncedTimestamp = 1705000000L,
            sequenceNumber = 99
        )
        assertEquals(9, openSessionBytes.size)

        val decodedOpenSession = BinaryPacketCodec.decodeOpenSession(openSessionBytes)
        assertNotNull(decodedOpenSession)
        assertEquals(1705000000L, decodedOpenSession!!.lastSyncedTimestamp)
        assertEquals(99, decodedOpenSession.sequenceNumber)
        assertTrue(decodedOpenSession.isValidCrc)

        // HistoryAck (7B)
        val historyAckBytes = BinaryPacketCodec.encodeHistoryAck(
            chunkId = 12,
            status = 0, // OK
            reserved = 0
        )
        assertEquals(7, historyAckBytes.size)

        val decodedHistoryAck = BinaryPacketCodec.decodeHistoryAck(historyAckBytes)
        assertNotNull(decodedHistoryAck)
        assertEquals(12, decodedHistoryAck!!.chunkId)
        assertEquals(0, decodedHistoryAck.status)
        assertEquals(0, decodedHistoryAck.reserved)
        assertTrue(decodedHistoryAck.isValidCrc)

        // Heartbeat (9B)
        val heartbeatBytes = BinaryPacketCodec.encodeHeartbeat(
            timestamp = 1705000000L,
            sequenceNumber = 77
        )
        assertEquals(9, heartbeatBytes.size)

        val decodedHeartbeat = BinaryPacketCodec.decodeHeartbeat(heartbeatBytes)
        assertNotNull(decodedHeartbeat)
        assertEquals(1705000000L, decodedHeartbeat!!.timestamp)
        assertEquals(77, decodedHeartbeat.sequenceNumber)
        assertTrue(decodedHeartbeat.isValidCrc)
    }

    @Test
    fun testClearFlashRoundtrip() {
        val encoded = BinaryPacketCodec.encodeClearFlash(
            upToTimestamp = 1710000000L,
            mode = 0,
            sequenceNumber = 12
        )
        assertEquals(10, encoded.size)
        assertEquals(BinaryPacketCodec.OPCODE_CLEAR_FLASH, encoded[0])

        val decoded = BinaryPacketCodec.decodeClearFlash(encoded)
        assertNotNull(decoded)
        assertEquals(1710000000L, decoded!!.upToTimestamp)
        assertEquals(0, decoded.mode)
        assertEquals(12, decoded.sequenceNumber)
        assertTrue(decoded.isValidCrc)
    }

    @Test
    fun testAeadEncryptedFrameExactSizes() {
        val dummySecret = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"

        // 1. Live Telemetry (21B plain) -> 47B encrypted frame
        val livePoint = ParsedTripPoint(1710000000L, 12.34, 77.56, 60.0f, 180, true, true, false, 12)
        val plainLive = BinaryPacketCodec.encodeLiveTelemetry(1, livePoint)
        assertEquals(21, plainLive.size)

        val encryptedLive = CryptoEngine.encryptFrame(plainLive, dummySecret)
        assertNotNull(encryptedLive)
        assertEquals(47, encryptedLive!!.size)
        assertEquals(BinaryPacketCodec.OPCODE_LIVE_TELEMETRY, encryptedLive[0])

        val decryptedLive = CryptoEngine.decryptFrame(encryptedLive, dummySecret)
        assertNotNull(decryptedLive)
        assertEquals(21, decryptedLive!!.size)

        val decodedLive = BinaryPacketCodec.decodeLiveTelemetry(decryptedLive)
        assertNotNull(decodedLive)
        assertTrue(decodedLive!!.isValidCrc)
        assertEquals(12.34, decodedLive.tripPoint.latitude, 1e-7)

        // 2. History Chunk (488B plain) -> 514B encrypted frame
        val plainChunk = BinaryPacketCodec.encodeHistoryChunk(1, 1, listOf(livePoint))
        assertEquals(488, plainChunk.size)

        val encryptedChunk = CryptoEngine.encryptFrame(plainChunk, dummySecret)
        assertNotNull(encryptedChunk)
        assertEquals(514, encryptedChunk!!.size)
        assertEquals(BinaryPacketCodec.OPCODE_HISTORY_CHUNK, encryptedChunk[0])

        val decryptedChunk = CryptoEngine.decryptFrame(encryptedChunk, dummySecret)
        assertNotNull(decryptedChunk)
        assertEquals(488, decryptedChunk!!.size)

        val decodedChunk = BinaryPacketCodec.decodeHistoryChunk(decryptedChunk)
        assertNotNull(decodedChunk)
        assertTrue(decodedChunk!!.isValidCrc)
        assertEquals(1, decodedChunk.count)
    }

    @Test
    fun testDeriveDeviceTopicIdVector() {
        val pubKey = "4d8a1c92b8e3f7a1029c83b4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4"
        val derivedId = MqttTelemetrySource.deriveDeviceTopicId("SAFAR_01", pubKey)
        assertEquals("a996b58b008271c7", derivedId)

        val fallbackId = MqttTelemetrySource.deriveDeviceTopicId("SAFAR_01", "")
        assertEquals("safar_01", fallbackId)
    }
}
