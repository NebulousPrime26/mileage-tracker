package com.nebulousprime26.mileage_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nebulousprime26.mileage_tracker.data.Trip
import com.nebulousprime26.mileage_tracker.ui.LandingScreen
import com.nebulousprime26.mileage_tracker.ui.MileageTheme
import com.nebulousprime26.mileage_tracker.ui.SettingsScreen
import com.nebulousprime26.mileage_tracker.ui.StatsScreen
import com.nebulousprime26.mileage_tracker.ui.TripEntryScreen
import com.nebulousprime26.mileage_tracker.ui.TripViewModel
import com.nebulousprime26.mileage_tracker.ui.TripsScreen

private enum class Screen { Landing, Trips, Entry, Stats, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: TripViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()

            val versionName = remember { readVersionName() }

            MileageTheme(themeMode = themeMode) {
                var screen by remember { mutableStateOf(Screen.Landing) }
                var editingTrip by remember { mutableStateOf<Trip?>(null) }

                val trips by vm.trips.collectAsStateWithLifecycle()
                val maxEndMileage by vm.maxEndMileage.collectAsStateWithLifecycle()
                val lastEndPostalCode by vm.lastEndPostalCode.collectAsStateWithLifecycle()
                val lastStartPostalCode by vm.lastStartPostalCode.collectAsStateWithLifecycle()
                val lastLicensePlate by vm.lastLicensePlate.collectAsStateWithLifecycle()
                val postalFirst by vm.postalFirst.collectAsStateWithLifecycle()
                val draftLeft by vm.draftLeft.collectAsStateWithLifecycle()
                val roundTripAssumption by vm.roundTripAssumption.collectAsStateWithLifecycle()
                val autoFillEndTime by vm.autoFillEndTime.collectAsStateWithLifecycle()

                BackHandler(enabled = screen != Screen.Landing) {
                    when (screen) {
                        Screen.Entry -> {
                            editingTrip = null
                            screen = Screen.Trips
                        }
                        Screen.Stats -> {
                            screen = Screen.Trips
                        }
                        Screen.Settings -> {
                            screen = Screen.Landing
                        }
                        Screen.Trips -> {
                            screen = Screen.Landing
                        }
                        Screen.Landing -> { /* unreachable, enabled = false */ }
                    }
                }

                when (screen) {
                    Screen.Landing -> LandingScreen(
                        versionName = versionName,
                        onContinue = { screen = Screen.Trips },
                        onSettings = { screen = Screen.Settings },
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
                        onStats = { screen = Screen.Stats },
                        onBack = { screen = Screen.Landing },
                    )

                    Screen.Stats -> StatsScreen(
                        viewModel = vm,
                        onBack = { screen = Screen.Trips },
                    )

                    Screen.Settings -> SettingsScreen(
                        viewModel = vm,
                        onBack = { screen = Screen.Landing },
                    )

                    Screen.Entry -> TripEntryScreen(
                        existingTrip = editingTrip,
                        existingTrips = trips,
                        defaultStartMileage = maxEndMileage,
                        defaultStartPostalCode = lastEndPostalCode,
                        defaultEndPostalCode = if (roundTripAssumption) {
                            lastStartPostalCode
                        } else {
                            null
                        },
                        defaultLicensePlate = lastLicensePlate,
                        postalFirst = postalFirst,
                        draftLeft = draftLeft,
                        autoFillEndTime = autoFillEndTime,
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

    private fun readVersionName(): String {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}