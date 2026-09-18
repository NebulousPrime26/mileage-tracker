package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.nebulousprime26.mileage_tracker.data.Trip

@Composable
fun LandingScreen(
    viewModel: TripViewModel = viewModel(),
    onContinue: () -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Mileage",
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (trips.isEmpty()) {
                Text(
                    text = "No trips yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(trips, key = { it.id }) { trip ->
                        TripRow(
                            trip = trip,
                            onDelete = { viewModel.deleteTrip(trip.id) },
                        )
                    }
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("Continue")
            }

            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text("Import")
            }

            OutlinedButton(
                onClick = onExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text("Export")
            }
        }
    }
}

@Composable
private fun TripRow(trip: Trip, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
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