package com.nebulousprime26.mileage_tracker.data

import androidx.room.Entity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
    val distanceMileage: Double
        get() = endMileage - startMileage
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY date DESC")
    fun getAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE privateUse = :isPrivate ORDER BY date DESC")
    fun getByPrivateUse(isPrivate: Boolean): Flow<List<Trip>>

    @Query("SELECT MAX(endMileage) FROM trips")
    fun getMaxEndMileage(): Flow<Double?>
    
    @Insert
    suspend fun insert(trip: Trip)

    @Update
    suspend fun update(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)
}