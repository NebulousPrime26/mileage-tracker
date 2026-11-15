package com.nebulousprime26.mileage_tracker.ui.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.nebulousprime26.mileage_tracker.settings.data.SettingsRepository
import com.nebulousprime26.mileage_tracker.settings.model.AppLanguage
import com.nebulousprime26.mileage_tracker.settings.model.Theme

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    val fabOnRight = repo.fabOnRight.asState(true)
    val theme = repo.theme.asState(Theme.SYSTEM)
    val appLanguage = repo.appLanguage.asState(AppLanguage.SYSTEM)
    val postalFirst = repo.postalFirst.asState(true)
    val draftLeft = repo.draftLeft.asState(true)
    val roundTripAssumption = repo.roundTripAssumption.asState(true)
    val autoFillEndTime = repo.autoFillEndTime.asState(true)
    val allowSpacesInPostal = repo.allowSpacesInPostal.asState(false)

    fun setFabOnRight(v: Boolean) { viewModelScope.launch { repo.setFabOnRight(v) } }
    fun setTheme(v: Theme) { viewModelScope.launch { repo.setTheme(v) } }
    fun setAppLanguage(v: AppLanguage) { viewModelScope.launch { repo.setAppLanguage(v) } }
    fun setPostalFirst(v: Boolean) { viewModelScope.launch { repo.setPostalFirst(v) } }
    fun setDraftLeft(v: Boolean) { viewModelScope.launch { repo.setDraftLeft(v) } }
    fun setRoundTripAssumption(v: Boolean) { viewModelScope.launch { repo.setRoundTripAssumption(v) } }
    fun setAutoFillEndTime(v: Boolean) { viewModelScope.launch { repo.setAutoFillEndTime(v) } }
    fun setAllowSpacesInPostal(v: Boolean) { viewModelScope.launch { repo.setAllowSpacesInPostal(v) } }

    private fun <T> Flow<T>.asState(default: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), default)
}