package com.example.safar20.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "device_name")
    val deviceName: String,

    @ColumnInfo(name = "nostr_pub_key")
    val nostrPubKey: String,

    @ColumnInfo(name = "shared_secret_hex")
    val sharedSecretHex: String,

    @ColumnInfo(name = "sim_number")
    val simNumber: String,

    @ColumnInfo(name = "last_synced_timestamp")
    val lastSyncedTimestamp: Long = 0L,

    @ColumnInfo(name = "last_lat")
    val lastLat: Double? = null,

    @ColumnInfo(name = "last_lon")
    val lastLon: Double? = null,

    @ColumnInfo(name = "last_speed")
    val lastSpeed: Float? = null,

    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean = false
)
