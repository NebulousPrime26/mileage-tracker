package com.nebulousprime26.mileage_tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trip::class], version = 1)
abstract class MileageDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}

private const val DB_NAME = "mileage.db"

@Volatile
private var INSTANCE: MileageDatabase? = null

private val lock = Any()

fun getDatabase(context: Context): MileageDatabase {
    return INSTANCE ?: synchronized(lock) {
        INSTANCE ?: Room.databaseBuilder(
            context.applicationContext,
            MileageDatabase::class.java,
            DB_NAME
        ).build().also { INSTANCE = it }
    }
}