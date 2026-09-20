package com.nebulousprime26.mileage_tracker.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebulousprime26.mileage_tracker.data.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        onClick = { onEditTrip(trip) },
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()) }
    val dateText = formatter.format(Date(trip.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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