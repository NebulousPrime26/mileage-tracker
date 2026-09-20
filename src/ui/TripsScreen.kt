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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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

    // The trip currently playing its "selected" sweep. Non-null means a
    // sweep is running and navigation is pending.
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }

    // Wait for the sweep, then navigate. The delay matches the animation
    // duration so they finish together.
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
                            // Ignore taps while a sweep is already running.
                            if (selectedTrip == null) selectedTrip = trip
                        },
                        onDelete = { viewModel.deleteTrip(trip.id) },
                    )
                }
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
    val formatter = remember { SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()) }
    val dateText = formatter.format(Date(trip.date))

    // 0 = sweep hasn't started, 1 = sweep fully crossed the card.
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

                // The light sweep. Only draws while this row is selected.
                val progress = sweep.value
                if (progress > 0f) {
                    val bandWidth = size.width * 0.6f
                    val centerX =
                        -bandWidth + (size.width + 2f * bandWidth) * progress

                    // A soft white band fading in and out at its edges.
                    // White reads as a highlight in both light and dark
                    // themes, unlike a tinted color which would fight the
                    // card's surface color.
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${trip.startPostalCode} → ${trip.endPostalCode}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "${trip.distanceMileage} km",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (trip.privateUse) {
                    Text(
                        text = "Private use",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}