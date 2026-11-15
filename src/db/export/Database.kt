package com.nebulousprime26.mileage_tracker.db

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first

import com.nebulousprime26.mileage_tracker.db.cipher.encryptText
import com.nebulousprime26.mileage_tracker.db.cipher.EncryptedBackup
import com.nebulousprime26.mileage_tracker.db.local.Trip
import com.nebulousprime26.mileage_tracker.db.local.MileageDatabase

private const val JSON_FORMAT_VERSION = 1

/** Serialises all trips and returns them encrypted, ready to write. */
suspend fun exportDb(context: Context): EncryptedBackup {
    val serialisedTrips = serialiseTrips(context)
    
    return encryptText(serialisedTrips)
}

private suspend fun serialiseTrips(context: Context): ByteArray {
    val trips = MileageDatabase.getInstance(context).tripDao().getAll().first()

    val root = JSONObject().apply {
        put("formatVersion", JSON_FORMAT_VERSION)
        put("trips", JSONArray(trips.map { it.toJson() }))
    }

    return root.toString().toByteArray(Charsets.UTF_8)
}

private fun Trip.toJson(): JSONObject = JSONObject().apply {
    put("startDate", startDate)
    put("endDate", endDate)
    put("startPostalCode", startPostalCode)
    put("endPostalCode", endPostalCode)
    put("licensePlate", licensePlate)
    put("startMileage", startMileage)
    put("endMileage", endMileage)
    put("isPrivate", isPrivate)
    put("notes", notes)
    put("isDraft", isDraft)
}