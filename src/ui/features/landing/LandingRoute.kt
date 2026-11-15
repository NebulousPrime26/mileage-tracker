package com.nebulousprime26.mileage_tracker.ui.features.landing

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LandingRoute(
    versionName: String,
    viewModel: LandingViewModel = viewModel(),
    onContinue: () -> Unit,
    onSettings: () -> Unit,
) {
    LandingScreen(
        versionName = versionName,
        viewModel = viewModel,
        onContinue = onContinue,
        onSettings = onSettings,
    )
}