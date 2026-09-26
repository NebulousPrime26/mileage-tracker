package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebulousprime26.mileage_tracker.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TripViewModel,
    onBack: () -> Unit,
) {
    val fabOnRight by viewModel.fabOnRight.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val postalFirst by viewModel.postalFirst.collectAsStateWithLifecycle()
    val draftLeft by viewModel.draftLeft.collectAsStateWithLifecycle()
    val roundTripAssumption by viewModel.roundTripAssumption.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Appearance ───────────────────────────────────────────
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
            )

            ThemeModeSelector(
                selected = themeMode,
                onSelect = { viewModel.setThemeMode(it) },
            )

            HorizontalDivider()

            // ── Positions ────────────────────────────────────────────
            Text(
                text = "Positions",
                style = MaterialTheme.typography.titleMedium,
            )

            PositionSetting(
                title = "Add Trip button",
                explanation = "Where the floating button sits on the Trips screen.",
                isRight = fabOnRight,
                onToggle = { viewModel.setFabOnRight(it) },
            )

            PositionSetting(
                title = "Postal Code group",
                explanation = "Which side of the trip entry form the postal codes appear on, relative to mileage.",
                isRight = !postalFirst,
                onToggle = { viewModel.setPostalFirst(!it) },
            )

            PositionSetting(
                title = "Save Draft button",
                explanation = "Which side of the trip entry form the draft button appears on, relative to save.",
                isRight = !draftLeft,
                onToggle = { viewModel.setDraftLeft(!it) },
            )

            HorizontalDivider()

            // ── Behaviour ────────────────────────────────────────────
            Text(
                text = "Behaviour",
                style = MaterialTheme.typography.titleMedium,
            )

            ToggleSetting(
                title = "Assume round trips",
                explanation = "When adding a trip, pre-fill the end postal code with where the previous trip started.",
                checked = roundTripAssumption,
                onToggle = { viewModel.setRoundTripAssumption(it) },
            )

            HorizontalDivider()
        }
    }
}

/**
 * A single row showing "Title    Left [Switch] Right" with the toggle
 * cluster right-aligned, and an explanation beneath. The active side's
 * label is drawn in the primary colour so the state reads without
 * having to interpret the switch itself.
 */
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
                text = "Left",
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
                text = "Right",
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

/**
 * A simple on/off setting: the title on the left, the switch on the
 * right, and an explanation line below.
 */
@Composable
private fun ToggleSetting(
    title: String,
    explanation: String,
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
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            val label = when (mode) {
                ThemeMode.SYSTEM -> "Follow system"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            }

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
                RadioButton(
                    selected = mode == selected,
                    onClick = null,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}