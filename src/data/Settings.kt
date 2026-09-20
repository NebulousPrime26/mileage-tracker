package com.nebulousprime26.mileage_tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

/** How the app should decide between light and dark colours. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * Persistent user preferences that aren't part of the trip data.
 * Currently just the FAB side and the theme mode, but this is where
 * future appearance options (units, default sorting) would live.
 */
class SettingsRepository(private val context: Context) {

    private val fabOnRightKey = booleanPreferencesKey("fab_on_right")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    /** True = FAB on the right side. False = FAB on the left. Defaults to right. */
    val fabOnRight: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[fabOnRightKey] ?: true }

    suspend fun setFabOnRight(onRight: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[fabOnRightKey] = onRight
        }
    }

    /** Which theme the app should use. Defaults to following the system. */
    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
        .map { prefs -> ThemeMode.fromStorage(prefs[themeModeKey]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}