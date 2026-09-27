package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.sp
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
 * Duration of the filter sheet's enter and exit animations. A single
 * value is used for both so they feel symmetric.
 */
private const val FILTER_SHEET_ANIM_MS = 350

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

    // Outer Box so the filter sheet overlay can draw on top of the
    // whole screen, including the top bar and FABs.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trips") },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Back") }
                    },
                    actions = {
                        TextButton(onClick = onStats) { Text("Stats") }
                    },
                )
            },
            floatingActionButtonPosition =
                if (fabOnRight) FabPosition.End else FabPosition.Start,
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (fabOnRight) {
                        FilterFab(
                            isActive = filterSpec.isActive,
                            activeCount = filterSpec.activeCount,
                            onClick = { showFilterSheet = true },
                        )
                        ExtendedFloatingActionButton(onClick = onAddTrip) {
                            Text("Add trip")
                        }
                    } else {
                        ExtendedFloatingActionButton(onClick = onAddTrip) {
                            Text("Add trip")
                        }
                        FilterFab(
                            isActive = filterSpec.isActive,
                            activeCount = filterSpec.activeCount,
                            onClick = { showFilterSheet = true },
                        )
                    }
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

        // Filter sheet overlay sits above the entire Scaffold.
        FilterSheetOverlay(
            visible = showFilterSheet,
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
 * The filter FAB. Uses the same ExtendedFAB shape as Add trip so the
 * pair reads as a matched set, but with a subdued container when
 * inactive and a tinted container when a filter is applied.
 */
@Composable
private fun FilterFab(
    isActive: Boolean,
    activeCount: Int,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (isActive) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Text(
            text = if (isActive) "Filter ($activeCount)" else "Filter",
        )
    }
}

// ── Filter sheet overlay ─────────────────────────────────────────────

/**
 * A custom bottom-sheet overlay. Unlike Material 3's `ModalBottomSheet`,
 * its enter and exit animations are both driven by the same `tween`, so
 * the slide-down takes exactly as long as the slide-up.
 *
 * The overlay is always composed; visibility is controlled by
 * [visible], and `AnimatedVisibility` handles the transition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetOverlay(
    visible: Boolean,
    initial: TripFilterSpec,
    onApply: (TripFilterSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    // The sheet stays in the tree while hidden, so the working spec has
    // to be reset each time the sheet becomes visible — otherwise the
    // user would see their previous edits rather than the applied filter.
    var spec by remember { mutableStateOf(initial) }
    LaunchedEffect(visible) {
        if (visible) spec = initial
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Scrim ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(FILTER_SHEET_ANIM_MS)),
            exit = fadeOut(tween(FILTER_SHEET_ANIM_MS)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        // ── Sheet ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = FILTER_SHEET_ANIM_MS,
                    easing = FastOutSlowInEasing,
                ),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = FILTER_SHEET_ANIM_MS,
                    easing = FastOutSlowInEasing,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Drag handle, purely decorative since the sheet
                    // isn't draggable in this implementation.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp),
                            ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Filter trips",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { spec = TripFilterSpec() },
                            enabled = spec.isActive,
                        ) {
                            Text("Reset")
                        }
                    }

                    FilterSection(label = "Dates") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateField(
                                label = "From",
                                millis = spec.startDateFrom,
                                modifier = Modifier.weight(1f),
                                onClick = { showStartDatePicker = true },
                            )
                            DateField(
                                label = "To",
                                millis = spec.startDateTo,
                                modifier = Modifier.weight(1f),
                                onClick = { showEndDatePicker = true },
                            )
                        }
                    }

                    FilterSection(label = "Route") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactField(
                                value = spec.startPostalContains,
                                onValueChange = {
                                    spec = spec.copy(startPostalContains = it)
                                },
                                label = "Start postal",
                                modifier = Modifier.weight(1f),
                            )
                            CompactField(
                                value = spec.endPostalContains,
                                onValueChange = {
                                    spec = spec.copy(endPostalContains = it)
                                },
                                label = "End postal",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    FilterSection(label = "Vehicle & distance") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactField(
                                value = spec.licensePlateContains,
                                onValueChange = {
                                    spec = spec.copy(licensePlateContains = it)
                                },
                                label = "Plate",
                                modifier = Modifier.weight(1.2f),
                            )
                            CompactNumberField(
                                value = spec.minDistanceKm,
                                onValueChange = {
                                    spec = spec.copy(minDistanceKm = it)
                                },
                                label = "Min km",
                                modifier = Modifier.weight(1f),
                            )
                            CompactNumberField(
                                value = spec.maxDistanceKm,
                                onValueChange = {
                                    spec = spec.copy(maxDistanceKm = it)
                                },
                                label = "Max km",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    FilterSection(label = "Category") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                CategoryChip("Both", spec.privateOnly == null) {
                                    spec = spec.copy(privateOnly = null)
                                }
                                CategoryChip("Private", spec.privateOnly == true) {
                                    spec = spec.copy(privateOnly = true)
                                }
                                CategoryChip("Business", spec.privateOnly == false) {
                                    spec = spec.copy(privateOnly = false)
                                }
                            }
                            Text(
                                text = "Drafts",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Switch(
                                checked = spec.includeDrafts,
                                onCheckedChange = {
                                    spec = spec.copy(includeDrafts = it)
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onApply(spec) },
                            modifier = Modifier.weight(2f),
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }

    // ── Date pickers ─────────────────────────────────────────────
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

// ── Filter sheet inputs ──────────────────────────────────────────────

@Composable
private fun FilterSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun DateField(
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
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
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
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
        ),
        modifier = modifier,
    )
}

@Composable
private fun CompactNumberField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { text -> onValueChange(text.toDoubleOrNull()) },
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

// ── Delete dialog ────────────────────────────────────────────────────

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