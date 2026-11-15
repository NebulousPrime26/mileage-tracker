package com.nebulousprime26.mileage_tracker.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Enum to indicate the app's UI theme. */
enum class Theme (val darkScheme: Boolean?){
    SYSTEM(null),
    LIGHT(false),
    DARK(true);

    companion object {
        fun fromStorage(value: String?): Theme =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Wraps the app's UI in Material theme based on the user preference. */
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