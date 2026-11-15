package com.nebulousprime26.mileage_tracker.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.nebulousprime26.mileage_tracker.R

@Composable
fun ImportDoneDialog(
    count: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_done_title)) },
        text = {
            Text(
                if (count == 1) {
                    stringResource(R.string.import_done_one)
                } else {
                    stringResource(R.string.import_done_many, count)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
    )
}
