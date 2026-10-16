package com.example.safar20.data.network

import com.example.safar20.data.local.SafarDatabase
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Realistic Mock ESP32 Telemetry Simulator for SAFAR 2.0.
 * Emits 1 Hz 16-byte binary live telemetry (accelerating from 0 to 85 km/h along Nagpur roads)
 * and seeds 2 realistic historical rides into RoomDB for Phase 2 & Phase 3 UI testing.
 */
object MockEsp32Simulator {

    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _liveTelemetryFlow = MutableSharedFlow<BinaryPacketCodec.ParsedTripPoint>(extraBufferCapacity = 64)
    val liveTelemetryFlow: SharedFlow<BinaryPacketCodec.ParsedTripPoint> = _liveTelemetryFlow.asSharedFlow()

    private val _rawPacketFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val rawPacketFlow: SharedFlow<ByteArray> = _rawPacketFlow.asSharedFlow()

    // Nagpur Center (Zero Mile Monument) starting coordinates
    private const val START_LAT = 21.1458
    private const val START_LON = 79.0882

    private var currentLat = START_LAT
    private var currentLon = START_LON
    private var currentSpeedKmh = 0.0f
    private var headingDegrees = 210 // Heading South-West towards Wardha Road / Airport
    private var sequenceNumber = 1

    fun isSimulating(): Boolean = simulationJob?.isActive == true

    /**
     * Starts emitting 1 Hz live telemetry binary packets.
     */
    fun startSimulation() {
        if (isSimulating()) return

        currentLat = START_LAT
        currentLon = START_LON
        currentSpeedKmh = 0.0f
        headingDegrees = 210

        simulationJob = scope.launch {
            var secondCount = 0

            while (isActive) {
                secondCount++
                val nowSec = System.currentTimeMillis() / 1000

                // Simulate realistic acceleration and cruising speed up to 85 km/h
                if (secondCount <= 18) {
                    // Smooth acceleration from 0 to 82 km/h in 18 seconds
                    currentSpeedKmh += 4.5f + (Math.random() * 0.8f).toFloat()
                } else if (secondCount in 19..40) {
                    // Cruising on highway around 72..85 km/h
                    val variation = (Math.random() * 4.0 - 2.0).toFloat()
                    currentSpeedKmh = (currentSpeedKmh + variation).coerceIn(68.0f, 85.0f)
                } else if (secondCount in 41..48) {
                    // Slowing down for roundabout/turn
                    currentSpeedKmh = (currentSpeedKmh - 6.0f).coerceAtLeast(28.0f)
                    headingDegrees = (headingDegrees + 4) % 360
                } else {
                    // Re-accelerating on open road
                    currentSpeedKmh = (currentSpeedKmh + 3.5f).coerceAtMost(83.5f)
                    if (secondCount > 70) secondCount = 19 // Loop cruising cycle
                }

                // Calculate coordinate shift based on velocity (V / 3.6 meters/sec)
                val metersMoved = (currentSpeedKmh / 3.6f).toDouble()
                val radHeading = Math.toRadians(headingDegrees.toDouble())

                // 1 deg latitude ≈ 111,000 meters
                val deltaLat = (metersMoved / 111_000.0) * cos(radHeading)
                val deltaLon = (metersMoved / (111_000.0 * cos(Math.toRadians(currentLat)))) * sin(radHeading)

                currentLat += deltaLat
                currentLon += deltaLon

                val parsedPoint = BinaryPacketCodec.ParsedTripPoint(
                    timestamp = nowSec,
                    latitude = currentLat,
                    longitude = currentLon,
                    speedKmh = currentSpeedKmh,
                    heading = headingDegrees,
                    isIgnitionOn = true,
                    isTripStart = (secondCount == 1),
                    isTripEnd = false,
                    satCount = 14
                )

                val rawPacket = BinaryPacketCodec.encodeLiveTelemetry(sequenceNumber, parsedPoint)
                sequenceNumber = (sequenceNumber + 1) and 0xFFFF

                _liveTelemetryFlow.emit(parsedPoint)
                _rawPacketFlow.emit(rawPacket)

                delay(1000) // 1 Hz tick
            }
        }
    }

