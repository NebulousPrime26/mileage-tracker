package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.nebulousprime26.mileage_tracker.data.ThemeMode

/**
 * Wraps the app's UI in a MaterialTheme whose colour scheme is chosen
 * by the user's preference. SYSTEM follows the OS setting; LIGHT and
 * DARK force the respective scheme regardless of the OS.
 */
@Composable
fun MileageTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}