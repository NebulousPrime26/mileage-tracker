package com.nebulousprime26.mileage_tracker.ui.features.stats

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StatsRoute(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(),
) {
    StatsScreen(
        viewModel = viewModel,
        onBack = onBack,
    )
}