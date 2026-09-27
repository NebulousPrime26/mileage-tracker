package com.nebulousprime26.mileage_tracker.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nebulousprime26.mileage_tracker.data.TripCrypto

/**
 * Shows the freshly generated key before anything is written to disk.
 * The key is placed on the clipboard as soon as the dialog appears.
 * The backup file is only created if the user taps "I understand".
 */
@Composable
fun ExportKeyDialog(
    key: String,
    filename: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val clipboard = LocalClipboard.current

    // Copy the key once, when the dialog first appears. Keyed on `key`
    // so a re-composition doesn't re-copy (which would be harmless but
    // would keep firing the suspend call).
    LaunchedEffect(key) {
        val clipData = ClipData.newPlainText("Mileage backup key", key)
        clipboard.setClipEntry(clipData.toClipEntry())
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save your encryption key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This key won't be shown again. Write it down to " +
                        "import this backup later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                        ),
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Text(
                    text = "Copied to clipboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Backup will be saved as $filename.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("I understand")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

/** Confirmation shown after the backup file has been written. */
@Composable
fun ExportDoneDialog(
    filename: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup saved") },
        text = {
            Text("Saved to Downloads as $filename.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

/** Asks the user to type or paste the key for an import. */
@Composable
fun ImportKeyDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var keyInput by remember { mutableStateOf("") }
    val isValid = TripCrypto.parseKey(keyInput) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter encryption key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paste or type the key that was shown when this backup was created.")
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Key") },
                    placeholder = { Text("A3F2 B19C 4D7E …") },
                    isError = keyInput.isNotEmpty() && !isValid,
                    supportingText = if (keyInput.isNotEmpty() && !isValid) {
                        { Text("Key must be 64 hex characters") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(keyInput) },
                enabled = isValid,
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ImportConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace all trips?") },
        text = {
            Text(
                text = "All existing trips will be deleted and replaced " +
                    "by the backup. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Replace and import",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Post-import confirmation. */
@Composable
fun ImportDoneDialog(
    count: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import complete") },
        text = {
            Text(
                if (count == 1) "1 trip was imported."
                else "$count trips were imported."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

/** Generic failure message for either direction. */
@Composable
fun BackupErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup failed") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}