package com.nebulousprime26.mileage_tracker.db.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [Trip::class],
    version = 1,
    exportSchema = true
)
abstract class MileageDatabase : RoomDatabase() {
    
    abstract fun tripDao(): TripDao
    
    companion object {
        private const val DB_NAME = "mileage.db"
        private const val PREFS_NAME = "db_prefs"

        @Volatile
        private var INSTANCE: MileageDatabase? = null

        private val lock = Any()
        
        fun getInstance(context: Context): MileageDatabase {
            return INSTANCE ?: synchronized(lock) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it}
            }
        }
        
        private fun buildDatabase(context: Context): MileageDatabase {
            System.loadLibrary("sqlcipher")

            val keyManager = SqlCipherKeyManager(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
            val passphrase = keyManager.getOrCreateDatabaseKey()
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                MileageDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                .build()
        }
    }
}