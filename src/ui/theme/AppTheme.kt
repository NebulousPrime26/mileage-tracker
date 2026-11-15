package com.nebulousprime26.mileage_tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

import com.nebulousprime26.mileage_tracker.settings.model.Theme

@Composable
fun AppTheme(
    theme: Theme,
    content: @Composable () -> Unit,
) {
    val darkScheme = theme.darkScheme ?: isSystemInDarkTheme()

    MaterialTheme(
        colorScheme = if (darkScheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}