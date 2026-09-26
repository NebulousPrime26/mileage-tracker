package com.nebulousprime26.mileage_tracker.ui

/** Which metric the stats screen is currently displaying. */
enum class StatsMetric { MILEAGE, DURATION }

/** Aggregated mileage and duration for a single calendar year. */
data class YearlyStats(
    val year: Int,
    val privateMileage: Double,
    val businessMileage: Double,
    val privateDurationMillis: Long,
    val businessDurationMillis: Long,
    val privateTripCount: Int,
    val businessTripCount: Int,
) {
    val totalMileage: Double get() = privateMileage + businessMileage
    val totalDurationMillis: Long get() = privateDurationMillis + businessDurationMillis

    val privateAverageDurationMillis: Long
        get() = if (privateTripCount > 0) privateDurationMillis / privateTripCount else 0L
    val businessAverageDurationMillis: Long
        get() = if (businessTripCount > 0) businessDurationMillis / businessTripCount else 0L
    val totalAverageDurationMillis: Long
        get() {
            val totalCount = privateTripCount + businessTripCount
            return if (totalCount > 0) totalDurationMillis / totalCount else 0L
        }
}

/** Aggregated mileage and duration for a single calendar month. */
data class MonthlyStats(
    val year: Int,
    val month: Int, // 0 = January
    val privateMileage: Double,
    val businessMileage: Double,
    val privateDurationMillis: Long,
    val businessDurationMillis: Long,
    val privateTripCount: Int,
    val businessTripCount: Int,
) {
    val totalMileage: Double get() = privateMileage + businessMileage
    val totalDurationMillis: Long get() = privateDurationMillis + businessDurationMillis

    val privateAverageDurationMillis: Long
        get() = if (privateTripCount > 0) privateDurationMillis / privateTripCount else 0L
    val businessAverageDurationMillis: Long
        get() = if (businessTripCount > 0) businessDurationMillis / businessTripCount else 0L
    val totalAverageDurationMillis: Long
        get() {
            val totalCount = privateTripCount + businessTripCount
            return if (totalCount > 0) totalDurationMillis / totalCount else 0L
        }

    val label: String get() = "${MONTH_ABBREVS.getOrElse(month) { "?" }} ${year % 100}"
}

private val MONTH_ABBREVS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)