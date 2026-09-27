package com.nebulousprime26.mileage_tracker

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nebulousprime26.mileage_tracker.data.AppLanguage
import com.nebulousprime26.mileage_tracker.data.Trip
import com.nebulousprime26.mileage_tracker.ui.LandingScreen
import com.nebulousprime26.mileage_tracker.ui.MileageTheme
import com.nebulousprime26.mileage_tracker.ui.SettingsScreen
import com.nebulousprime26.mileage_tracker.ui.StatsScreen
import com.nebulousprime26.mileage_tracker.ui.TripEntryScreen
import com.nebulousprime26.mileage_tracker.ui.TripViewModel
import com.nebulousprime26.mileage_tracker.ui.TripsScreen

private enum class Screen { Landing, Trips, Entry, Stats, Settings }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: TripViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val appLanguage by vm.appLanguage.collectAsStateWithLifecycle()

            // Read the version once from the installed APK's manifest.
            val versionName = remember { readVersionName() }

            // React to language changes: when the preference shifts, ask
            // AppCompat to apply the new locale. This causes a
            // configuration change, which recreates the activity, so the
            // rest of this composable never sees a half-updated state.
            //
            // This only works because MainActivity extends
            // AppCompatActivity — AppCompatDelegate.setApplicationLocales
            // is a no-op on a plain ComponentActivity.
            LaunchedEffect(appLanguage) {
                val target = appLanguage.tag
                val current = AppCompatDelegate.getApplicationLocales()
                    .toLanguageTags()
                    .takeIf { it.isNotEmpty() }
                if (target != current) {
                    AppCompatDelegate.setApplicationLocales(
                        if (target == null) LocaleListCompat.getEmptyLocaleList()
                        else LocaleListCompat.forLanguageTags(target),
                    )
                }
            }

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
                val allowSpacesInPostal by vm.allowSpacesInPostal.collectAsStateWithLifecycle()

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
                        viewModel = vm,
                        onContinue = { screen = Screen.Trips },
                        onSettings = { screen = Screen.Settings },
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
                        // Only pre-fill the end postal as a return leg when
                        // the user hasn't opted out of the round trip
                        // assumption in Settings.
                        defaultEndPostalCode = if (roundTripAssumption) {
                            lastStartPostalCode
                        } else {
                            null
                        },
                        defaultLicensePlate = lastLicensePlate,
                        postalFirst = postalFirst,
                        draftLeft = draftLeft,
                        autoFillEndTime = autoFillEndTime,
                        allowSpacesInPostal = allowSpacesInPostal,
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

    /**
     * Reads the app's versionName from its own manifest. Returns
     * "unknown" if the manifest doesn't declare one or the lookup fails,
     * so the UI always has something to show.
     */
    private fun readVersionName(): String {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}