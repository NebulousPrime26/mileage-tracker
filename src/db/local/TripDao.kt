package com.nebulousprime26.mileage_tracker.db.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate DESC, id DESC")
    fun getAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE isPrivate = :isPrivate ORDER BY startDate DESC, id DESC")
    fun getByPrivateUse(isPrivate: Boolean): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getById(id: Long): Flow<Trip?>

    @Query("SELECT MAX(endMileage) FROM trips WHERE isDraft = 0")
    fun getMaxEndMileage(): Flow<Double?>

    @Query("SELECT endPostalCode FROM trips WHERE isDraft = 0 ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLastEndPostalCode(): Flow<String?>

    /** The start postal code of the most recent completed trip. */
    @Query("SELECT startPostalCode FROM trips WHERE isDraft = 0 AND startPostalCode != '' ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLastStartPostalCode(): Flow<String?>

    /** The last non-blank plate on a completed trip. */
    @Query("SELECT licensePlate FROM trips WHERE isDraft = 0 AND licensePlate != '' ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLastLicensePlate(): Flow<String?>

    @Insert
    suspend fun insert(trip: Trip): Long

    @Insert
    suspend fun insertAll(trips: List<Trip>)

    @Update
    suspend fun update(trip: Trip): Int

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(trips: List<Trip>) {
        deleteAll()
        insertAll(trips)
    }
}