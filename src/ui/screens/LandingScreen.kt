package com.nebulousprime26.mileage_tracker.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.data.backup.TripCsv
import com.nebulousprime26.mileage_tracker.ui.TripViewModel
import com.nebulousprime26.mileage_tracker.ui.dialogs.BackupErrorDialog
import com.nebulousprime26.mileage_tracker.ui.dialogs.ExportDoneDialog
import com.nebulousprime26.mileage_tracker.ui.dialogs.ExportKeyDialog
import com.nebulousprime26.mileage_tracker.ui.dialogs.ImportConfirmDialog
import com.nebulousprime26.mileage_tracker.ui.dialogs.ImportDoneDialog
import com.nebulousprime26.mileage_tracker.ui.dialogs.ImportKeyDialog

@Composable
fun LandingScreen(
    versionName: String,
    viewModel: TripViewModel,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
) {
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val shareUri by viewModel.shareUri.collectAsStateWithLifecycle()
    val busy = backupState is TripViewModel.BackupState.Working
    val context = LocalContext.current

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::beginImport) }

    // Hand a freshly prepared CSV to the system share sheet, then clear
    // the pending URI so the effect doesn't fire again.
    LaunchedEffect(shareUri) {
        val uri = shareUri ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = TripCsv.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        viewModel.clearShareUri()
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
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                ) {
                    Text(stringResource(R.string.landing_continue))
                }

                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.landing_settings))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { importPicker.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.landing_import))
                    }
                    OutlinedButton(
                        onClick = viewModel::exportTrips,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.landing_export))
                    }
                    OutlinedButton(
                        onClick = viewModel::shareTripsAsCsv,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.landing_share))
                    }
                }
            }

            Text(
                text = stringResource(R.string.version_format, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }

    BackupDialogs(state = backupState, viewModel = viewModel)
}

/**
 * Renders whichever backup dialog matches the current state. Extracted
 * from [LandingScreen] so the screen's body reads as one thing — the
 * layout — and the state-to-dialog mapping lives on its own.
 */
@Composable
private fun BackupDialogs(
    state: TripViewModel.BackupState,
    viewModel: TripViewModel,
) {
    when (state) {
        is TripViewModel.BackupState.ExportPreview -> ExportKeyDialog(
            key = state.key,
            filename = state.filename,
            onConfirm = viewModel::confirmExport,
            onCancel = viewModel::cancelExport,
        )

        is TripViewModel.BackupState.Exported -> ExportDoneDialog(
            filename = state.filename,
            onDismiss = viewModel::dismissBackupState,
        )

        is TripViewModel.BackupState.ImportAwaitingKey -> ImportKeyDialog(
            onConfirm = { key -> viewModel.submitImportKey(state.uri, key) },
            onDismiss = viewModel::dismissBackupState,
        )

        is TripViewModel.BackupState.ImportConfirming -> ImportConfirmDialog(
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::dismissBackupState,
        )

        is TripViewModel.BackupState.Imported -> ImportDoneDialog(
            count = state.count,
            onDismiss = viewModel::dismissBackupState,
        )

        is TripViewModel.BackupState.Failed -> BackupErrorDialog(
            message = state.message,
            onDismiss = viewModel::dismissBackupState,
        )

        TripViewModel.BackupState.Idle,
        TripViewModel.BackupState.Working -> Unit
    }
}