package com.example.safar20.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trip_summaries WHERE device_id = :deviceId ORDER BY start_timestamp DESC")
    fun getTripsForDevice(deviceId: String): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE trip_id = :tripId")
    suspend fun getTripById(tripId: String): TripSummaryEntity?

    @Query("SELECT * FROM trip_summaries WHERE device_id = :deviceId ORDER BY start_timestamp DESC LIMIT 1")
    suspend fun getLatestTripSummaryForDevice(deviceId: String): TripSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTrip(trip: TripSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(point: LocationPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripPoints(points: List<LocationPointEntity>)

    @Query("SELECT * FROM location_points WHERE trip_id = :tripId ORDER BY timestamp ASC")
    fun getPointsForTrip(tripId: String): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE trip_id = :tripId ORDER BY timestamp ASC")
    suspend fun getPointsListForTrip(tripId: String): List<LocationPointEntity>

    @Query("SELECT * FROM location_points WHERE device_id = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPointForDevice(deviceId: String): LocationPointEntity?

    @Query("SELECT timestamp FROM location_points WHERE device_id = :deviceId AND timestamp >= :minTs AND timestamp <= :maxTs")
    suspend fun getExistingTimestampsInRange(deviceId: String, minTs: Long, maxTs: Long): List<Long>

    @Query("SELECT MAX(timestamp) FROM location_points WHERE device_id = :deviceId")
    suspend fun getLastSyncedTimestamp(deviceId: String): Long?

    @Query("DELETE FROM trip_summaries WHERE trip_id = :tripId")
    suspend fun deleteTripSummary(tripId: String)

    @Query("DELETE FROM location_points WHERE trip_id = :tripId")
    suspend fun deletePointsForTrip(tripId: String)

    @Transaction
    suspend fun deleteTripAndPoints(tripId: String) {
        deletePointsForTrip(tripId)
        deleteTripSummary(tripId)
    }

    @Query("DELETE FROM trip_summaries WHERE device_id = :deviceId AND trip_id LIKE 'DEMO_%'")
    suspend fun deleteDemoTripSummaries(deviceId: String)

    @Query("DELETE FROM location_points WHERE device_id = :deviceId AND trip_id LIKE 'DEMO_%'")
    suspend fun deleteDemoLocationPoints(deviceId: String)

    @Transaction
    suspend fun deleteDemoTrips(deviceId: String) {
        deleteDemoLocationPoints(deviceId)
        deleteDemoTripSummaries(deviceId)
    }

    @Query("DELETE FROM trip_summaries WHERE device_id = :deviceId")
    suspend fun deleteAllTripSummariesForDevice(deviceId: String)

    @Query("DELETE FROM location_points WHERE device_id = :deviceId")
    suspend fun deleteAllLocationPointsForDevice(deviceId: String)

    @Transaction
    suspend fun deleteAllTripsForDevice(deviceId: String) {
        deleteAllLocationPointsForDevice(deviceId)
        deleteAllTripSummariesForDevice(deviceId)
    }

    @Transaction
    suspend fun saveHistoryChunkTransactional(
        deviceId: String,
        tripSummaries: List<TripSummaryEntity>,
        locationPoints: List<LocationPointEntity>,
        newLastSyncedTimestamp: Long,
        deviceDao: DeviceDao
    ) {
        for (trip in tripSummaries) {
            insertOrUpdateTrip(trip)
        }
        insertTripPoints(locationPoints)
        if (newLastSyncedTimestamp > 0) {
            deviceDao.updateLastSyncedTimestamp(deviceId, newLastSyncedTimestamp)
        }
    }
}
