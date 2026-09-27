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
 * Which language the app should display. SYSTEM follows the device
 * setting, ENGLISH and DUTCH force a specific locale.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    DUTCH("nl");

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

class SettingsRepository(private val context: Context) {

    private val fabOnRightKey = booleanPreferencesKey("fab_on_right")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val postalFirstKey = booleanPreferencesKey("postal_first")
    private val draftLeftKey = booleanPreferencesKey("draft_left")
    private val roundTripAssumptionKey = booleanPreferencesKey("round_trip_assumption")
    private val autoFillEndTimeKey = booleanPreferencesKey("auto_fill_end_time")
    private val allowSpacesInPostalKey = booleanPreferencesKey("allow_spaces_in_postal")

    val fabOnRight: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[fabOnRightKey] ?: true }

    suspend fun setFabOnRight(onRight: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[fabOnRightKey] = onRight }
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
        .map { prefs -> ThemeMode.fromStorage(prefs[themeModeKey]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    val appLanguage: Flow<AppLanguage> = context.settingsDataStore.data
        .map { prefs -> AppLanguage.fromStorage(prefs[appLanguageKey]) }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs -> prefs[appLanguageKey] = language.name }
    }

    val postalFirst: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[postalFirstKey] ?: true }

    suspend fun setPostalFirst(postalFirst: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[postalFirstKey] = postalFirst }
    }

    val draftLeft: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[draftLeftKey] ?: true }

    suspend fun setDraftLeft(draftLeft: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[draftLeftKey] = draftLeft }
    }

    val roundTripAssumption: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[roundTripAssumptionKey] ?: true }

    suspend fun setRoundTripAssumption(value: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[roundTripAssumptionKey] = value }
    }

    val autoFillEndTime: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[autoFillEndTimeKey] ?: true }

    suspend fun setAutoFillEndTime(value: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[autoFillEndTimeKey] = value }
    }

    val allowSpacesInPostal: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[allowSpacesInPostalKey] ?: false }

    suspend fun setAllowSpacesInPostal(value: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[allowSpacesInPostalKey] = value }
    }
}