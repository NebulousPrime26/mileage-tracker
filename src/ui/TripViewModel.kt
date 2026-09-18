package com.nebulousprime26.mileage_tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.nebulousprime26.mileage_tracker.data.getDatabase
import com.nebulousprime26.mileage_tracker.data.Trip
import com.nebulousprime26.mileage_tracker.data.TripDao

class TripViewModel(app: Application) : AndroidViewModel(app) {
    private val dao: TripDao = getDatabase(app).tripDao()

    val trips: StateFlow<List<Trip>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val privateTrips: StateFlow<List<Trip>> = dao.getByPrivateUse(true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTrip(trip: Trip) {
        viewModelScope.launch { dao.insert(trip) }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}