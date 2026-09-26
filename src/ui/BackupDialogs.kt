package com.nebulousprime26.mileage_tracker.ui

import android.content.ClipData
import android.widget.Toast
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
 * Shows the freshly generated key. Warning text makes clear that
 * dismissing this dialog discards the key permanently.
 */
@Composable
fun ExportKeyDialog(
    key: String,
    filename: String,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save your encryption key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Backup saved to Downloads as $filename.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "This key is not stored anywhere. Once you close this " +
                        "dialog it is gone forever. Write it down now — you will " +
                        "need it to import this backup.",
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
                TextButton(onClick = {
                    scope.launch {
                        val clipData = ClipData.newPlainText("Mileage backup key", key)
                        clipboard.setClipEntry(clipData.toClipEntry())
                        Toast.makeText(context, "Key copied", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Copy key")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("I've written it down")
            }
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
                Text("Import")
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