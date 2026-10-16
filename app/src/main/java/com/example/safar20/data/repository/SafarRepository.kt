package com.example.safar20.data.repository

import android.content.Context
import android.net.Uri
import com.example.safar20.data.local.PreferencesDataStore
import com.example.safar20.data.local.SafarDatabase
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity
import com.example.safar20.data.network.BinaryPacketCodec
import com.example.safar20.data.network.MockEsp32Simulator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Unified Repository for SAFAR 2.0.
 * Coordinates RoomDB DAOs, PreferencesDataStore, QR Pairing parser,
 * and Mock ESP32 Telemetry Simulator.
 */
class SafarRepository(
    private val database: SafarDatabase,
    private val preferencesDataStore: PreferencesDataStore
) {

    private val deviceDao = database.deviceDao()
    private val tripDao = database.tripDao()

    // --- Device Management Flows ---
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()
    val selectedDevice: Flow<DeviceEntity?> = deviceDao.getSelectedDevice()

    // --- Preferences Flows ---
    val isDarkTheme: Flow<Boolean> = preferencesDataStore.isDarkTheme
    val speedUnit: Flow<String> = preferencesDataStore.speedUnit

    // --- Live Telemetry Stream ---
    val liveTelemetryStream: SharedFlow<BinaryPacketCodec.ParsedTripPoint> = MockEsp32Simulator.liveTelemetryFlow

    // --- Trip History Flows ---
    fun getTripsForDevice(deviceId: String): Flow<List<TripSummaryEntity>> {
        return tripDao.getTripsForDevice(deviceId)
    }

    fun getPointsForTrip(tripId: String): Flow<List<LocationPointEntity>> {
        return tripDao.getPointsForTrip(tripId)
    }

    suspend fun getPointsListForTrip(tripId: String): List<LocationPointEntity> {
        return tripDao.getPointsListForTrip(tripId)
    }

    // --- Actions ---
    suspend fun setSelectedDevice(deviceId: String) {
        deviceDao.setSelectedDevice(deviceId)
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        preferencesDataStore.setDarkTheme(isDark)
    }

    suspend fun setSpeedUnit(unit: String) {
        preferencesDataStore.setSpeedUnit(unit)
    }

    suspend fun toggleSpeedUnit() {
        preferencesDataStore.toggleSpeedUnit()
    }

    // --- Simulator Control ---
    fun startSimulator() {
        MockEsp32Simulator.startSimulation()
    }

    fun stopSimulator() {
        MockEsp32Simulator.stopSimulation()
    }

    fun isSimulatorRunning(): Boolean = MockEsp32Simulator.isSimulating()

    suspend fun seedMockData(deviceId: String = "SAFAR_01") {
        MockEsp32Simulator.seedSampleTrips(database, deviceId)
    }

    // --- QR Pairing & Device Add ---
    /**
     * Parses QR Payload URI:
     * safar://pair?id=SAFAR_01&name=Classic350&pub=<64_HEX_NOSTR>&sec=<64_HEX_SECRET>&sim=+919876543210
     */
    suspend fun parseAndPairQrUri(qrUriString: String): Result<DeviceEntity> {
        return try {
            val uri = Uri.parse(qrUriString)
            if (uri.scheme != "safar" || uri.host != "pair") {
                return Result.failure(IllegalArgumentException("Invalid QR Code Scheme. Expected safar://pair"))
            }

            val deviceId = uri.getQueryParameter("id") ?: uri.getQueryParameter("device_id")
            if (deviceId.isNullOrEmpty()) {
                return Result.failure(IllegalArgumentException("Device ID missing from QR Code"))
            }

            val deviceName = uri.getQueryParameter("name") ?: uri.getQueryParameter("device_name") ?: "RE Classic 350"
            val pubKey = uri.getQueryParameter("pub") ?: uri.getQueryParameter("nostr_pub_key") ?: ""
            val secretHex = uri.getQueryParameter("sec") ?: uri.getQueryParameter("shared_secret") ?: ""
            val simNumber = uri.getQueryParameter("sim") ?: uri.getQueryParameter("sim_number") ?: ""

            val device = DeviceEntity(
                deviceId = deviceId,
                deviceName = deviceName,
                nostrPubKey = pubKey,
                sharedSecretHex = secretHex,
                simNumber = simNumber,
                lastSyncedTimestamp = System.currentTimeMillis() / 1000,
                isSelected = true
            )

            deviceDao.insertAndSelectDevice(device)
            Result.success(device)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addManualDevice(
        deviceId: String,
        deviceName: String,
        simNumber: String,
        pubKey: String = "",
        secretHex: String = ""
    ): DeviceEntity {
        val device = DeviceEntity(
            deviceId = deviceId,
            deviceName = deviceName,
            nostrPubKey = pubKey,
            sharedSecretHex = secretHex,
            simNumber = simNumber,
            lastSyncedTimestamp = System.currentTimeMillis() / 1000,
            isSelected = true
        )
        deviceDao.insertAndSelectDevice(device)
        return device
    }

    suspend fun deleteDevice(deviceId: String) {
        deviceDao.deleteDeviceById(deviceId)
    }

    suspend fun updateLastKnownLocation(
        deviceId: String,
        lat: Double,
        lon: Double,
        speed: Float,
        timestamp: Long
    ) {
        deviceDao.updateLastKnownLocation(deviceId, lat, lon, speed, timestamp)
    }

    companion object {
        @Volatile
        private var INSTANCE: SafarRepository? = null

        fun getInstance(context: Context): SafarRepository {
            return INSTANCE ?: synchronized(this) {
                val db = SafarDatabase.getDatabase(context)
                val prefs = PreferencesDataStore(context)
                val instance = SafarRepository(db, prefs)
                INSTANCE = instance
                instance
            }
        }
    }
}
