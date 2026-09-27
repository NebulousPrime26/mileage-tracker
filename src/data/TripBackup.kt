package com.nebulousprime26.mileage_tracker.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serializes trips to JSON and reads/writes the encrypted backup file
 * to the device's Downloads folder.
 */
object TripBackup {

    private const val JSON_FORMAT_VERSION = 1
    private const val MIME = "application/octet-stream"
    private const val EXTENSION = "mlgbak"

    // ── Serialization ────────────────────────────────────────────────

    fun serializeTrips(trips: List<Trip>): ByteArray {
        val root = JSONObject().apply {
            put("formatVersion", JSON_FORMAT_VERSION)
            put("trips", JSONArray().apply {
                trips.forEach { put(it.toJson()) }
            })
        }
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun deserializeTrips(json: ByteArray): List<Trip> {
        val root = JSONObject(String(json, Charsets.UTF_8))
        val version = root.optInt("formatVersion", 1)
        require(version <= JSON_FORMAT_VERSION) {
            "Backup was made with a newer version of the app (format $version)."
        }
        val arr = root.getJSONArray("trips")
        return (0 until arr.length()).map { arr.getJSONObject(it).toTrip() }
    }

    private fun Trip.toJson(): JSONObject = JSONObject().apply {
        put("startDate", startDate)
        put("endDate", endDate)
        put("startPostalCode", startPostalCode)
        put("endPostalCode", endPostalCode)
        put("licensePlate", licensePlate)
        put("startMileage", startMileage)
        put("endMileage", endMileage)
        put("privateUse", privateUse)
        put("notes", notes)
        put("isDraft", isDraft)
    }

    private fun JSONObject.toTrip(): Trip = Trip(
        id = 0L, // always a new row on import
        startDate = getLong("startDate"),
        endDate = getLong("endDate"),
        startPostalCode = getString("startPostalCode"),
        endPostalCode = getString("endPostalCode"),
        licensePlate = optString("licensePlate", ""),
        startMileage = getDouble("startMileage"),
        endMileage = getDouble("endMileage"),
        privateUse = getBoolean("privateUse"),
        notes = optString("notes", ""),
        isDraft = optBoolean("isDraft", false),
    )

    // ── Downloads ────────────────────────────────────────────────────

    /** Writes [bytes] to the Downloads folder and returns the resulting URI. */
    fun writeToDownloads(context: Context, filename: String, bytes: ByteArray): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, filename, bytes)
        } else {
            writeLegacy(filename, bytes)
        }
    }

    private fun writeViaMediaStore(
        context: Context,
        filename: String,
        bytes: ByteArray,
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, MIME)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create file in Downloads")

        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Could not open Downloads file for writing")
        } finally {
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun writeLegacy(filename: String, bytes: ByteArray): Uri {
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS,
        )
        downloads.mkdirs()
        val file = File(downloads, filename)
        file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    fun defaultFilename(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
            .format(Date())
        return "mileage-backup-$stamp.$EXTENSION"
    }
}