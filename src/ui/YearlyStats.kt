package com.nebulousprime26.mileage_tracker.ui

data class YearlyStats(
    val year: Int,
    val privateMileage: Double,
    val businessMileage: Double,
) {
    val totalMileage: Double get() = privateMileage + businessMileage
}

data class MonthlyStats(
    val year: Int,
    val month: Int, // 0 = January
    val privateMileage: Double,
    val businessMileage: Double,
) {
    val totalMileage: Double get() = privateMileage + businessMileage
    val label: String get() = "${MONTH_ABBREVS.getOrElse(month) { "?" }} ${year % 100}"
}

private val MONTH_ABBREVS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)