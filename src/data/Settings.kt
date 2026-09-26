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
    private val postalFirstKey = booleanPreferencesKey("postal_first")
    private val draftLeftKey = booleanPreferencesKey("draft_left")
    private val roundTripAssumptionKey = booleanPreferencesKey("round_trip_assumption")
    private val autoFillEndTimeKey = booleanPreferencesKey("auto_fill_end_time")
    private val allowSpacesInPostalKey = booleanPreferencesKey("allow_spaces_in_postal")

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

    /** True = postal group on the left, mileage on the right. Defaults to true. */
    val postalFirst: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[postalFirstKey] ?: true }

    suspend fun setPostalFirst(postalFirst: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[postalFirstKey] = postalFirst
        }
    }

    /** True = "Save as draft" on the left, save on the right. Defaults to true. */
    val draftLeft: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[draftLeftKey] ?: true }

    suspend fun setDraftLeft(draftLeft: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[draftLeftKey] = draftLeft
        }
    }

    /**
     * True = assume the next trip is a return leg, so the end postal is
     * pre-filled with the previous trip's start. Defaults to true.
     */
    val roundTripAssumption: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[roundTripAssumptionKey] ?: true }

    suspend fun setRoundTripAssumption(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[roundTripAssumptionKey] = value
        }
    }

    /**
     * True = set the end time to the current time when the end mileage
     * is first filled in on a new trip. Defaults to true.
     */
    val autoFillEndTime: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[autoFillEndTimeKey] ?: true }

    suspend fun setAutoFillEndTime(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[autoFillEndTimeKey] = value
        }
    }

    /**
     * True = postal codes may contain spaces. False = spaces are stripped
     * as the user types. Defaults to false.
     */
    val allowSpacesInPostal: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[allowSpacesInPostalKey] ?: false }

    suspend fun setAllowSpacesInPostal(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[allowSpacesInPostalKey] = value
        }
    }
}