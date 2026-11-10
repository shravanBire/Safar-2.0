package com.example.safar20.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.net.NoRouteToHostException
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.UUID
import javax.net.ssl.SSLException

/**
 * MQTT TelemetrySource implementation for SAFAR 2.0.
 * Connects to EMQX / MQTT broker over TLS on Dispatchers.IO, subscribes to device telemetry topics,
 * decrypts ChaCha20-Poly1305 frames, decodes wire protocol binary packets,
 * manages OpenSession, 20s watch-heartbeats, and transactional history chunk ACK ingestion.
 */
class MqttTelemetrySource(
    val deviceId: String = "SAFAR_01",
    val nostrPubKey: String = "",
    val sharedSecretHex: String = "",
    val brokerUri: String = "ssl://broker.hivemq.com:8883",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val lastSyncedProvider: (suspend () -> Long)? = null,
    var onHistoryChunkReceived: (suspend (BinaryPacketCodec.HistoryChunkPacket) -> Boolean)? = null
) : TelemetrySource {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val liveReplayValidator = CryptoEngine.ReplayWindowValidator(maxStaleWindowSeconds = 300L)
    private val statusReplayValidator = CryptoEngine.ReplayWindowValidator(maxStaleWindowSeconds = 300L)

    private val _liveTelemetryStream = MutableSharedFlow<BinaryPacketCodec.ParsedTripPoint>(extraBufferCapacity = 64)
    override val liveTelemetryStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint> = _liveTelemetryStream.asSharedFlow()

    private val _statusStream = MutableSharedFlow<BinaryPacketCodec.ParsedTripPoint>(extraBufferCapacity = 64)
    override val statusStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint> = _statusStream.asSharedFlow()

    private val _linkState = MutableStateFlow<LinkState>(LinkState.Idle)
    override val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    private var mqttClient: MqttClient? = null
    @Volatile
    private var isConnected = false
    private var heartbeatJob: Job? = null
    private var sequenceNumber = 1
    private var lastPacketAcceptedTime = 0L

    @Volatile
    var undecryptableFrameCount = 0
        private set

    val topicDevId: String = deriveDeviceTopicId(deviceId, nostrPubKey)
    val statusTopic = "safar/v1/$topicDevId/status"
    val liveTopic = "safar/v1/$topicDevId/live"
    val histTopic = "safar/v1/$topicDevId/hist"
    val cmdTopic = "safar/v1/$topicDevId/cmd"

    override fun isRunning(): Boolean = isConnected

    override fun start() {
        if (isConnected || deviceId.isBlank()) return

        if (!isValidSecretHex(sharedSecretHex)) {
            _linkState.value = LinkState.Error("Tracker has no valid encryption key. Re-pair using QR")
            return
        }

        _linkState.value = LinkState.Connecting

        scope.launch {
            try {
                val clientId = "SafarApp_" + UUID.randomUUID().toString().take(8)
                val client = MqttClient(brokerUri, clientId, MemoryPersistence())
                mqttClient = client

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        isConnected = true
                        _linkState.value = LinkState.Connected
                        liveReplayValidator.reset()
                        statusReplayValidator.reset()
                        lastPacketAcceptedTime = System.currentTimeMillis()

                        scope.launch {
                            try {
                                client.subscribe(arrayOf(statusTopic, liveTopic, histTopic), intArrayOf(1, 1, 1))

                                val freshLastSynced = lastSyncedProvider?.invoke() ?: 0L
                                val openSessionPacket = BinaryPacketCodec.encodeOpenSession(
                                    lastSyncedTimestamp = freshLastSynced,
                                    sequenceNumber = sequenceNumber++
                                )
                                publishCommand(openSessionPacket)
                                startHeartbeatLoop()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        isConnected = false
                        stopHeartbeat()
                        _linkState.value = LinkState.Connecting
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        scope.launch {
                            processIncomingMessage(topic, message)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = true
                    connectionTimeout = 15
                    keepAliveInterval = 30
                    if (mqttUsername.isNotBlank()) userName = mqttUsername
                    if (mqttPassword.isNotBlank()) password = mqttPassword.toCharArray()
                }

                client.connect(options)
            } catch (e: Exception) {
                isConnected = false
                val errorMsg = mapExceptionToHumanMessage(e)
                _linkState.value = LinkState.Error(errorMsg)
            }
        }
    }

    override fun stop() {
        stopHeartbeat()
        _linkState.value = LinkState.Idle
        scope.launch {
            try {
                mqttClient?.let { client ->
                    if (client.isConnected) {
                        client.unsubscribe(arrayOf(statusTopic, liveTopic, histTopic))
                        client.disconnect()
                    }
                    client.close()
                }
            } catch (_: Exception) {
            } finally {
                mqttClient = null
                isConnected = false
            }
        }
    }

    fun sendClearFlashCommand(mode: Int = 0, upToTimestamp: Long = System.currentTimeMillis() / 1000): Boolean {
        val packet = BinaryPacketCodec.encodeClearFlash(
            upToTimestamp = upToTimestamp,
            mode = mode,
            sequenceNumber = sequenceNumber++
        )
        return publishCommand(packet)
    }

    fun publishCommand(commandPacket: ByteArray): Boolean {
        val client = mqttClient ?: return false
        if (!client.isConnected) return false
        if (!isValidSecretHex(sharedSecretHex)) return false

        return try {
            val payloadToSend = CryptoEngine.encryptFrame(commandPacket, sharedSecretHex) ?: return false
            val message = MqttMessage(payloadToSend).apply { qos = 1 }
            client.publish(cmdTopic, message)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startHeartbeatLoop() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(20_000)
                val heartbeatPacket = BinaryPacketCodec.encodeHeartbeat(
                    timestamp = System.currentTimeMillis() / 1000,
                    sequenceNumber = sequenceNumber++
                )
                publishCommand(heartbeatPacket)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun processIncomingMessage(topic: String, message: MqttMessage) {
        val rawBytes = message.payload ?: return
        if (rawBytes.isEmpty()) return

        if (!isValidSecretHex(sharedSecretHex)) {
            undecryptableFrameCount++
            return
        }

        val plainData = CryptoEngine.decryptFrame(rawBytes, sharedSecretHex)
        if (plainData == null || plainData.isEmpty()) {
            undecryptableFrameCount++
            return
        }

        val now = System.currentTimeMillis()
        if (lastPacketAcceptedTime > 0 && (now - lastPacketAcceptedTime) > 60_000) {
            liveReplayValidator.reset()
            statusReplayValidator.reset()
        }

        when (topic) {
            statusTopic -> {
                val livePacket = BinaryPacketCodec.decodeLiveTelemetry(plainData)
                if (livePacket != null && livePacket.isValidCrc) {
                    if (statusReplayValidator.validateAndRecord(livePacket.tripPoint.timestamp, livePacket.sequenceNumber, checkStale = false)) {
                        lastPacketAcceptedTime = now
                        _statusStream.emit(livePacket.tripPoint)
                    }
                }
            }
            liveTopic -> {
                val livePacket = BinaryPacketCodec.decodeLiveTelemetry(plainData)
                if (livePacket != null && livePacket.isValidCrc) {
                    if (liveReplayValidator.validateAndRecord(livePacket.tripPoint.timestamp, livePacket.sequenceNumber, checkStale = true)) {
                        lastPacketAcceptedTime = now
                        _liveTelemetryStream.emit(livePacket.tripPoint)
                    }
                }
            }
            histTopic -> {
                val chunkPacket = BinaryPacketCodec.decodeHistoryChunk(plainData)
                if (chunkPacket != null && chunkPacket.isValidCrc) {
                    lastPacketAcceptedTime = now
                    val isIngested = onHistoryChunkReceived?.invoke(chunkPacket) ?: true
                    val ackStatus = if (isIngested) 0 else 1
                    val ackPacket = BinaryPacketCodec.encodeHistoryAck(
                        chunkId = chunkPacket.chunkId,
                        status = ackStatus
                    )
                    publishCommand(ackPacket)
                }
            }
        }
    }

    companion object {
        fun isValidSecretHex(hex: String): Boolean {
            val clean = hex.trim()
            return clean.length == 64 && clean.matches(Regex("^[0-9a-fA-F]{64}$"))
        }

        fun deriveDeviceTopicId(deviceId: String, nostrPubKey: String): String {
            val cleanPub = nostrPubKey.trim()
            if (cleanPub.length == 64) {
                return try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hash = digest.digest(cleanPub.toByteArray(Charsets.UTF_8))
                    val hex = hash.joinToString("") { "%02x".format(it) }
                    hex.take(16).lowercase()
                } catch (_: Exception) {
                    deviceId.lowercase().replace(" ", "_")
                }
            }
            return deviceId.lowercase().replace(" ", "_")
        }

        fun mapExceptionToHumanMessage(e: Exception): String {
            return when {
                e is MqttException && (e.reasonCode == 4 || e.reasonCode == 5) -> "Bad credentials or unauthorized"
                e is UnknownHostException || e is NoRouteToHostException -> "Unknown host or no internet connection"
                e is MqttException && e.reasonCode == MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt() -> "Connection timed out"
                e is SSLException || e.cause is SSLException -> "SSL/TLS certificate error"
                e is IllegalArgumentException || e is URISyntaxException -> "Invalid MQTT broker URI"
                else -> e.message ?: "Connection error"
            }
        }
    }
}
