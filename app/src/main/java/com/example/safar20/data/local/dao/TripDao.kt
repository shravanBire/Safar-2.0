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
}
