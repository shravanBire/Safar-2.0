package com.example.safar20.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity
import com.example.safar20.data.network.BinaryPacketCodec
import com.example.safar20.data.repository.SafarRepository
import com.example.safar20.ui.components.ConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared ViewModel for SAFAR 2.0.
 * Serves HomeScreen, TripsScreen, and SettingsScreen with a single source of truth.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

    // --- Live Telemetry State ---
    private val _liveTelemetry = MutableStateFlow<BinaryPacketCodec.ParsedTripPoint?>(null)
    val liveTelemetry: StateFlow<BinaryPacketCodec.ParsedTripPoint?> = _liveTelemetry.asStateFlow()

    private val _liveOdoDistanceKm = MutableStateFlow(18.4)
    val liveOdoDistanceKm: StateFlow<Double> = _liveOdoDistanceKm.asStateFlow()

    private val _liveTripDistanceKm = MutableStateFlow(18.4)
    val liveTripDistanceKm: StateFlow<Double> = _liveTripDistanceKm.asStateFlow()

    private val _liveAvgSpeedKmh = MutableStateFlow(31.0)
    val liveAvgSpeedKmh: StateFlow<Double> = _liveAvgSpeedKmh.asStateFlow()

    private val _liveMaxSpeedKmh = MutableStateFlow(78.0)
    val liveMaxSpeedKmh: StateFlow<Double> = _liveMaxSpeedKmh.asStateFlow()

    private var liveSpeedSum: Double = 31.0
    private var liveSpeedPointCount: Int = 1

    private var previousPoint: BinaryPacketCodec.ParsedTripPoint? = null

    private val _isSimulatorRunning = MutableStateFlow(false)
    val isSimulatorRunning: StateFlow<Boolean> = _isSimulatorRunning.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = combine(
        selectedDevice,
        _isSimulatorRunning,
        _liveTelemetry
    ) { device, isSim, telemetry ->
        when {
            device == null -> ConnectionState.OFFLINE
            isSim -> ConnectionState.SIMULATOR_SYNCING
            telemetry != null && (System.currentTimeMillis() / 1000 - telemetry.timestamp) < 10 -> ConnectionState.P2P_DIRECT
            else -> ConnectionState.PARKED
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.PARKED)

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

                selectedDevice.value?.let { device ->
                    repository.updateLastKnownLocation(
                        deviceId = device.deviceId,
                        lat = telemetryPoint.latitude,
                        lon = telemetryPoint.longitude,
                        speed = telemetryPoint.speedKmh,
                        timestamp = telemetryPoint.timestamp
                    )
                }
            }
        }

        // Auto-seed mock data on initial launch
        viewModelScope.launch {
            repository.seedMockData("SAFAR_01")
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

    fun toggleSimulator(enable: Boolean) {
        _isSimulatorRunning.value = enable
        if (enable) {
            _liveTripDistanceKm.value = 0.0
            _liveAvgSpeedKmh.value = 0.0
            _liveMaxSpeedKmh.value = 0.0
            liveSpeedSum = 0.0
            liveSpeedPointCount = 0
            previousPoint = null
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
        onSuccess: (DeviceEntity) -> Unit
    ) {
        viewModelScope.launch {
            val device = repository.addManualDevice(id, name, simNumber)
            onSuccess(device)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
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
