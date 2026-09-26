package com.nebulousprime26.mileage_tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Trip::class], version = 4)
abstract class MileageDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}

private const val DB_NAME = "mileage.db"
private const val PREFS_NAME = "db_prefs"

@Volatile
private var INSTANCE: MileageDatabase? = null

private val lock = Any()

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips RENAME COLUMN date TO startDate")
        db.execSQL("ALTER TABLE trips ADD COLUMN endDate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE trips SET endDate = startDate")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN licensePlate TEXT NOT NULL DEFAULT ''")
    }
}

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { INSTANCE = it }
        }
    }
}