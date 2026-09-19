package com.nebulousprime26.mileage_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nebulousprime26.mileage_tracker.data.Trip
import com.nebulousprime26.mileage_tracker.ui.LandingScreen
import com.nebulousprime26.mileage_tracker.ui.TripEntryScreen
import com.nebulousprime26.mileage_tracker.ui.TripViewModel
import com.nebulousprime26.mileage_tracker.ui.TripsScreen

private enum class Screen { Landing, Trips, Entry }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: TripViewModel = viewModel()
                var screen by remember { mutableStateOf(Screen.Landing) }
                var editingTrip by remember { mutableStateOf<Trip?>(null) }

                when (screen) {
                    Screen.Landing -> LandingScreen(
                        onContinue = { screen = Screen.Trips },
                        onImport = { /* TODO */ },
                        onExport = { /* TODO */ },
                    )

                    Screen.Trips -> TripsScreen(
                        viewModel = vm,
                        onAddTrip = {
                            editingTrip = null
                            screen = Screen.Entry
                        },
                        onEditTrip = { trip ->
                            editingTrip = trip
                            screen = Screen.Entry
                        },
                        onBack = { screen = Screen.Landing },
                    )

                    Screen.Entry -> TripEntryScreen(
                        existingTrip = editingTrip,
                        onSave = { trip ->
                            if (editingTrip == null) {
                                vm.addTrip(trip)
                            } else {
                                vm.updateTrip(trip)
                            }
                            editingTrip = null
                            screen = Screen.Trips
                        },
                        onCancel = {
                            editingTrip = null
                            screen = Screen.Trips
                        },
                    )
                }
            }
        }
    }
}