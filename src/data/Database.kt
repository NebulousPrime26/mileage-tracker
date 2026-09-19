package com.nebulousprime26.mileage_tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Trip::class], version = 1)
abstract class MileageDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}

private const val DB_NAME = "mileage.db"
private const val PREFS_NAME = "db_prefs"

@Volatile
private var INSTANCE: MileageDatabase? = null

private val lock = Any()

fun getDatabase(context: Context): MileageDatabase {
    return INSTANCE ?: synchronized(lock) {
        INSTANCE ?: run {
            System.loadLibrary("sqlcipher")

            val keyManager = SqlCipherKeyManager(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
            val passphrase = keyManager.getOrCreateDatabaseKey()

            val factory = SupportOpenHelperFactory(passphrase)

            Room.databaseBuilder(
                context.applicationContext,
                MileageDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .build()
                .also { INSTANCE = it }
        }
    }
}