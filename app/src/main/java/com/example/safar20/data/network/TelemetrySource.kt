package com.example.safar20.data.network

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface LinkState {
    object Idle : LinkState
    object Connecting : LinkState
    object Connected : LinkState
    data class Error(val message: String) : LinkState
}

/**
 * Abstraction interface for live telemetry stream sources in SAFAR 2.0.
 * Connects via MQTT over TLS to EMQX relay broker.
 */
interface TelemetrySource {
    val liveTelemetryStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint>
    val statusStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint>
    val linkState: StateFlow<LinkState>
    fun start()
    fun stop()
    fun isRunning(): Boolean
}
