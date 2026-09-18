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
import com.nebulousprime26.mileage_tracker.ui.LandingScreen
import com.nebulousprime26.mileage_tracker.ui.TripEntryScreen
import com.nebulousprime26.mileage_tracker.ui.TripViewModel

private enum class Screen { Landing, Entry }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: TripViewModel = viewModel()
                var screen by remember { mutableStateOf(Screen.Landing) }

                when (screen) {
                    Screen.Landing -> LandingScreen(
                        viewModel = vm,
                        onContinue = { screen = Screen.Entry },
                        onImport = { /* TODO */ },
                        onExport = { /* TODO */ },
                    )

                    Screen.Entry -> TripEntryScreen(
                        onSave = { trip ->
                            vm.addTrip(trip)
                            screen = Screen.Landing
                        },
                        onCancel = { screen = Screen.Landing },
                    )
                }
            }
        }
    }
}