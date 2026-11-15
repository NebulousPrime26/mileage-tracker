package com.nebulousprime26.mileage_tracker.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.nebulousprime26.mileage_tracker.R

@Composable
fun ExportDoneDialog(
    filename: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_done_title)) },
        text = {
            Text(stringResource(R.string.export_done_message, filename))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
    )
}