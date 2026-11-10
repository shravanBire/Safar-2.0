package com.example.safar20.data.network

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ChaCha20-Poly1305 Crypto Engine for SAFAR 2.0 wire protocol encryption/decryption.
 * Encrypted frame layout per Section 5: Opcode (1B) | Nonce (12B) | Ciphertext | Tag (16B)
 * Opcode is authenticated as 1-byte AAD. Inner payload strips 1B Opcode & 2B trailing CRC16 before encrypting.
 */
object CryptoEngine {

    const val NONCE_SIZE = 12
    const val TAG_SIZE = 16
    private val secureRandom = SecureRandom()

    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.trim()
        require(cleanHex.length == 64) { "Shared secret hex must be exactly 64 characters (32 bytes)" }
        val result = ByteArray(32)
        for (i in 0 until 32) {
            val index = i * 2
            result[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    /**
     * Encrypts a plain packet byte array (with opcode at byte[0] and CRC16 at the last 2 bytes).
     * Strips 1B opcode and 2B CRC16, encrypts inner payload with ChaCha20-Poly1305 and AAD = byte[0].
     * Returns encrypted frame: Opcode (1B) | Nonce (12B) | Ciphertext | Tag (16B)
     */
    fun encryptFrame(plainPacket: ByteArray, sharedSecretHex: String): ByteArray? {
        if (plainPacket.size < 4) return null
        return try {
            val opcode = plainPacket[0]
            val innerPayload = BinaryPacketCodec.getInnerPayload(plainPacket) ?: return null

            val keyBytes = hexToBytes(sharedSecretHex)
            val keySpec = SecretKeySpec(keyBytes, "ChaCha20")

            val nonce = ByteArray(NONCE_SIZE)
            secureRandom.nextBytes(nonce)
            val ivSpec = IvParameterSpec(nonce)

            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            cipher.updateAAD(byteArrayOf(opcode))

            val ciphertextAndTag = cipher.doFinal(innerPayload)

            val result = ByteArray(1 + NONCE_SIZE + ciphertextAndTag.size)
            result[0] = opcode
            System.arraycopy(nonce, 0, result, 1, NONCE_SIZE)
            System.arraycopy(ciphertextAndTag, 0, result, 1 + NONCE_SIZE, ciphertextAndTag.size)
            result
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decrypts an encrypted frame: Opcode (1B) | Nonce (12B) | Ciphertext | Tag (16B).
     * Reconstructs full plain packet with Opcode at byte[0] and valid recalculated CRC16 at the end.
     * Returns null if decryption or AAD authentication fails.
     */
    fun decryptFrame(encryptedFrame: ByteArray, sharedSecretHex: String): ByteArray? {
        if (encryptedFrame.size < 1 + NONCE_SIZE + TAG_SIZE) return null
        return try {
            val opcode = encryptedFrame[0]
            val nonce = encryptedFrame.copyOfRange(1, 1 + NONCE_SIZE)
            val ciphertextAndTag = encryptedFrame.copyOfRange(1 + NONCE_SIZE, encryptedFrame.size)

            val keyBytes = hexToBytes(sharedSecretHex)
            val keySpec = SecretKeySpec(keyBytes, "ChaCha20")
            val ivSpec = IvParameterSpec(nonce)

            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            cipher.updateAAD(byteArrayOf(opcode))

            val innerPayload = cipher.doFinal(ciphertextAndTag)

            BinaryPacketCodec.reconstructPlainPacket(opcode, innerPayload)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Replay Protection Window Validator.
     * Rejects duplicate or stale packets by tracking timestamps and sequence numbers.
     */
    class ReplayWindowValidator(private val maxStaleWindowSeconds: Long = 300L) {
        private var lastSeenTimestamp: Long = 0L
        private var lastSeenSequence: Int = -1

        @Synchronized
        fun validateAndRecord(timestamp: Long, sequenceNumber: Int, checkStale: Boolean = true): Boolean {
            if (checkStale && maxStaleWindowSeconds > 0 && lastSeenTimestamp > 0 && timestamp < (lastSeenTimestamp - maxStaleWindowSeconds)) {
                return false
            }
            if (timestamp == lastSeenTimestamp && lastSeenSequence >= 0 && sequenceNumber <= lastSeenSequence) {
                return false
            }
            if (timestamp > lastSeenTimestamp) {
                lastSeenTimestamp = timestamp
                lastSeenSequence = sequenceNumber
            } else if (timestamp == lastSeenTimestamp) {
                lastSeenSequence = maxOf(lastSeenSequence, sequenceNumber)
            }
            return true
        }

        @Synchronized
        fun reset() {
            lastSeenTimestamp = 0L
            lastSeenSequence = -1
        }
    }
}
