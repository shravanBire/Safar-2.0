package com.example.safar20.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.safar20.data.local.dao.DeviceDao
import com.example.safar20.data.local.dao.TripDao
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity

@Database(
    entities = [
        DeviceEntity::class,
        TripSummaryEntity::class,
        LocationPointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SafarDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: SafarDatabase? = null

        fun getDatabase(context: Context): SafarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafarDatabase::class.java,
                    "safar_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
