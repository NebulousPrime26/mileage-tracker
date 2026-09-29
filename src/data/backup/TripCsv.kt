package com.nebulousprime26.mileage_tracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.data.local.Trip

/**
 * Builds and shares a CSV of all trips in chronological order. The
 * first column is a 1-indexed ride number, so the file reads as a
 * numbered log rather than a raw data dump.
 */
object TripCsv {

    private const val MIME = "text/csv"

    // ISO-style, locale-independent: unambiguous when opened in any tool,
    // and sortable as plain text.
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
    private val filenameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    /**
     * Serializes [trips] to CSV. Rows are sorted by start time ascending,
     * and each row is prefixed with its position in that ordering.
     *
     * Takes a [Context] so the column headers and the Yes/No values for
     * the Private column are resolved against the app's active locale.
     */
    fun build(context: Context, trips: List<Trip>): String {
        val sb = StringBuilder()

        // Column headers
        val headers = listOf(
            context.getString(R.string.csv_header_ride),
            context.getString(R.string.csv_header_start),
            context.getString(R.string.csv_header_end),
            context.getString(R.string.csv_header_start_postal),
            context.getString(R.string.csv_header_end_postal),
            context.getString(R.string.csv_header_license_plate),
            context.getString(R.string.csv_header_start_mileage),
            context.getString(R.string.csv_header_end_mileage),
            context.getString(R.string.csv_header_distance_km),
            context.getString(R.string.csv_header_private),
            context.getString(R.string.csv_header_notes),
        )
        sb.append(headers.joinToString(","))
        sb.append('\n')

        val yes = context.getString(R.string.csv_yes)
        val no = context.getString(R.string.csv_no)

        trips
            .sortedBy { it.startDate }
            .forEachIndexed { index, trip ->
                sb.append(index + 1).append(',')
                sb.append(escape(dateTimeFormat.format(Date(trip.startDate)))).append(',')
                sb.append(escape(dateTimeFormat.format(Date(trip.endDate)))).append(',')
                sb.append(escape(trip.startPostalCode)).append(',')
                sb.append(escape(trip.endPostalCode)).append(',')
                sb.append(escape(trip.licensePlate)).append(',')
                sb.append(formatNumber(trip.startMileage)).append(',')
                sb.append(formatNumber(trip.endMileage)).append(',')
                sb.append(formatNumber(trip.distanceMileage)).append(',')
                sb.append(if (trip.privateUse) escape(yes) else escape(no)).append(',')
                sb.append(escape(trip.notes))
                sb.append('\n')
            }

        return sb.toString()
    }

    /**
     * Writes [content] to a cache file and returns a content:// URI
     * suitable for an `ACTION_SEND` intent.
     */
    fun writeToCache(context: Context, filename: String, content: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, filename)
        file.writeText(content, Charsets.UTF_8)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun defaultFilename(): String =
        "mileage-trips-${filenameFormat.format(Date())}.csv"

    val mimeType: String get() = MIME

    /**
     * Quotes a field if it contains characters that would otherwise
     * break the CSV structure. Double quotes inside the value are
     * doubled, per RFC 4180.
     */
    private fun escape(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    /**
     * Formats a double with one decimal place, locale-independent.
     * Uses Locale.ROOT so a device set to a comma-decimal locale
     * doesn't produce "42,5" and break the CSV parsing.
     */
    private fun formatNumber(value: Double): String =
        String.format(Locale.ROOT, "%.1f", value)
}