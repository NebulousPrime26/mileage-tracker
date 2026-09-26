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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val mileageFormatter = DecimalFormat("#,##0.##")
private val shortDateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

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
        val v = existingTrip?.startMileage
        mutableStateOf(if (v == null || v == 0.0) "" else v.toString())
    }
    var endMileageText by remember(existingTrip?.id) {
        val v = existingTrip?.endMileage
        mutableStateOf(if (v == null || v == 0.0) "" else v.toString())
    }
    var privateUse by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.privateUse ?: false)
    }
    var notes by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.notes ?: "")
    }
    var startDateMillis by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.startDate ?: System.currentTimeMillis())
    }
    var endDateMillis by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.endDate ?: System.currentTimeMillis())
    }

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

    // ── Validation ───────────────────────────────────────────────────

    val dateOrderError: String? = if (endDateMillis < startDateMillis) {
        "End date & time can't be before the start."
    } else {
        null
    }

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

    val earlierTrip: Trip? = if (startMileage == null) {
        null
    } else {
        existingTrips
            .filter { it.id != existingTrip?.id && it.startDate < startDateMillis }
            .maxByOrNull { it.endMileage }
    }

    val laterTrip: Trip? = if (endMileage == null) {
        null
    } else {
        existingTrips
            .filter { it.id != existingTrip?.id && it.startDate > startDateMillis }
            .minByOrNull { it.startMileage }
    }

    val chronologyError: String? = when {
        startMileage == null || endMileage == null || endMileage < startMileage -> null

        earlierTrip != null && startMileage < earlierTrip.endMileage ->
            "Start mileage can't be below ${mileageFormatter.format(earlierTrip.endMileage)} km: " +
                "the trip on ${shortDateFormatter.format(Date(earlierTrip.startDate))} " +
                "already ended there."

        laterTrip != null && endMileage > laterTrip.startMileage ->
            "End mileage can't exceed ${mileageFormatter.format(laterTrip.startMileage)} km: " +
                "the trip on ${shortDateFormatter.format(Date(laterTrip.startDate))} " +
                "already starts there."

        else -> null
    }

    val canSave = startPostalCode.isNotBlank() &&
        endPostalCode.isNotBlank() &&
        startMileage != null &&
        endMileage != null &&
        endMileage >= startMileage &&
        dateOrderError == null &&
        conflictingTrip == null &&
        chronologyError == null

    val hasDraftContent = startPostalCode.isNotBlank() ||
        endPostalCode.isNotBlank() ||
        startMileageText.isNotBlank() ||
        endMileageText.isNotBlank() ||
        notes.isNotBlank()
    val canSaveDraft = hasDraftContent

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
                text = "* Required field. Fields can be left blank if saving as a draft.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DateTimePickerField(
                label = "Start date & time *",
                value = startDateMillis,
                onValueChange = { startDateMillis = it },
            )

            DateTimePickerField(
                label = "End date & time *",
                value = endDateMillis,
                onValueChange = { endDateMillis = it },
            )

            if (dateOrderError != null) {
                Text(
                    text = "⚠ $dateOrderError",
                    color = MaterialTheme.colorScheme.error,
                )
            }

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
                isError = conflictingTrip != null || chronologyError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = endMileageText,
                onValueChange = { endMileageText = it },
                label = { Text("End mileage *") },
                singleLine = true,
                isError = conflictingTrip != null || chronologyError != null,
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
                Text(
                    text = "⚠ Mileage range overlaps an existing trip " +
                        "(${mileageFormatter.format(conflictingTrip.startMileage)}–" +
                        "${mileageFormatter.format(conflictingTrip.endMileage)} km " +
                        "on ${shortDateFormatter.format(Date(conflictingTrip.startDate))})",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (chronologyError != null) {
                Text(
                    text = "⚠ $chronologyError",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = privateUse, onCheckedChange = { privateUse = it })
                Text("Private")
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
                    text = if (hasDraftContent) {
                        "Fill in all required fields to save, or save as a draft."
                    } else {
                        "Fill in the fields above to save."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    onSave(
                        Trip(
                            id = tripId,
                            startDate = startDateMillis,
                            endDate = endDateMillis,
                            startPostalCode = startPostalCode,
                            endPostalCode = endPostalCode,
                            startMileage = startMileage!!,
                            endMileage = endMileage!!,
                            privateUse = privateUse,
                            notes = notes,
                            isDraft = false,
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    when {
                        !isEditing -> "Save trip"
                        existingTrip.isDraft -> "Complete trip"
                        else -> "Save changes"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    onSave(
                        Trip(
                            id = tripId,
                            startDate = startDateMillis,
                            endDate = endDateMillis,
                            startPostalCode = startPostalCode,
                            endPostalCode = endPostalCode,
                            startMileage = startMileage ?: 0.0,
                            endMileage = endMileage ?: 0.0,
                            privateUse = privateUse,
                            notes = notes,
                            isDraft = true,
                        )
                    )
                },
                enabled = canSaveDraft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (existingTrip?.isDraft == true) "Update draft"
                    else "Save as draft"
                )
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * A read-only field showing a formatted date-time, plus its own date
 * picker followed by a time picker. Managing the two dialogs inside
 * this composable keeps the parent screen free of picker state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateUtcMillis by remember { mutableStateOf<Long?>(null) }

    val formatter = remember {
        SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
    }
    val formatted = formatter.format(Date(value))

    Box {
        OutlinedTextField(
            value = formatted,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = { showDatePicker = true }),
        )
    }

    if (showDatePicker) {
        val initialUtcDateMillis = remember(value) {
            val local = Calendar.getInstance().apply { timeInMillis = value }
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

    if (showTimePicker) {
        val current = remember(value) {
            Calendar.getInstance().apply { timeInMillis = value }
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
                        onValueChange(localCal.timeInMillis)
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