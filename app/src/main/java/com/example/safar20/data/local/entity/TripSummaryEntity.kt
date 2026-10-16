package com.example.safar20.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_summaries")
data class TripSummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "trip_id")
    val tripId: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long,

    @ColumnInfo(name = "end_timestamp")
    val endTimestamp: Long,

    @ColumnInfo(name = "distance_km")
    val distanceKm: Double,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    @ColumnInfo(name = "avg_speed_kmh")
    val avgSpeedKmh: Double,

    @ColumnInfo(name = "max_speed_kmh")
    val maxSpeedKmh: Double
)
