package com.nebulousprime26.mileage_tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.db.local.Trip
import com.nebulousprime26.mileage_tracker.db.local.TripDao
import com.nebulousprime26.mileage_tracker.db.DatabaseProvider
import com.nebulousprime26.mileage_tracker.settings.data.SettingsRepository
import com.nebulousprime26.mileage_tracker.settings.model.AppLanguage
import com.nebulousprime26.mileage_tracker.settings.model.Theme

class TripViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: TripDao = DatabaseProvider.getInstance(app).tripDao()
    private val settingsRepo = SettingsRepository(app)

    // ── Trip data ────────────────────────────────────────────────────

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

    val lastStartPostalCode: StateFlow<String?> = dao.getLastStartPostalCode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val lastLicensePlate: StateFlow<String?> = dao.getLastLicensePlate()
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

    val theme: StateFlow<Theme> = settingsRepo.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Theme.SYSTEM,
        )

    fun setTheme(mode: Theme) {
        viewModelScope.launch { settingsRepo.setTheme(mode) }
    }

    val appLanguage: StateFlow<AppLanguage> = settingsRepo.appLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppLanguage.SYSTEM,
        )

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepo.setAppLanguage(language) }
    }

    val postalFirst: StateFlow<Boolean> = settingsRepo.postalFirst
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setPostalFirst(value: Boolean) {
        viewModelScope.launch { settingsRepo.setPostalFirst(value) }
    }

    val draftLeft: StateFlow<Boolean> = settingsRepo.draftLeft
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setDraftLeft(value: Boolean) {
        viewModelScope.launch { settingsRepo.setDraftLeft(value) }
    }

    val roundTripAssumption: StateFlow<Boolean> = settingsRepo.roundTripAssumption
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setRoundTripAssumption(value: Boolean) {
        viewModelScope.launch { settingsRepo.setRoundTripAssumption(value) }
    }

    val autoFillEndTime: StateFlow<Boolean> = settingsRepo.autoFillEndTime
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setAutoFillEndTime(value: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoFillEndTime(value) }
    }

    val allowSpacesInPostal: StateFlow<Boolean> = settingsRepo.allowSpacesInPostal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setAllowSpacesInPostal(value: Boolean) {
        viewModelScope.launch { settingsRepo.setAllowSpacesInPostal(value) }
    }

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