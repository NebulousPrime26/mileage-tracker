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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nebulousprime26.mileage_tracker.data.Trip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryScreen(
    existingTrip: Trip? = null,
    existingTrips: List<Trip> = emptyList(),
    defaultStartMileage: Double? = null,
    defaultStartPostalCode: String? = null,
    onSave: (Trip) -> Unit,
    onCancel: () -> Unit,
) {
    val isEditing = existingTrip != null
    val tripId = existingTrip?.id ?: 0L

    var startPostalCode by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.startPostalCode ?: "")
    }
    var endPostalCode by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.endPostalCode ?: "")
    }
    var startMileageText by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.startMileage?.toString() ?: "")
    }
    var endMileageText by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.endMileage?.toString() ?: "")
    }
    var privateUse by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.privateUse ?: false)
    }
    var notes by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.notes ?: "")
    }
    var dateTimeMillis by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.date ?: System.currentTimeMillis())
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateUtcMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(existingTrip?.id, defaultStartMileage, defaultStartPostalCode) {
        if (existingTrip == null) {
            if (startMileageText.isEmpty() && defaultStartMileage != null) {
                startMileageText = defaultStartMileage.toString()
            }
            if (startPostalCode.isEmpty() && !defaultStartPostalCode.isNullOrBlank()) {
                startPostalCode = defaultStartPostalCode.uppercase(Locale.ROOT)
            }
        }
    }

    val startMileage = startMileageText.toDoubleOrNull()
    val endMileage = endMileageText.toDoubleOrNull()

    // A trip's [start, end] range must not intersect any other trip's range.
    // Touching at a boundary (one trip ends where the next begins) is allowed —
    // that's the normal chaining pattern for a mileage log. Overlap requires
    // strict inequality on both sides.
    val conflictingTrip: Trip? = if (
        startMileage == null ||
        endMileage == null ||
        endMileage < startMileage
    ) {
        null
    } else {
        existingTrips.firstOrNull { other ->
            other.id != existingTrip?.id &&
                startMileage < other.endMileage &&
                other.startMileage < endMileage
        }
    }

    val canSave = startPostalCode.isNotBlank() &&
        endPostalCode.isNotBlank() &&
        startMileage != null &&
        endMileage != null &&
        endMileage >= startMileage &&
        conflictingTrip == null

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEditing) "Edit trip" else "New trip") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "* Required field",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DateTimeField(
                dateTimeMillis = dateTimeMillis,
                onClick = { showDatePicker = true },
            )

            OutlinedTextField(
                value = startPostalCode,
                onValueChange = { startPostalCode = it.uppercase(Locale.ROOT) },
                label = { Text("Start postal code *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = endPostalCode,
                onValueChange = { endPostalCode = it.uppercase(Locale.ROOT) },
                label = { Text("End postal code *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = startMileageText,
                onValueChange = { startMileageText = it },
                label = { Text("Start mileage *") },
                singleLine = true,
                isError = conflictingTrip != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = endMileageText,
                onValueChange = { endMileageText = it },
                label = { Text("End mileage *") },
                singleLine = true,
                isError = conflictingTrip != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (startMileage != null && endMileage != null && endMileage < startMileage) {
                Text(
                    text = "⚠ End mileage must be greater than or equal to start mileage",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (conflictingTrip != null) {
                val formatter = remember {
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                }
                Text(
                    text = "⚠ Mileage range overlaps an existing trip " +
                        "(${conflictingTrip.startMileage}–${conflictingTrip.endMileage} km " +
                        "on ${formatter.format(Date(conflictingTrip.date))})",
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

            if (!canSave) {
                Text(
                    text = "Fill in all required fields to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    onSave(
                        Trip(
                            id = tripId,
                            date = dateTimeMillis,
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
                Text(if (isEditing) "Save changes" else "Save trip")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }

    // ── Date picker ──────────────────────────────────────────────────
    if (showDatePicker) {
        val initialUtcDateMillis = remember(dateTimeMillis) {
            val local = Calendar.getInstance().apply { timeInMillis = dateTimeMillis }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    local.get(Calendar.YEAR),
                    local.get(Calendar.MONTH),
                    local.get(Calendar.DAY_OF_MONTH),
                )
            }.timeInMillis
        }

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcDateMillis,
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDateUtcMillis = pickerState.selectedDateMillis
                        showDatePicker = false
                        if (pendingDateUtcMillis != null) showTimePicker = true
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

    // ── Time picker ──────────────────────────────────────────────────
    if (showTimePicker) {
        val current = remember(dateTimeMillis) {
            Calendar.getInstance().apply { timeInMillis = dateTimeMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = current.get(Calendar.HOUR_OF_DAY),
            initialMinute = current.get(Calendar.MINUTE),
            is24Hour = true,
        )

        AlertDialog(
            onDismissRequest = {
                pendingDateUtcMillis = null
                showTimePicker = false
            },
            title = { Text("Select time") },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dateUtc = pendingDateUtcMillis ?: return@TextButton
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = dateUtc
                        }
                        val localCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        dateTimeMillis = localCal.timeInMillis
                        pendingDateUtcMillis = null
                        showTimePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDateUtcMillis = null
                        showTimePicker = false
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DateTimeField(
    dateTimeMillis: Long,
    onClick: () -> Unit,
) {
    val formatter = remember {
        SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
    }
    val formatted = formatter.format(Date(dateTimeMillis))

    Box {
        OutlinedTextField(
            value = formatted,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date & time *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}