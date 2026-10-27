package com.nebulousprime26.mileage_tracker.db

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.db.local.Trip

private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
    .withZone(ZoneId.systemDefault())
private fun Double.csvNumber(): String = String.format(Locale.ROOT, "%.1f", this)

/** Builds and shares a CSV of all trips. */
fun shareCsv(context: Context, trips: List<Trip>): Uri {
    val file = File(context.cacheDir, "shared").apply { mkdirs() }
        .resolve("mileage-trips-${LocalDate.now()}.csv")
        .apply { writeText(buildCsv(context, trips), Charsets.UTF_8) }

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Builds the CSV body for [trips], in chronological order. */
private fun buildCsv(context: Context, trips: List<Trip>): String {
    val sb = StringBuilder()

    // Header row
    sb.append(
        listOf(
            R.string.csv_header_ride,
            R.string.csv_header_start,
            R.string.csv_header_end,
            R.string.csv_header_start_postal,
            R.string.csv_header_end_postal,
            R.string.csv_header_license_plate,
            R.string.csv_header_start_mileage,
            R.string.csv_header_end_mileage,
            R.string.csv_header_distance_km,
            R.string.csv_header_private,
            R.string.csv_header_notes,
        ).joinToString(",") { context.getString(it) }
    ).append('\n')

    val yes = context.getString(R.string.csv_yes)
    val no = context.getString(R.string.csv_no)

    trips
        .sortedBy { it.startDate }
        .forEachIndexed { index, trip ->
            val row = listOf(
                (index + 1).toString(),
                dateTimeFormatter.format(Instant.ofEpochMilli(trip.startDate)),
                dateTimeFormatter.format(Instant.ofEpochMilli(trip.endDate)),
                trip.startPostalCode,
                trip.endPostalCode,
                trip.licensePlate,
                trip.startMileage.csvNumber(),
                trip.endMileage.csvNumber(),
                trip.distanceMileage.csvNumber(),
                if (trip.privateUse) yes else no,
                trip.notes,
            )
            sb.append(row.joinToString(",") { escape(it) }).append('\n')
        }

    return sb.toString()
}

/**
 * Quotes a field if it contains characters that would otherwise break
 * the CSV structure. Double quotes inside the value are doubled, per
 * RFC 4180.
 */
private fun escape(value: String): String {
    val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
}