package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
    defaultEndPostalCode: String? = null,
    defaultLicensePlate: String? = null,
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
    var licensePlate by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.licensePlate ?: "")
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

    // Seed new trips from the last completed trip: the odometer continues
    // from the highest end mileage, the journey starts where the last one
    // finished, and — assuming a round trip — ends where the last one
    // started. Only for new trips, only when the field is still empty,
    // and only once the values have actually arrived from the database.
    LaunchedEffect(
        existingTrip?.id,
        defaultStartMileage,
        defaultStartPostalCode,
        defaultEndPostalCode,
        defaultLicensePlate,
    ) {
        if (existingTrip == null) {
            if (startMileageText.isEmpty() && defaultStartMileage != null) {
                startMileageText = defaultStartMileage.toString()
            }
            if (startPostalCode.isEmpty() && !defaultStartPostalCode.isNullOrBlank()) {
                startPostalCode = defaultStartPostalCode.uppercase(Locale.ROOT)
            }
            if (endPostalCode.isEmpty() && !defaultEndPostalCode.isNullOrBlank()) {
                endPostalCode = defaultEndPostalCode.uppercase(Locale.ROOT)
            }
            if (licensePlate.isEmpty() && !defaultLicensePlate.isNullOrBlank()) {
                licensePlate = defaultLicensePlate.uppercase(Locale.ROOT)
            }
        }
    }

    // When adding a new trip, filling in the end mileage marks the moment
    // the trip ended, so the end time is bumped to now. The transition is
    // detected on the blank → non-blank edge, and the whole effect is
    // skipped for edits, so an existing trip's saved end time is never
    // overwritten by touching the mileage field.
    var lastEndMileageWasBlank by remember(existingTrip?.id) {
        mutableStateOf(endMileageText.isBlank())
    }
    LaunchedEffect(endMileageText, existingTrip?.id) {
        val nowBlank = endMileageText.isBlank()
        if (existingTrip == null && lastEndMileageWasBlank && !nowBlank) {
            endDateMillis = System.currentTimeMillis()
        }
        lastEndMileageWasBlank = nowBlank
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
        licensePlate.isNotBlank() ||
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Vehicle (horizontal: plate + private chip) ───────────
            HorizontalFieldGroup {
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it.uppercase(Locale.ROOT) },
                    label = { Text("License plate") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = privateUse,
                    onClick = { privateUse = !privateUse },
                    label = { Text("Private") },
                    leadingIcon = if (privateUse) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                            )
                        }
                    } else null,
                )
            }

            // ── Dates (horizontal: start + end) ──────────────────────
            HorizontalFieldGroup {
                DateTimePickerField(
                    label = "Start",
                    value = startDateMillis,
                    onValueChange = { startDateMillis = it },
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                DateTimePickerField(
                    label = "End",
                    value = endDateMillis,
                    onValueChange = { endDateMillis = it },
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
            }

            if (dateOrderError != null) {
                SectionError(dateOrderError)
            }

            // ── Postal (left) and mileage (right), side by side ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VerticalFieldGroup(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = startPostalCode,
                        onValueChange = { startPostalCode = it.uppercase(Locale.ROOT) },
                        label = { Text("Start postal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = endPostalCode,
                        onValueChange = { endPostalCode = it.uppercase(Locale.ROOT) },
                        label = { Text("End postal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                VerticalFieldGroup(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = startMileageText,
                        onValueChange = { startMileageText = it },
                        label = { Text("Start mileage") },
                        singleLine = true,
                        isError = conflictingTrip != null || chronologyError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = endMileageText,
                        onValueChange = { endMileageText = it },
                        label = { Text("End mileage") },
                        singleLine = true,
                        isError = conflictingTrip != null || chronologyError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (startMileage != null && endMileage != null && endMileage < startMileage) {
                SectionError("End mileage must be greater than or equal to start mileage")
            }
            if (conflictingTrip != null) {
                SectionError(
                    "Mileage range overlaps an existing trip " +
                        "(${mileageFormatter.format(conflictingTrip.startMileage)}–" +
                        "${mileageFormatter.format(conflictingTrip.endMileage)} km " +
                        "on ${shortDateFormatter.format(Date(conflictingTrip.startDate))})"
                )
            }
            if (chronologyError != null) {
                SectionError(chronologyError)
            }

            // ── Notes (no highlight) ─────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 3,
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

            // ── Actions: draft on the left, save on the right ────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onSave(
                            Trip(
                                id = tripId,
                                startDate = startDateMillis,
                                endDate = endDateMillis,
                                startPostalCode = startPostalCode,
                                endPostalCode = endPostalCode,
                                licensePlate = licensePlate,
                                startMileage = startMileage ?: 0.0,
                                endMileage = endMileage ?: 0.0,
                                privateUse = privateUse,
                                notes = notes,
                                isDraft = true,
                            )
                        )
                    },
                    enabled = canSaveDraft,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (existingTrip?.isDraft == true) "Update draft"
                        else "Save as draft"
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
                                licensePlate = licensePlate,
                                startMileage = startMileage!!,
                                endMileage = endMileage!!,
                                privateUse = privateUse,
                                notes = notes,
                                isDraft = false,
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            !isEditing -> "Save trip"
                            existingTrip.isDraft -> "Complete trip"
                            else -> "Save changes"
                        }
                    )
                }
            }

            // ── Cancel ───────────────────────────────────────────────
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
 * A subtle rounded background grouping fields on a single horizontal
 * row. Used where the two fields are naturally paired side by side
 * (plate + chip, start + end date).
 */
@Composable
private fun HorizontalFieldGroup(
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * A subtle rounded background grouping fields stacked vertically.
 * Used where the two fields form a start/end pair that reads better
 * one above the other (postal codes, mileage). Takes a modifier so
 * two of these can sit side by side with equal weight.
 */
@Composable
private fun VerticalFieldGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionError(message: String) {
    Text(
        text = "⚠ $message",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/**
 * A read-only field showing a formatted date-time. Tapping opens the
 * date picker, then the time picker.
 *
 * In compact mode the format drops the year and uses a shorter
 * separator, so the value fits comfortably when two fields share a row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateUtcMillis by remember { mutableStateOf<Long?>(null) }

    val formatter = remember(compact) {
        SimpleDateFormat(
            if (compact) "d MMM · HH:mm" else "EEE, d MMM yyyy · HH:mm",
            Locale.getDefault(),
        )
    }
    val formatted = formatter.format(Date(value))

    Box(modifier = modifier) {
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