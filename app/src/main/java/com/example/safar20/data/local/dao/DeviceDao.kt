package com.example.safar20.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.safar20.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY device_name ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE is_selected = 1 LIMIT 1")
    fun getSelectedDevice(): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE is_selected = 1 LIMIT 1")
    suspend fun getSelectedDeviceOnce(): DeviceEntity?

    @Query("SELECT * FROM devices WHERE device_id = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Query("UPDATE devices SET is_selected = CASE WHEN device_id = :deviceId THEN 1 ELSE 0 END")
    suspend fun setSelectedDevice(deviceId: String)

    @Transaction
    suspend fun insertAndSelectDevice(device: DeviceEntity) {
        insertOrUpdateDevice(device.copy(isSelected = true))
        setSelectedDevice(device.deviceId)
    }

    @Query("""
        UPDATE devices 
        SET last_lat = :lat, 
            last_lon = :lon, 
            last_speed = :speed, 
            last_synced_timestamp = :timestamp 
        WHERE device_id = :deviceId
    """)
    suspend fun updateLastKnownLocation(
        deviceId: String,
        lat: Double,
        lon: Double,
        speed: Float,
        timestamp: Long
    )

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE device_id = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)
}
