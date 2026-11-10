package com.example.safar20.viewModels

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity
import com.example.safar20.data.network.BinaryPacketCodec
import com.example.safar20.data.network.LinkState
import com.example.safar20.data.network.MockTelemetrySource
import com.example.safar20.data.network.MqttTelemetrySource
import com.example.safar20.data.repository.SafarRepository
import com.example.safar20.ui.components.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared ViewModel for SAFAR 2.0.
 * Single source of truth for Cockpit Dashboard, Logbook, and Settings.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SafarViewModel(
    private val repository: SafarRepository
) : ViewModel() {

    // --- Core StateFlows from Repository ---
    val allDevices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDevice: StateFlow<DeviceEntity?> = repository.selectedDevice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val speedUnit: StateFlow<String> = repository.speedUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km/h")

    val mqttBrokerUri: StateFlow<String> = repository.mqttBrokerUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val mqttUsername: StateFlow<String> = repository.mqttUsername
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val mqttPassword: StateFlow<String> = repository.mqttPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val linkState: StateFlow<LinkState> = repository.linkState
    val historySyncProgress: StateFlow<Int?> = repository.historySyncProgress
    val isDevModeEnabled: StateFlow<Boolean> = repository.isDevModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Foreground and Network State ---
    private val _isForeground = MutableStateFlow(true)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private data class ConnectConfig(
        val device: DeviceEntity?,
        val brokerUri: String,
        val username: String,
        val password: String,
        val isSim: Boolean,
        val isFg: Boolean,
        val isNet: Boolean
    )

    // --- Live Telemetry State ---
    private val _hasLiveData = MutableStateFlow(false)
    val hasLiveData: StateFlow<Boolean> = _hasLiveData.asStateFlow()

    private val _liveTelemetry = MutableStateFlow<BinaryPacketCodec.ParsedTripPoint?>(null)
    val liveTelemetry: StateFlow<BinaryPacketCodec.ParsedTripPoint?> = _liveTelemetry.asStateFlow()

    private val _liveOdoDistanceKm = MutableStateFlow(0.0)
    val liveOdoDistanceKm: StateFlow<Double> = _liveOdoDistanceKm.asStateFlow()

    private val _liveTripDistanceKm = MutableStateFlow(0.0)
    val liveTripDistanceKm: StateFlow<Double> = _liveTripDistanceKm.asStateFlow()

    private val _liveAvgSpeedKmh = MutableStateFlow(0.0)
    val liveAvgSpeedKmh: StateFlow<Double> = _liveAvgSpeedKmh.asStateFlow()

    private val _liveMaxSpeedKmh = MutableStateFlow(0.0)
    val liveMaxSpeedKmh: StateFlow<Double> = _liveMaxSpeedKmh.asStateFlow()

    private var liveSpeedSum: Double = 0.0
    private var liveSpeedPointCount: Int = 0

    private var previousPoint: BinaryPacketCodec.ParsedTripPoint? = null
    private var lastLocationDbWriteTime: Long = 0L

    @Volatile
    private var lastLivePacketElapsedRealtime: Long = 0L

    private val _isSimulatorRunning = MutableStateFlow(false)
    val isSimulatorRunning: StateFlow<Boolean> = _isSimulatorRunning.asStateFlow()

    private fun tickerFlow(periodMs: Long) = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs)
        }
    }

    val connectionState: StateFlow<ConnectionState> = combine(
        combine(selectedDevice, mqttBrokerUri, _isSimulatorRunning) { dev, uri, isSim ->
            Triple(dev, uri, isSim)
        },
        combine(linkState, historySyncProgress, tickerFlow(1000L)) { link, progress, _ ->
            Pair(link, progress)
        }
    ) { (device, brokerUri, isSim), (link, progress) ->
        when {
            device == null -> ConnectionState.NO_DEVICE
            brokerUri.isBlank() -> ConnectionState.BROKER_NOT_CONFIGURED
            isSim -> ConnectionState.LIVE
            progress != null -> ConnectionState.SYNCING(progress)
            link is LinkState.Error -> ConnectionState.BROKER_ERROR(link.message)
            link is LinkState.Connecting -> ConnectionState.CONNECTING
            link is LinkState.Connected -> {
                val elapsedSinceLive = SystemClock.elapsedRealtime() - lastLivePacketElapsedRealtime
                if (lastLivePacketElapsedRealtime > 0L && elapsedSinceLive <= 5000L) {
                    ConnectionState.LIVE
                } else {
                    ConnectionState.WAITING_FOR_TRACKER
                }
            }
            else -> ConnectionState.PARKED(device?.lastSyncedTimestamp)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.NO_DEVICE)

    // --- Trip History State ---
    val tripHistory: StateFlow<List<TripSummaryEntity>> = selectedDevice
        .flatMapLatest { device ->
            if (device != null) {
                repository.getTripsForDevice(device.deviceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    val selectedTripPoints: StateFlow<List<LocationPointEntity>> = _selectedTripId
        .flatMapLatest { tripId ->
            if (tripId != null) {
                repository.getPointsForTrip(tripId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect live telemetry from Repository
        viewModelScope.launch {
            repository.liveTelemetryStream.collect { telemetryPoint ->
                _hasLiveData.value = true
                lastLivePacketElapsedRealtime = SystemClock.elapsedRealtime()

                if (telemetryPoint.isTripStart) {
                    _liveTripDistanceKm.value = 0.0
                    previousPoint = null
                }

                val prev = previousPoint
                if (prev != null && telemetryPoint.speedKmh > 1.0f) {
                    val deltaM = calculateDistanceMeters(
                        prev.latitude, prev.longitude,
                        telemetryPoint.latitude, telemetryPoint.longitude
                    )
                    val deltaKm = deltaM / 1000.0
                    _liveOdoDistanceKm.value += deltaKm
                    _liveTripDistanceKm.value += deltaKm
                }
                if (telemetryPoint.speedKmh > 0f) {
                    liveSpeedPointCount++
                    liveSpeedSum += telemetryPoint.speedKmh
                    _liveAvgSpeedKmh.value = liveSpeedSum / liveSpeedPointCount
                    if (telemetryPoint.speedKmh > _liveMaxSpeedKmh.value) {
                        _liveMaxSpeedKmh.value = telemetryPoint.speedKmh.toDouble()
                    }
                }
                previousPoint = telemetryPoint
                _liveTelemetry.value = telemetryPoint

                val nowSec = System.currentTimeMillis() / 1000
                if (nowSec - lastLocationDbWriteTime >= 10) {
                    lastLocationDbWriteTime = nowSec
                    selectedDevice.value?.let { device ->
                        repository.updateLastKnownLocation(
                            deviceId = device.deviceId,
                            lat = telemetryPoint.latitude,
                            lon = telemetryPoint.longitude,
                            speed = telemetryPoint.speedKmh,
                            timestamp = nowSec
                        )
                    }
                }
            }
        }

        // Centralized debounced connection evaluator
        viewModelScope.launch {
            combine(
                combine(selectedDevice, mqttBrokerUri, mqttUsername, mqttPassword) { dev, uri, user, pass ->
                    listOf(dev, uri, user, pass)
                },
                combine(_isSimulatorRunning, _isForeground, _isNetworkAvailable) { isSim, isFg, isNet ->
                    Triple(isSim, isFg, isNet)
                }
            ) { devConfig, stateConfig ->
                val dev = devConfig[0] as DeviceEntity?
                val uri = devConfig[1] as String
                val user = devConfig[2] as String
                val pass = devConfig[3] as String
                val (isSim, isFg, isNet) = stateConfig
                ConnectConfig(dev, uri, user, pass, isSim, isFg, isNet)
            }
                .debounce(300L)
                .collect { config ->
                    evaluateAndConnect(config)
                }
        }
    }

    fun setAppForeground(isForeground: Boolean) {
        _isForeground.value = isForeground
    }

    fun setNetworkAvailable(isAvailable: Boolean) {
        _isNetworkAvailable.value = isAvailable
    }

    private fun evaluateAndConnect(config: ConnectConfig) {
        if (!config.isFg || !config.isNet || config.isSim || config.device == null || config.brokerUri.isBlank()) {
            disconnect()
            return
        }
        connectIfReady(config.device, config.brokerUri, config.username, config.password)
    }

    fun connectIfReady(
        device: DeviceEntity? = selectedDevice.value,
        brokerUri: String = mqttBrokerUri.value,
        username: String = mqttUsername.value,
        password: String = mqttPassword.value
    ) {
        if (device == null || brokerUri.isBlank() || _isSimulatorRunning.value || !_isForeground.value || !_isNetworkAvailable.value) {
            disconnect()
            return
        }

        _liveTripDistanceKm.value = 0.0
        previousPoint = null

        val currentSource = repository.getTelemetrySource()
        if (currentSource is MqttTelemetrySource &&
            currentSource.deviceId == device.deviceId &&
            currentSource.brokerUri == brokerUri &&
            currentSource.mqttUsername == username &&
            currentSource.mqttPassword == password &&
            currentSource.sharedSecretHex == device.sharedSecretHex
        ) {
            if (!currentSource.isRunning()) {
                currentSource.start()
            }
        } else {
            val mqttSource = MqttTelemetrySource(
                deviceId = device.deviceId,
                nostrPubKey = device.nostrPubKey,
                sharedSecretHex = device.sharedSecretHex,
                brokerUri = brokerUri,
                mqttUsername = username,
                mqttPassword = password,
                lastSyncedProvider = { repository.getLastSyncedTimestamp(device.deviceId) }
            )
            repository.setTelemetrySource(mqttSource)
            mqttSource.start()
        }
    }

    fun disconnect() {
        val currentSource = repository.getTelemetrySource()
        if (currentSource is MqttTelemetrySource) {
            currentSource.stop()
        }
    }

    suspend fun testBrokerConnection(uri: String, username: String, password: String): String? {
        return withContext(Dispatchers.IO) {
            val cleanUri = uri.trim()
            if (!cleanUri.startsWith("ssl://")) {
                return@withContext "Invalid URI: Broker URI must start with ssl://"
            }

            try {
                val clientId = "SafarTest_" + UUID.randomUUID().toString().take(8)
                val testClient = MqttClient(
                    cleanUri,
                    clientId,
                    MemoryPersistence()
                )
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 5
                    keepAliveInterval = 10
                    if (username.isNotBlank()) userName = username
                    if (password.isNotBlank()) this.password = password.toCharArray()
                }
                testClient.connect(options)
                if (testClient.isConnected) {
                    testClient.disconnect()
                    testClient.close()
                    null
                } else {
                    "Connection failed"
                }
            } catch (e: Exception) {
                MqttTelemetrySource.mapExceptionToHumanMessage(e)
            }
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun saveMqttBrokerConfig(uri: String, username: String, password: String) {
        viewModelScope.launch {
            repository.setMqttBrokerConfig(uri, username, password)
        }
    }

    fun sendClearTrackerFlash(mode: Int = 0): Boolean {
        return repository.sendClearFlashCommand(mode)
    }

    // --- UI Actions ---
    fun selectDevice(deviceId: String) {
        viewModelScope.launch {
            repository.setSelectedDevice(deviceId)
            _selectedTripId.value = null
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(isDark)
        }
    }

    fun setSpeedUnit(unit: String) {
        viewModelScope.launch {
            repository.setSpeedUnit(unit)
        }
    }

    fun toggleSpeedUnit() {
        viewModelScope.launch {
            repository.toggleSpeedUnit()
        }
    }

    fun toggleDevMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDevModeEnabled(enabled)
        }
    }

    fun seedDemoRides() {
        selectedDevice.value?.let { device ->
            viewModelScope.launch {
                repository.seedDemoTrips(device.deviceId)
            }
        }
    }

    fun removeDemoRides() {
        selectedDevice.value?.let { device ->
            viewModelScope.launch {
                repository.deleteDemoTrips(device.deviceId)
            }
        }
    }

    fun toggleSimulator(enable: Boolean) {
        _isSimulatorRunning.value = enable
        if (enable) {
            _hasLiveData.value = true
            _liveTripDistanceKm.value = 0.0
            _liveAvgSpeedKmh.value = 0.0
            _liveMaxSpeedKmh.value = 0.0
            liveSpeedSum = 0.0
            liveSpeedPointCount = 0
            previousPoint = null
            repository.setTelemetrySource(MockTelemetrySource())
            repository.startSimulator()
        } else {
            repository.stopSimulator()
        }
    }

    fun selectTripForMapPreview(tripId: String?) {
        _selectedTripId.value = tripId
    }

    fun pairDeviceFromQr(
        qrUriString: String,
        onSuccess: (DeviceEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.parseAndPairQrUri(qrUriString)
            result.onSuccess { device ->
                onSuccess(device)
            }.onFailure { exception ->
                onError(exception.message ?: "Failed to parse QR URI")
            }
        }
    }

    fun addManualDevice(
        id: String,
        name: String,
        simNumber: String,
        pubKey: String,
        secretHex: String,
        onSuccess: (DeviceEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        if (id.isBlank() || name.isBlank()) {
            onError("Device ID and Name are required")
            return
        }
        val cleanPub = pubKey.trim()
        val cleanSec = secretHex.trim()

        if (cleanPub.length != 64 || !cleanPub.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            onError("Public Key must be a valid 64-character hex string")
            return
        }
        if (cleanSec.length != 64 || !cleanSec.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            onError("Shared Secret must be a valid 64-character hex string")
            return
        }

        viewModelScope.launch {
            val device = repository.addManualDevice(id, name, simNumber, cleanPub, cleanSec)
            onSuccess(device)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repository.deleteDeviceAndHistory(deviceId)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = SafarRepository.getInstance(context)
            return SafarViewModel(repo) as T
        }
    }
}
