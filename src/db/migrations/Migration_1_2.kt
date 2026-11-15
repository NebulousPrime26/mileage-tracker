package com.nebulousprime26.mileage_tracker.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Renames `trips.privateUse` → `trips.isPrivate`.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips RENAME COLUMN privateUse TO isPrivate")
    }
}