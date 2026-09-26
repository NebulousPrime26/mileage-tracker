package com.nebulousprime26.mileage_tracker.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val startDate: Long,
    val endDate: Long,
    val startPostalCode: String,
    val endPostalCode: String,
    val licensePlate: String,
    val startMileage: Double,
    val endMileage: Double,
    val privateUse: Boolean,
    val notes: String,
    val isDraft: Boolean,
) {
    val distanceMileage: Double
        get() = endMileage - startMileage

    /** Elapsed time of the trip in milliseconds. */
    val durationMillis: Long
        get() = (endDate - startDate).coerceAtLeast(0L)
}

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE privateUse = :isPrivate ORDER BY startDate DESC")
    fun getByPrivateUse(isPrivate: Boolean): Flow<List<Trip>>

    @Query("SELECT MAX(endMileage) FROM trips WHERE isDraft = 0")
    fun getMaxEndMileage(): Flow<Double?>

    @Query("SELECT endPostalCode FROM trips WHERE isDraft = 0 ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLastEndPostalCode(): Flow<String?>

    /** The start postal code of the most recent completed trip — the likely end of a round trip. */
    @Query("SELECT startPostalCode FROM trips WHERE isDraft = 0 AND startPostalCode != '' ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLastStartPostalCode(): Flow<String?>

    /** The last non-blank plate on a completed trip, so it can be offered as a default. */
    @Query("SELECT licensePlate FROM trips WHERE isDraft = 0 AND licensePlate != '' ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLastLicensePlate(): Flow<String?>

    @Insert
    suspend fun insert(trip: Trip)

    @Update
    suspend fun update(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)
}