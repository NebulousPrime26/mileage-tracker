package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebulousprime26.mileage_tracker.data.Trip
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val SWEEP_DURATION_MS = 220

/**
 * All the criteria a trip can be filtered on. Every field is optional —
 * null or blank means "no constraint on this field".
 */
private data class TripFilterSpec(
    val startDateFrom: Long? = null,
    val startDateTo: Long? = null,
    val startPostalContains: String = "",
    val endPostalContains: String = "",
    val licensePlateContains: String = "",
    val minDistanceKm: Double? = null,
    val maxDistanceKm: Double? = null,
    val privateOnly: Boolean? = null,   // null = both, true = private, false = business
    val includeDrafts: Boolean = false,
) {
    /** True when at least one criterion is set. */
    val isActive: Boolean
        get() = startDateFrom != null ||
            startDateTo != null ||
            startPostalContains.isNotBlank() ||
            endPostalContains.isNotBlank() ||
            licensePlateContains.isNotBlank() ||
            minDistanceKm != null ||
            maxDistanceKm != null ||
            privateOnly != null ||
            includeDrafts

    /** Number of individual criteria set, for the badge on the filter button. */
    val activeCount: Int
        get() = listOf(
            startDateFrom != null,
            startDateTo != null,
            startPostalContains.isNotBlank(),
            endPostalContains.isNotBlank(),
            licensePlateContains.isNotBlank(),
            minDistanceKm != null,
            maxDistanceKm != null,
            privateOnly != null,
            includeDrafts,
        ).count { it }
}

/** Applies the spec to a list of trips. */
private fun List<Trip>.applyFilter(spec: TripFilterSpec): List<Trip> = filter { trip ->
    (spec.includeDrafts || !trip.isDraft) &&
        (spec.startDateFrom == null || trip.startDate >= spec.startDateFrom) &&
        (spec.startDateTo == null || trip.startDate <= spec.startDateTo) &&
        (spec.startPostalContains.isBlank() ||
            trip.startPostalCode.contains(spec.startPostalContains, ignoreCase = true)) &&
        (spec.endPostalContains.isBlank() ||
            trip.endPostalCode.contains(spec.endPostalContains, ignoreCase = true)) &&
        (spec.licensePlateContains.isBlank() ||
            trip.licensePlate.contains(spec.licensePlateContains, ignoreCase = true)) &&
        (spec.minDistanceKm == null || trip.distanceMileage >= spec.minDistanceKm) &&
        (spec.maxDistanceKm == null || trip.distanceMileage <= spec.maxDistanceKm) &&
        (spec.privateOnly == null || trip.privateUse == spec.privateOnly)
}

