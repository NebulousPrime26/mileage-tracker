package com.nebulousprime26.mileage_tracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.nebulousprime26.mileage_tracker.ui.LandingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LandingScreen(
                    onContinue = {Toast.makeText(this, "Continue", Toast.LENGTH_SHORT).show()},
                    onImport = {Toast.makeText(this, "Import", Toast.LENGTH_SHORT).show()},
                    onExport = {Toast.makeText(this, "Export", Toast.LENGTH_SHORT).show()},
                )
            }
        }
    }
}