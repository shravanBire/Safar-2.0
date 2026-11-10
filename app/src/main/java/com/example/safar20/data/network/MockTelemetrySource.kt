package com.example.safar20.data.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock implementation of TelemetrySource wrapping the MockEsp32Simulator.
 */
class MockTelemetrySource : TelemetrySource {

    private val _statusStream = MutableSharedFlow<BinaryPacketCodec.ParsedTripPoint>(extraBufferCapacity = 64)
    override val statusStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint> = _statusStream.asSharedFlow()

    private val _linkState = MutableStateFlow<LinkState>(LinkState.Idle)
    override val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    override val liveTelemetryStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint> =
        MockEsp32Simulator.liveTelemetryFlow

    override fun start() {
        MockEsp32Simulator.startSimulation()
        _linkState.value = LinkState.Connected
    }

    override fun stop() {
        MockEsp32Simulator.stopSimulation()
        _linkState.value = LinkState.Idle
    }

    override fun isRunning(): Boolean {
        return MockEsp32Simulator.isSimulating()
    }
}
