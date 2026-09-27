package com.nebulousprime26.mileage_tracker.ui

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nebulousprime26.mileage_tracker.data.TripCrypto
import kotlinx.coroutines.launch

/**
 * Shows the freshly generated key before anything is written to disk.
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    text = "Backup will be saved as $filename.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    scope.launch {
                        val clipData = ClipData.newPlainText("Mileage backup key", key)
                        clipboard.setClipEntry(clipData.toClipEntry())
                        Toast.makeText(context, "Key copied", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Copy key")
                }
                TextButton(onClick = onConfirm) {
                    Text("I understand")
                }
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

/**
 * Final warning before the import destroys existing data.
 */
@Composable
fun ImportConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace all trips?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Importing will permanently delete every trip currently " +
                        "in the app, including drafts, and replace them with the " +
                        "contents of the backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "This cannot be undone. Export your current trips first " +
                        "if you want to keep them.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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