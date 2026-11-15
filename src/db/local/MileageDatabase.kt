package com.nebulousprime26.mileage_tracker.db.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Trip::class],
    version = 1,
    exportSchema = true
)
abstract class MileageDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}