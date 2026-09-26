package com.nebulousprime26.mileage_tracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LandingScreen(
    versionName: String,
    viewModel: TripViewModel,
    onContinue: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()

    // SAF picker for choosing a backup file to import.
    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.beginImport(uri)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Mileage",
                    style = MaterialTheme.typography.headlineLarge,
                )

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                ) {
                    Text("Continue")
                }

                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text("Settings")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            importPicker.launch(arrayOf("application/octet-stream"))
                        },
                        enabled = backupState !is TripViewModel.BackupState.Working,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Import")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.exportTrips() },
                        enabled = backupState !is TripViewModel.BackupState.Working,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Export")
                    }
                }
            }

            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }

    // ── Backup dialogs ───────────────────────────────────────────────
    when (val state = backupState) {
        is TripViewModel.BackupState.Exported -> ExportKeyDialog(
            key = state.key,
            filename = state.filename,
            onDismiss = { viewModel.dismissBackupState() },
        )
        is TripViewModel.BackupState.ImportAwaitingKey -> ImportKeyDialog(
            onConfirm = { key -> viewModel.importTrips(state.uri, key) },
            onDismiss = { viewModel.dismissBackupState() },
        )
        is TripViewModel.BackupState.Imported -> ImportDoneDialog(
            count = state.count,
            onDismiss = { viewModel.dismissBackupState() },
        )
        is TripViewModel.BackupState.Failed -> BackupErrorDialog(
            message = state.message,
            onDismiss = { viewModel.dismissBackupState() },
        )
        TripViewModel.BackupState.Idle,
        TripViewModel.BackupState.Working -> Unit
    }
}