private fun formatDateRange(start: Long, end: Long): String {
    val dayFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    val startCal = Calendar.getInstance().apply { timeInMillis = start }
    val endCal = Calendar.getInstance().apply { timeInMillis = end }

    val sameDay = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
        startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)

    return if (sameDay) {
        "${dayFmt.format(Date(start))} · " +
            "${timeFmt.format(Date(start))} – ${timeFmt.format(Date(end))}"
    } else {
        "${dayFmt.format(Date(start))} ${timeFmt.format(Date(start))} – " +
            "${dayFmt.format(Date(end))} ${timeFmt.format(Date(end))}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: TripViewModel,
    onAddTrip: () -> Unit,
    onEditTrip: (Trip) -> Unit,
    onStats: () -> Unit,
    onBack: () -> Unit,
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val fabOnRight by viewModel.fabOnRight.collectAsStateWithLifecycle()

    var filterSpec by remember { mutableStateOf(TripFilterSpec()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var tripPendingDelete by remember { mutableStateOf<Trip?>(null) }

    val filteredTrips = remember(trips, filterSpec) {
        trips.applyFilter(filterSpec)
    }

    LaunchedEffect(selectedTrip) {
        val trip = selectedTrip ?: return@LaunchedEffect
        delay(SWEEP_DURATION_MS.toLong())
        onEditTrip(trip)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trips") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { showFilterSheet = true }) {
                        Text(
                            text = if (filterSpec.isActive) {
                                "Filter (${filterSpec.activeCount})"
                            } else {
                                "Filter"
                            },
                        )
                    }
                    TextButton(onClick = onStats) { Text("Stats") }
                },
            )
        },
        floatingActionButtonPosition =
            if (fabOnRight) FabPosition.End else FabPosition.Start,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTrip) {
                Text("Add trip")
            }
        },
    ) { padding ->
        if (filteredTrips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        trips.isEmpty() ->
                            "No trips yet.\nTap \"Add trip\" to create one."
                        filterSpec.isActive ->
                            "No trips match the current filter."
                        else ->
                            "No trips."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredTrips, key = { it.id }) { trip ->
                    TripRow(
                        trip = trip,
                        isSelected = selectedTrip?.id == trip.id,
                        onClick = {
                            if (selectedTrip == null) selectedTrip = trip
                        },
                        onDelete = { tripPendingDelete = trip },
                    )
                }
            }
        }
    }

    // ── Filter sheet ─────────────────────────────────────────────────
    if (showFilterSheet) {
        FilterSheet(
            initial = filterSpec,
            onApply = {
                filterSpec = it
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }

    tripPendingDelete?.let { trip ->
        DeleteConfirmDialog(
            trip = trip,
            onConfirm = {
                viewModel.deleteTrip(trip.id)
                tripPendingDelete = null
            },
            onDismiss = { tripPendingDelete = null },
        )
    }
}

/**
 * Bottom sheet holding all filter criteria. Changes are staged locally
 * and only applied when the user taps Apply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    initial: TripFilterSpec,
    onApply: (TripFilterSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var spec by remember { mutableStateOf(initial) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Filter trips",
                style = MaterialTheme.typography.headlineSmall,
            )

            // ── Date range ──────────────────────────────────────────
            Text("Date range", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterDateField(
                    label = "From",
                    millis = spec.startDateFrom,
                    modifier = Modifier.weight(1f),
                    onClick = { showStartDatePicker = true },
                )
                FilterDateField(
                    label = "To",
                    millis = spec.startDateTo,
                    modifier = Modifier.weight(1f),
                    onClick = { showEndDatePicker = true },
                )
            }

            if (spec.startDateFrom != null || spec.startDateTo != null) {
                TextButton(onClick = {
                    spec = spec.copy(startDateFrom = null, startDateTo = null)
                }) {
                    Text("Clear dates")
                }
            }

            HorizontalDivider()

            // ── Postal codes ────────────────────────────────────────
            Text("Postal codes", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = spec.startPostalContains,
                onValueChange = { spec = spec.copy(startPostalContains = it) },
                label = { Text("Start postal contains") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = spec.endPostalContains,
                onValueChange = { spec = spec.copy(endPostalContains = it) },
                label = { Text("End postal contains") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // ── License plate ───────────────────────────────────────
            Text("Vehicle", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = spec.licensePlateContains,
                onValueChange = { spec = spec.copy(licensePlateContains = it) },
                label = { Text("License plate contains") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // ── Mileage range ───────────────────────────────────────
            Text("Distance (km)", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = spec.minDistanceKm?.toString() ?: "",
                    onValueChange = { text ->
                        spec = spec.copy(minDistanceKm = text.toDoubleOrNull())
                    },
                    label = { Text("Min") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = spec.maxDistanceKm?.toString() ?: "",
                    onValueChange = { text ->
                        spec = spec.copy(maxDistanceKm = text.toDoubleOrNull())
                    },
                    label = { Text("Max") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            // ── Category ────────────────────────────────────────────
            Text("Category", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChoiceChip(
                    label = "Both",
                    selected = spec.privateOnly == null,
                    onClick = { spec = spec.copy(privateOnly = null) },
                )
                FilterChoiceChip(
                    label = "Private",
                    selected = spec.privateOnly == true,
                    onClick = { spec = spec.copy(privateOnly = true) },
                )
                FilterChoiceChip(
                    label = "Business",
                    selected = spec.privateOnly == false,
                    onClick = { spec = spec.copy(privateOnly = false) },
                )
            }

            HorizontalDivider()

            // ── Drafts ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Include drafts",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = spec.includeDrafts,
                    onCheckedChange = { spec = spec.copy(includeDrafts = it) },
                )
            }

            // ── Actions ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { spec = TripFilterSpec() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = { onApply(spec) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Apply")
                }
            }
        }
    }

    // ── Date pickers ─────────────────────────────────────────────────
    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = spec.startDateFrom?.let { toUtcDateMillis(it) }
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        spec = spec.copy(startDateFrom = startOfDayLocal(it))
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = spec.startDateTo?.let { toUtcDateMillis(it) }
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        spec = spec.copy(startDateTo = endOfDayLocal(it))
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** A small chip used for the mutually-exclusive category choice. */
@Composable
private fun FilterChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

/** A read-only date field that opens a picker when tapped. */
@Composable
private fun FilterDateField(
    label: String,
    millis: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val text = millis?.let { formatter.format(Date(it)) } ?: ""

    Box(modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Any") },
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

@Composable
private fun DeleteConfirmDialog(
    trip: Trip,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete trip?") },
        text = {
            Column {
                TripSummary(trip = trip)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TripSummary(trip: Trip) {
    val dateRangeText = remember(trip.startDate, trip.endDate) {
        formatDateRange(trip.startDate, trip.endDate)
    }

    val startText = trip.startPostalCode.ifBlank { "—" }
    val endText = trip.endPostalCode.ifBlank { "—" }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = startText,
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(18.dp),
            )
            Text(
                text = endText,
                style = MaterialTheme.typography.titleMedium,
            )

            if (trip.isDraft) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Draft",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Text(
            text = dateRangeText,
            style = MaterialTheme.typography.bodySmall,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${trip.distanceMileage} km",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (trip.licensePlate.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trip.licensePlate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trip.privateUse) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Private",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TripRow(
    trip: Trip,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val sweep = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            sweep.snapTo(0f)
            sweep.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SWEEP_DURATION_MS,
                    easing = LinearEasing,
                ),
            )
        } else {
            sweep.snapTo(0f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawWithContent {
                drawContent()

                val progress = sweep.value
                if (progress > 0f) {
                    val bandWidth = size.width * 0.6f
                    val centerX =
                        -bandWidth + (size.width + 2f * bandWidth) * progress

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                            startX = centerX - bandWidth / 2f,
                            endX = centerX + bandWidth / 2f,
                        ),
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TripSummary(trip = trip)
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

// ── Date helpers ─────────────────────────────────────────────────────

private fun toUtcDateMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
        )
    }.timeInMillis
}

private fun startOfDayLocal(utcDateMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun endOfDayLocal(utcDateMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}