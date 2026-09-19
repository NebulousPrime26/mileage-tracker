package com.nebulousprime26.mileage_tracker.ui

data class YearlyStats(
    val year: Int,
    val privateMileage: Double,
    val businessMileage: Double,
) {
    val totalMileage: Double get() = privateMileage + businessMileage
}