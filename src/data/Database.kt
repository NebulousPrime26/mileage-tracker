package com.nebulousprime26.mileage_tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trip::class], version = 1)
abstract class MileageDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var instance: MileageDatabase? = null
        fun get(context: Context): MileageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MileageDatabase::class.java,
                    "mileage.db"
                ).build().also { instance = it }
            }
    }
}