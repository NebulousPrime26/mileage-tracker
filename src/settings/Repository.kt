package com.nebulousprime26.mileage_tracker.settings

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

class SettingsRepository(private val context: Context) {
    private val store get() = context.settingsDataStore

    /** Read a boolean preference, falling back to [default] when unset. */
    private fun booleanPref(name: String, default: Boolean): Flow<Boolean> {
        val key = booleanPreferencesKey(name)
        return store.data.map { it[key] ?: default }
    }

    /** Write a boolean preference. */
    private suspend fun setBoolean(name: String, value: Boolean) {
        val key = booleanPreferencesKey(name)
        store.edit { it[key] = value }
    }

    /** Read a string preference, mapping through [decode]. */
    private fun <T> stringPref(
        name: String,
        decode: (String?) -> T,
    ): Flow<T> {
        val key = stringPreferencesKey(name)
        return store.data.map { decode(it[key]) }
    }

    /** Write a string preference from a value's [encode] representation. */
    private suspend fun <T> setString(
        name: String,
        value: T,
        encode: (T) -> String,
    ) {
        val key = stringPreferencesKey(name)
        store.edit { it[key] = encode(value) }
    }

    val fabOnRight: Flow<Boolean> = booleanPref("fab_on_right", true)
    suspend fun setFabOnRight(value: Boolean) = setBoolean("fab_on_right", value)

    val theme: Flow<Theme> = stringPref("theme_mode", Theme::fromStorage)
    suspend fun setTheme(value: Theme) = setString("theme_mode", value, Theme::name)

    val appLanguage: Flow<AppLanguage> = stringPref("app_language", AppLanguage::fromStorage)
    suspend fun setAppLanguage(value: AppLanguage) =
        setString("app_language", value, AppLanguage::name)

    val postalFirst: Flow<Boolean> = booleanPref("postal_first", true)
    suspend fun setPostalFirst(value: Boolean) = setBoolean("postal_first", value)

    val draftLeft: Flow<Boolean> = booleanPref("draft_left", true)
    suspend fun setDraftLeft(value: Boolean) = setBoolean("draft_left", value)

    val roundTripAssumption: Flow<Boolean> = booleanPref("round_trip_assumption", true)
    suspend fun setRoundTripAssumption(value: Boolean) =
        setBoolean("round_trip_assumption", value)

    val autoFillEndTime: Flow<Boolean> = booleanPref("auto_fill_end_time", true)
    suspend fun setAutoFillEndTime(value: Boolean) =
        setBoolean("auto_fill_end_time", value)

    val allowSpacesInPostal: Flow<Boolean> = booleanPref("allow_spaces_in_postal", false)
    suspend fun setAllowSpacesInPostal(value: Boolean) =
        setBoolean("allow_spaces_in_postal", value)
}