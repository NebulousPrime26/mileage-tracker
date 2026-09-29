package com.nebulousprime26.mileage_tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.ui.TripViewModel
import com.nebulousprime26.mileage_tracker.data.settings.AppLanguage
import com.nebulousprime26.mileage_tracker.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TripViewModel,
    onBack: () -> Unit,
) {
    val fabOnRight by viewModel.fabOnRight.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val postalFirst by viewModel.postalFirst.collectAsStateWithLifecycle()
    val draftLeft by viewModel.draftLeft.collectAsStateWithLifecycle()
    val roundTripAssumption by viewModel.roundTripAssumption.collectAsStateWithLifecycle()
    val autoFillEndTime by viewModel.autoFillEndTime.collectAsStateWithLifecycle()
    val allowSpacesInPostal by viewModel.allowSpacesInPostal.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Appearance ───────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleMedium,
            )

            ThemeModeSelector(
                selected = themeMode,
                onSelect = { viewModel.setThemeMode(it) },
            )

            HorizontalDivider()

            // ── Language ─────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_language),
                style = MaterialTheme.typography.titleMedium,
            )

            LanguageSelector(
                selected = appLanguage,
                onSelect = { viewModel.setAppLanguage(it) },
            )

            HorizontalDivider()

            // ── Positions ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_positions),
                style = MaterialTheme.typography.titleMedium,
            )

            PositionSetting(
                title = stringResource(R.string.settings_add_trip_button),
                explanation = stringResource(R.string.settings_add_trip_button_desc),
                isRight = fabOnRight,
                onToggle = { viewModel.setFabOnRight(it) },
            )

            PositionSetting(
                title = stringResource(R.string.settings_postal_group),
                explanation = stringResource(R.string.settings_postal_group_desc),
                isRight = !postalFirst,
                onToggle = { viewModel.setPostalFirst(!it) },
            )

            PositionSetting(
                title = stringResource(R.string.settings_draft_button),
                explanation = stringResource(R.string.settings_draft_button_desc),
                isRight = !draftLeft,
                onToggle = { viewModel.setDraftLeft(!it) },
            )

            HorizontalDivider()

            // ── Behaviour ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_behaviour),
                style = MaterialTheme.typography.titleMedium,
            )

            ToggleSetting(
                title = stringResource(R.string.settings_round_trips),
                explanation = stringResource(R.string.settings_round_trips_desc),
                checked = roundTripAssumption,
                onToggle = { viewModel.setRoundTripAssumption(it) },
            )

            ToggleSetting(
                title = stringResource(R.string.settings_auto_fill_end_time),
                explanation = stringResource(R.string.settings_auto_fill_end_time_desc),
                checked = autoFillEndTime,
                onToggle = { viewModel.setAutoFillEndTime(it) },
            )

            ToggleSetting(
                title = stringResource(R.string.settings_allow_spaces),
                explanation = null,
                checked = allowSpacesInPostal,
                onToggle = { viewModel.setAllowSpacesInPostal(it) },
            )

            HorizontalDivider()
        }
    }
}

@Composable
private fun PositionSetting(
    title: String,
    explanation: String,
    isRight: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.settings_position_left),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRight) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Switch(
                checked = isRight,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                text = stringResource(R.string.settings_position_right),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    explanation: String?,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
            )
        }
        if (explanation != null) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        ThemeMode.entries.forEach { mode ->
            val label = stringResource(
                when (mode) {
                    ThemeMode.SYSTEM -> R.string.settings_theme_system
                    ThemeMode.LIGHT -> R.string.settings_theme_light
                    ThemeMode.DARK -> R.string.settings_theme_dark
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        AppLanguage.entries.forEach { language ->
            val label = stringResource(
                when (language) {
                    AppLanguage.SYSTEM -> R.string.settings_language_system
                    AppLanguage.ENGLISH -> R.string.settings_language_english
                    AppLanguage.DUTCH -> R.string.settings_language_dutch
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = language == selected,
                        onClick = { onSelect(language) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = language == selected, onClick = null)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}