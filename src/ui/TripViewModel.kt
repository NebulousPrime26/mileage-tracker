package com.nebulousprime26.mileage_tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nebulousprime26.mileage_tracker.data.Trip
import com.nebulousprime26.mileage_tracker.data.TripDao
import com.nebulousprime26.mileage_tracker.data.getDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TripViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: TripDao = getDatabase(app).tripDao()

    /** All trips, newest first. Re-emits whenever the database changes. */
    val trips: StateFlow<List<Trip>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Private-use trips only. */
    val privateTrips: StateFlow<List<Trip>> = dao.getByPrivateUse(true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Highest end mileage across all trips, or null when there are none. */
    val maxEndMileage: StateFlow<Double?> = dao.getMaxEndMileage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** End postal code of the most recent trip, or null when there are none. */
    val lastEndPostalCode: StateFlow<String?> = dao.getLastEndPostalCode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // ── Statistics filters ───────────────────────────────────────────

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

    /** Mileage split into private/business/total, aggregated per calendar year. */
    val yearlyStats: StateFlow<List<YearlyStats>> = combine(
        dao.getAll(),
        _filterStart,
        _filterEnd,
    ) { trips, start, end ->
        trips
            .filter { trip ->
                (start == null || trip.date >= start) &&
                (end == null || trip.date <= end)
            }
            .groupBy { trip ->
                Calendar.getInstance().apply { timeInMillis = trip.date }
                    .get(Calendar.YEAR)
            }
            .map { (year, yearTrips) ->
                YearlyStats(
                    year = year,
                    privateMileage = yearTrips.filter { it.privateUse }
                        .sumOf { it.distanceMileage },
                    businessMileage = yearTrips.filter { !it.privateUse }
                        .sumOf { it.distanceMileage },
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
                (start == null || trip.date >= start) &&
                (end == null || trip.date <= end)
            }
            .groupBy { trip ->
                val cal = Calendar.getInstance().apply { timeInMillis = trip.date }
                cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
            }
            .map { (key, monthTrips) ->
                MonthlyStats(
                    year = key.first,
                    month = key.second,
                    privateMileage = monthTrips.filter { it.privateUse }
                        .sumOf { it.distanceMileage },
                    businessMileage = monthTrips.filter { !it.privateUse }
                        .sumOf { it.distanceMileage },
                )
            }
            .sortedWith(compareBy({ it.year }, { it.month }))
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addTrip(trip: Trip) {
        viewModelScope.launch { dao.insert(trip) }
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch { dao.update(trip) }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}