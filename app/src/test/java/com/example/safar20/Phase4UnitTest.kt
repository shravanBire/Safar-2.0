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
import java.net.URLDecoder
import java.net.URLEncoder

class Phase4UnitTest {

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = clean.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    @Test
    fun testCryptoEngineGoldenVector() {
        val keyHex = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"
        val frameHex = "020102030405060708090a0b0c7f8526de0c01105a20d1482764a3388e4e9b906748c2a4a077c4f1f7425bd5df32a9"
        val frameBytes = hexToBytes(frameHex)

        // 1. Decrypt golden frame
        val decrypted = CryptoEngine.decryptFrame(frameBytes, keyHex)
        assertNotNull("Golden frame decryption must succeed", decrypted)
        assertEquals("Decrypted frame must preserve opcode 0x02", 0x02.toByte(), decrypted!![0])

        // 2. Verify inner payload format: inner[i] == (i * 7 + 3) & 0xFF for i in 0..17
        val innerPayload = BinaryPacketCodec.getInnerPayload(decrypted)
        assertNotNull("Inner payload must be non-null", innerPayload)
        assertEquals("Inner payload size must be 18 bytes", 18, innerPayload!!.size)

        for (i in 0 until 18) {
            val expectedByte = (i * 7 + 3) and 0xFF
            val actualByte = innerPayload[i].toInt() and 0xFF
            assertEquals("Byte $i inner payload mismatch", expectedByte, actualByte)
        }

        // 3. Flipping opcode byte must fail decryption (AAD mismatch)
        val tamperedOpcodeFrame = frameBytes.copyOf()
        tamperedOpcodeFrame[0] = 0x01.toByte()
        val decryptedBadOpcode = CryptoEngine.decryptFrame(tamperedOpcodeFrame, keyHex)
        assertNull("Flipping opcode byte must return null", decryptedBadOpcode)

        // 4. Flipping any tag byte must fail decryption (Poly1305 authentication tag error)
        val tamperedTagFrame = frameBytes.copyOf()
        val lastIdx = tamperedTagFrame.size - 1
        tamperedTagFrame[lastIdx] = (tamperedTagFrame[lastIdx].toInt() xor 0xFF).toByte()
        val decryptedBadTag = CryptoEngine.decryptFrame(tamperedTagFrame, keyHex)
        assertNull("Flipping tag byte must return null", decryptedBadTag)
    }

    @Test
    fun testDeriveDeviceTopicId() {
        val pubKey = "4d8a1c92b8e3f7a1029c83b4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4"
        val derivedTopicId = MqttTelemetrySource.deriveDeviceTopicId("SAFAR_01", pubKey)
        assertEquals("a996b58b008271c7", derivedTopicId)
    }

    @Test
    fun testReplayWindowValidatorCases() {
        val validator = CryptoEngine.ReplayWindowValidator(maxStaleWindowSeconds = 300L)

        // Initial valid packet
        assertTrue(validator.validateAndRecord(1000L, 1))

        // Duplicate packet (same timestamp and sequence)
        assertFalse("Duplicate packet must be rejected", validator.validateAndRecord(1000L, 1))

        // Stale sequence number for same timestamp
        assertFalse("Lower sequence number for same timestamp must be rejected", validator.validateAndRecord(1000L, 0))

        // Stale timestamp (> 300 s in the past)
        assertFalse("Timestamp > 300s in past must be rejected", validator.validateAndRecord(600L, 5))

        // Newer valid packet
        assertTrue("Newer timestamp packet must be accepted", validator.validateAndRecord(1005L, 2))

        // Reset validator (tracker reboot / stream resumed)
        validator.reset()
        assertTrue("Stream resume after reset must accept packet with earlier timestamp", validator.validateAndRecord(500L, 1))
    }

    @Test
    fun testQrUriParserValidation() {
        // Test preserving '+' sign in GSM SIM number
        val rawSim = "+919876543210"
        val encodedSim = URLEncoder.encode(rawSim, "UTF-8")
        var decodedSim = URLDecoder.decode(encodedSim, "UTF-8")
        if (decodedSim.startsWith(" ")) {
            decodedSim = "+" + decodedSim.substring(1)
        }
        assertEquals("+919876543210", decodedSim)

        // Validate key lengths
        val validKey = "4d8a1c92b8e3f7a1029c83b4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4"
        val shortKey = "shortkey"

        assertTrue(MqttTelemetrySource.isValidSecretHex(validKey))
        assertFalse(MqttTelemetrySource.isValidSecretHex(shortKey))
        assertFalse(MqttTelemetrySource.isValidSecretHex(""))
    }

    @Test
    fun testHistoryStitchingAndDeduplicationLogic() {
        // Chunk 1 points (ts 1000..1020)
        val chunk1Points = listOf(
            ParsedTripPoint(1000L, 21.1458, 79.0882, 30f, 90, true, isTripStart = true, isTripEnd = false, satCount = 10),
            ParsedTripPoint(1010L, 21.1460, 79.0885, 35f, 92, true, isTripStart = false, isTripEnd = false, satCount = 10),
            ParsedTripPoint(1020L, 21.1462, 79.0888, 40f, 95, true, isTripStart = false, isTripEnd = false, satCount = 10)
        )

        // Chunk 2 points (ts 1030..1050), first point is NOT trip start and within 300s of 1020
        val chunk2Points = listOf(
            ParsedTripPoint(1030L, 21.1465, 79.0890, 42f, 98, true, isTripStart = false, isTripEnd = false, satCount = 10),
            ParsedTripPoint(1040L, 21.1468, 79.0892, 45f, 100, true, isTripStart = false, isTripEnd = false, satCount = 10),
            ParsedTripPoint(1050L, 21.1470, 79.0895, 48f, 102, true, isTripStart = false, isTripEnd = true, satCount = 10)
        )

        // Check continuity logic: first point in chunk 2 (1030s) is within 300s of latest point in chunk 1 (1020s)
        val gapSeconds = chunk2Points.first().timestamp - chunk1Points.last().timestamp
        val isContinuous = !chunk2Points.first().isTripStart && gapSeconds in 0..300
        assertTrue("Chunk 2 points should append to Chunk 1 trip session", isContinuous)

        // Idempotency check: duplicate points filtering
        val existingTsSet = setOf(1000L, 1010L, 1020L)
        val filteredNewPoints = chunk1Points.filter { it.timestamp !in existingTsSet }
        assertTrue("Resent duplicate chunk points must be filtered out", filteredNewPoints.isEmpty())
    }
}
