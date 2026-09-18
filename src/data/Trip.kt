package com.nebulousprime26.mileage_tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val startPostalCode: String = "",
    val endPostalCode: String = "",
    val startMileage: Double,
    val endMileage: Double,
    val privateUse: Boolean = false,
    val notes: String = "",
) {
    val distance: Double
        get() = endMileage - startMileage
}