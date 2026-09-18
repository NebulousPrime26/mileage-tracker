package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nebulousprime26.mileage_tracker.data.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryScreen(
    onSave: (Trip) -> Unit,
    onCancel: () -> Unit,
) {
    var startPostalCode by remember { mutableStateOf("") }
    var endPostalCode by remember { mutableStateOf("") }
    var startMileageText by remember { mutableStateOf("") }
    var endMileageText by remember { mutableStateOf("") }
    var privateUse by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    // Date state — epoch millis, defaults to today
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val startMileage = startMileageText.toDoubleOrNull()
    val endMileage = endMileageText.toDoubleOrNull()
    val canSave = startMileage != null &&
        endMileage != null &&
        endMileage >= startMileage

    Scaffold(
        topBar = { TopAppBar(title = { Text("New trip") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Date field ───────────────────────────────────────────
            DateField(
                dateMillis = dateMillis,
                onClick = { showDatePicker = true },
            )

            OutlinedTextField(
                value = startPostalCode,
                onValueChange = { startPostalCode = it },
                label = { Text("Start postal code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = endPostalCode,
                onValueChange = { endPostalCode = it },
                label = { Text("End postal code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = startMileageText,
                onValueChange = { startMileageText = it },
                label = { Text("Start mileage") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = endMileageText,
                onValueChange = { endMileageText = it },
                label = { Text("End mileage") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (startMileage != null && endMileage != null && endMileage < startMileage) {
                Text(
                    text = "⚠ End mileage must be greater than or equal to start mileage",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = privateUse, onCheckedChange = { privateUse = it })
                Text("Private use")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        Trip(
                            date = dateMillis,
                            startPostalCode = startPostalCode,
                            endPostalCode = endPostalCode,
                            startMileage = startMileage!!,
                            endMileage = endMileage!!,
                            privateUse = privateUse,
                            notes = notes,
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Save trip")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }

    // ── Date picker dialog ───────────────────────────────────────────
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis,
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * A read-only text field that looks like a normal form field but opens
 * the date picker when tapped anywhere on its surface.
 */
@Composable
private fun DateField(
    dateMillis: Long,
    onClick: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val formatted = formatter.format(Date(dateMillis))

    Box {
        OutlinedTextField(
            value = formatted,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay that captures taps across the whole field
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}