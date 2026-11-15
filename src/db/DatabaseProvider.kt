package com.nebulousprime26.mileage_tracker.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

import com.nebulousprime26.mileage_tracker.db.cipher.DbKeyManager
import com.nebulousprime26.mileage_tracker.db.local.MileageDatabase
import com.nebulousprime26.mileage_tracker.db.migrations.ALL_MIGRATIONS

object DatabaseProvider {

    private const val DB_NAME = "mileage.db"
    private const val PREFS_NAME = "db_prefs"

    @Volatile
    private var INSTANCE: MileageDatabase? = null

    private val lock = Any()

    fun getInstance(context: Context): MileageDatabase =
        INSTANCE ?: synchronized(lock) {
            INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
        }

    private fun build(context: Context): MileageDatabase {
        System.loadLibrary("sqlcipher")

        val keyManager = DbKeyManager(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
        val passphrase = keyManager.getOrCreateDatabaseKey()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            MileageDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()
    }
}