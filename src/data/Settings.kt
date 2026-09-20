package com.nebulousprime26.mileage_tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

/**
 * Persistent user preferences that aren't part of the trip data.
 * Currently just the FAB side, but this is where future appearance
 * options (theme, units, default sorting) would live.
 */
class SettingsRepository(private val context: Context) {

    private val fabOnRightKey = booleanPreferencesKey("fab_on_right")

    /** True = FAB on the right side. False = FAB on the left. Defaults to right. */
    val fabOnRight: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[fabOnRightKey] ?: true }

    suspend fun setFabOnRight(onRight: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[fabOnRightKey] = onRight
        }
    }
}