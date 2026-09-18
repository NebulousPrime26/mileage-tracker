package com.nebulousprime26.mileage_tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LandingScreen(
    onContinue: () -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Mileage",
                style = MaterialTheme.typography.headlineLarge,
            )

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            ) {
                Text("Continue")
            }

            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("Import")
            }

            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("Export")
            }
        }
    }
}