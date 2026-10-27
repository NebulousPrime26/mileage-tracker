package com.nebulousprime26.mileage_tracker.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** How the app should decide between light and dark colours. */
enum class Theme {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): Theme =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * Wraps the app's UI in a MaterialTheme whose colour scheme is chosen
 * by the user's preference. SYSTEM follows the OS setting; LIGHT and
 * DARK force the respective scheme regardless of the OS.
 */
@Composable
fun AppTheme(
    theme: Theme,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (theme) {
        Theme.SYSTEM -> isSystemInDarkTheme()
        Theme.LIGHT -> false
        Theme.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}