    /**
     * Stops live telemetry emission.
     */
    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Seeds 2 sample historical rides and a default motorcycle into RoomDB if empty.
     */
    suspend fun seedSampleTrips(db: SafarDatabase, deviceId: String = "SAFAR_01") {
        val deviceDao = db.deviceDao()
        val tripDao = db.tripDao()

        // 1. Seed Device if not present
        val existingDevice = deviceDao.getDeviceById(deviceId)
        if (existingDevice == null) {
            deviceDao.insertOrUpdateDevice(
                DeviceEntity(
                    deviceId = deviceId,
                    deviceName = "Royal Enfield Classic 350",
                    nostrPubKey = "4d8a1c92b8e3f7a1029c83b4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4",
                    sharedSecretHex = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90",
                    simNumber = "+919876543210",
                    lastSyncedTimestamp = System.currentTimeMillis() / 1000,
                    lastLat = 21.1458,
                    lastLon = 79.0882,
                    lastSpeed = 42.5f,
                    isSelected = true
                )
            )
        }

        // 2. Check if trips already exist
        val existingTrips = tripDao.getTripsForDevice(deviceId).firstOrNull()
        if (!existingTrips.isNullOrEmpty()) return

        val nowSec = System.currentTimeMillis() / 1000
        val oneDaySec = 86400L

        // --- RIDE 1: Kamptee Highway Morning Ride ---
        val ride1Start = nowSec - oneDaySec - 7200 // Yesterday 06:30 AM approx
        val ride1End = ride1Start + 2700 // 45 mins duration
        val trip1Id = "TRIP_NAGPUR_01"

        val trip1Summary = TripSummaryEntity(
            tripId = trip1Id,
            deviceId = deviceId,
            startTimestamp = ride1Start,
            endTimestamp = ride1End,
            distanceKm = 24.5,
            durationMinutes = 45,
            avgSpeedKmh = 32.6,
            maxSpeedKmh = 82.0
        )

        val trip1Points = mutableListOf<LocationPointEntity>()
        var t1Lat = 21.1500
        var t1Lon = 79.0900
        val pointCount1 = 25

        for (i in 0 until pointCount1) {
            val progress = i.toDouble() / (pointCount1 - 1)
            t1Lat += 0.0028 + (Math.random() * 0.0004)
            t1Lon += 0.0024 + (Math.random() * 0.0003)
            val speed = when {
                i < 3 -> (i + 1) * 15.0f
                i in 4..20 -> (55.0 + Math.sin(i.toDouble()) * 22.0).toFloat().coerceIn(35f, 82f)
                else -> (pointCount1 - i) * 12.0f
            }

            trip1Points.add(
                LocationPointEntity(
                    tripId = trip1Id,
                    deviceId = deviceId,
                    timestamp = ride1Start + (progress * 2700).toLong(),
                    latitude = t1Lat,
                    longitude = t1Lon,
                    speedKmh = speed,
                    heading = 45 + (i * 2) % 30
                )
            )
        }

        // --- RIDE 2: Airport Expressway Sunset Commute ---
        val ride2Start = nowSec - (2 * oneDaySec) - 3600 // 2 days ago evening
        val ride2End = ride2Start + 2100 // 35 mins duration
        val trip2Id = "TRIP_NAGPUR_02"

        val trip2Summary = TripSummaryEntity(
            tripId = trip2Id,
            deviceId = deviceId,
            startTimestamp = ride2Start,
            endTimestamp = ride2End,
            distanceKm = 18.2,
            durationMinutes = 35,
            avgSpeedKmh = 31.2,
            maxSpeedKmh = 76.5
        )

        val trip2Points = mutableListOf<LocationPointEntity>()
        var t2Lat = 21.1400
        var t2Lon = 79.0800
        val pointCount2 = 20

        for (i in 0 until pointCount2) {
            val progress = i.toDouble() / (pointCount2 - 1)
            t2Lat -= 0.0035 + (Math.random() * 0.0003)
            t2Lon -= 0.0015 + (Math.random() * 0.0002)
            val speed = when {
                i < 3 -> (i + 1) * 18.0f
                i in 4..16 -> (50.0 + Math.cos(i.toDouble()) * 20.0).toFloat().coerceIn(30f, 76.5f)
                else -> (pointCount2 - i) * 10.0f
            }

            trip2Points.add(
                LocationPointEntity(
                    tripId = trip2Id,
                    deviceId = deviceId,
                    timestamp = ride2Start + (progress * 2100).toLong(),
                    latitude = t2Lat,
                    longitude = t2Lon,
                    speedKmh = speed,
                    heading = 215 + (i * 3) % 25
                )
            )
        }

        // Insert into Room
        tripDao.insertOrUpdateTrip(trip1Summary)
        tripDao.insertTripPoints(trip1Points)

        tripDao.insertOrUpdateTrip(trip2Summary)
        tripDao.insertTripPoints(trip2Points)
    }
}
