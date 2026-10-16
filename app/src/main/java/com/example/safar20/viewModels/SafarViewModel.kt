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
