package com.nebulousprime26.mileage_tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nebulousprime26.mileage_tracker.data.SettingsRepository
import com.nebulousprime26.mileage_tracker.data.ThemeMode
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
    private val settingsRepo = SettingsRepository(app)

    val trips: StateFlow<List<Trip>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val privateTrips: StateFlow<List<Trip>> = dao.getByPrivateUse(true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val maxEndMileage: StateFlow<Double?> = dao.getMaxEndMileage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val lastEndPostalCode: StateFlow<String?> = dao.getLastEndPostalCode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // ── User settings ────────────────────────────────────────────────

    val fabOnRight: StateFlow<Boolean> = settingsRepo.fabOnRight
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setFabOnRight(onRight: Boolean) {
        viewModelScope.launch { settingsRepo.setFabOnRight(onRight) }
    }

    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

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
                (start == null || trip.startDate >= start) &&
                (end == null || trip.startDate <= end)
            }
            .groupBy { trip ->
                val cal = Calendar.getInstance().apply { timeInMillis = trip.startDate }
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

    // ── Mutations ────────────────────────────────────────────────────

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