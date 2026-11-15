package com.nebulousprime26.mileage_tracker.db.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** When the trip began, as epoch millis. */
    val startDate: Long,
    /** When the trip ended, as epoch millis. Placeholder for drafts. */
    val endDate: Long,
    val startPostalCode: String,
    val endPostalCode: String,
    /** Optional vehicle identifier. Empty means "not specified". */
    val licensePlate: String,
    val startMileage: Double,
    /** Odometer at trip end. Placeholder for drafts. */
    val endMileage: Double,
    val isPrivate: Boolean,
    val notes: String,
    /** True when the trip was saved without completing all required fields. */
    val isDraft: Boolean,
) {
    val distance: Double
        get() = (endMileage - startMileage).coerceAtLeast(0.0)

    /** Elapsed time of the trip in milliseconds. */
    val durationMillis: Long
        get() = (endDate - startDate).coerceAtLeast(0L)
}