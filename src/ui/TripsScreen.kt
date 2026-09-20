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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebulousprime26.mileage_tracker.data.Trip
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Length of the highlight sweep. Kept short so it registers as feedback
// without becoming a wait the user notices.
private const val SWEEP_DURATION_MS = 220

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

    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var tripPendingDelete by remember { mutableStateOf<Trip?>(null) }

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
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = onStats) {
                        Text("Stats")
                    }
                },
            )
        },
        floatingActionButtonPosition =
            if (fabOnRight) FabPosition.End else FabPosition.Start,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTrip,
            ) {
                Text("Add trip")
            }
        },
    ) { padding ->
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No trips yet.\nTap \"Add trip\" to create one.",
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
                items(trips, key = { it.id }) { trip ->
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

    // ── Delete confirmation ──────────────────────────────────────────
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TripSummary(trip: Trip) {
    val formatter = remember { SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()) }
    val dateText = formatter.format(Date(trip.date))

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trip.startPostalCode,
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
                text = trip.endPostalCode,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${trip.distanceMileage} km",
                style = MaterialTheme.typography.bodyMedium,
            )
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