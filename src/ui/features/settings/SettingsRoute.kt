package com.nebulousprime26.mileage_tracker.ui.features.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    SettingsScreen(
        viewModel = viewModel,
        onBack = onBack,
    )
}