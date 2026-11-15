package com.nebulousprime26.mileage_tracker.ui.features.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.domain.MonthlyStats
import com.nebulousprime26.mileage_tracker.domain.YearlyStats
import com.nebulousprime26.mileage_tracker.db.local.TripDao
import com.nebulousprime26.mileage_tracker.db.DatabaseProvider

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao: TripDao = DatabaseProvider.getInstance(app).tripDao()

    private val _filterStart = MutableStateFlow<Long?>(null)
    private val _filterEnd = MutableStateFlow<Long?>(null)

    val filterStart: StateFlow<Long?> = _filterStart
    val filterEnd: StateFlow<Long?> = _filterEnd

    fun setFilterStart(millis: Long?) { _filterStart.value = millis }
    fun setFilterEnd(millis: Long?) { _filterEnd.value = millis }
    fun clearFilters() {
        _filterStart.value = null
        _filterEnd.value = null
    }

    val yearlyStats: StateFlow<List<YearlyStats>> = combine(
        dao.getAll(),
        _filterStart,
        _filterEnd,
    ) { trips, start, end ->
        trips
            .filter { trip ->
                (start == null || trip.startDate >= start) &&
                (end == null || trip.startDate <= end)
            }
            .groupBy { trip ->
                Calendar.getInstance().apply { timeInMillis = trip.startDate }
                    .get(Calendar.YEAR)
            }
            .map { (year, yearTrips) ->
                val privateTrips = yearTrips.filter { it.isPrivate }
                val businessTrips = yearTrips.filter { !it.isPrivate }
                YearlyStats(
                    year = year,
                    privateMileage = privateTrips.sumOf { it.distance },
                    businessMileage = businessTrips.sumOf { it.distance },
                    privateDurationMillis = privateTrips.sumOf { it.durationMillis },
                    businessDurationMillis = businessTrips.sumOf { it.durationMillis },
                    privateTripCount = privateTrips.size,
                    businessTripCount = businessTrips.size,
                )
            }
            .sortedBy { it.year }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val monthlyStats: StateFlow<List<MonthlyStats>> = combine(
        dao.getAll(),
        _filterStart,
        _filterEnd,
    ) { trips, start, end ->
        trips
            .filter { trip ->
                (start == null || trip.startDate >= start) &&
                (end == null || trip.startDate <= end)
            }
            .groupBy { trip ->
                val cal = Calendar.getInstance().apply { timeInMillis = trip.startDate }
                cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
            }
            .map { (key, monthTrips) ->
                val privateTrips = monthTrips.filter { it.isPrivate }
                val businessTrips = monthTrips.filter { !it.isPrivate }
                MonthlyStats(
                    year = key.first,
                    month = key.second,
                    privateMileage = privateTrips.sumOf { it.distance },
                    businessMileage = businessTrips.sumOf { it.distance },
                    privateDurationMillis = privateTrips.sumOf { it.durationMillis },
                    businessDurationMillis = businessTrips.sumOf { it.durationMillis },
                    privateTripCount = privateTrips.size,
                    businessTripCount = businessTrips.size,
                )
            }
            .sortedWith(compareBy({ it.year }, { it.month }))
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}