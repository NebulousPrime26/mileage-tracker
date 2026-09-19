package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: TripViewModel,
    onBack: () -> Unit,
) {
    val stats by viewModel.yearlyStats.collectAsStateWithLifecycle()
    val filterStart by viewModel.filterStart.collectAsStateWithLifecycle()
    val filterEnd by viewModel.filterEnd.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val privateColor = MaterialTheme.colorScheme.primary
    val businessColor = MaterialTheme.colorScheme.tertiary
    val totalColor = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Filters ──────────────────────────────────────────────
            Text("Filter by date", style = MaterialTheme.typography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterDateField(
                    label = "From",
                    millis = filterStart,
                    modifier = Modifier.weight(1f),
                    onClick = { showStartPicker = true },
                )
                FilterDateField(
                    label = "To",
                    millis = filterEnd,
                    modifier = Modifier.weight(1f),
                    onClick = { showEndPicker = true },
                )
            }

            if (filterStart != null || filterEnd != null) {
                TextButton(onClick = { viewModel.clearFilters() }) {
                    Text("Clear filters")
                }
            }

            if (stats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No trips in the selected range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            // ── Legend ───────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Private", privateColor)
                LegendItem("Business", businessColor)
                LegendItem("Total", totalColor)
            }

            // ── Chart ────────────────────────────────────────────────
            Text("Mileage per year", style = MaterialTheme.typography.titleMedium)

            YearlyBarChart(
                stats = stats,
                privateColor = privateColor,
                businessColor = businessColor,
                totalColor = totalColor,
            )

            HorizontalDivider()

            // ── Table ────────────────────────────────────────────────
            Text("Breakdown", style = MaterialTheme.typography.titleMedium)

            StatsTable(stats = stats)
        }
    }

    // ── Date pickers for filters ─────────────────────────────────────
    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterStart?.let { toUtcDateMillis(it) }
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            viewModel.setFilterStart(startOfDayLocal(it))
                        }
                        showStartPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterEnd?.let { toUtcDateMillis(it) }
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            viewModel.setFilterEnd(endOfDayLocal(it))
                        }
                        showEndPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ── Building blocks ──────────────────────────────────────────────────

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
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun YearlyBarChart(
    stats: List<YearlyStats>,
    privateColor: Color,
    businessColor: Color,
    totalColor: Color,
) {
    val maxValue = stats.maxOfOrNull { it.totalMileage }?.coerceAtLeast(1.0) ?: 1.0
    val chartHeight = 180.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        stats.forEach { year ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Bar(year.privateMileage, maxValue, chartHeight, privateColor)
                    Bar(year.businessMileage, maxValue, chartHeight, businessColor)
                    Bar(year.totalMileage, maxValue, chartHeight, totalColor)
                }
                Spacer(Modifier.height(4.dp))
                Text("${year.year}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Bar(
    value: Double,
    max: Double,
    maxHeight: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    val fraction = (value / max).toFloat().coerceIn(0f, 1f)
    val height = maxHeight * fraction
    Box(
        modifier = Modifier
            .width(14.dp)
            .height(height.coerceAtLeast(1.dp))
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
    )
}

@Composable
private fun StatsTable(stats: List<YearlyStats>) {
    val formatter = remember { java.text.DecimalFormat("#,##0.0") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            TableCell("Year", weight = 1f, header = true, align = TextAlign.Start)
            TableCell("Private", weight = 1.4f, header = true, align = TextAlign.End)
            TableCell("Business", weight = 1.4f, header = true, align = TextAlign.End)
            TableCell("Total", weight = 1.4f, header = true, align = TextAlign.End)
        }
        HorizontalDivider()

        stats.forEach { year ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                TableCell("${year.year}", weight = 1f, align = TextAlign.Start)
                TableCell(formatter.format(year.privateMileage), weight = 1.4f, align = TextAlign.End)
                TableCell(formatter.format(year.businessMileage), weight = 1.4f, align = TextAlign.End)
                TableCell(formatter.format(year.totalMileage), weight = 1.4f, align = TextAlign.End)
            }
            HorizontalDivider()
        }

        // Footer totals
        val totalPrivate = stats.sumOf { it.privateMileage }
        val totalBusiness = stats.sumOf { it.businessMileage }
        val totalAll = stats.sumOf { it.totalMileage }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            TableCell("All", weight = 1f, header = true, align = TextAlign.Start)
            TableCell(formatter.format(totalPrivate), weight = 1.4f, header = true, align = TextAlign.End)
            TableCell(formatter.format(totalBusiness), weight = 1.4f, header = true, align = TextAlign.End)
            TableCell(formatter.format(totalAll), weight = 1.4f, header = true, align = TextAlign.End)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = align,
        style = if (header) MaterialTheme.typography.labelLarge
        else MaterialTheme.typography.bodyMedium,
    )
}

// ── Date helpers ─────────────────────────────────────────────────────

/** Converts a local epoch-millis value to the UTC midnight the picker expects. */
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

/** UTC date from the picker → local midnight (00:00:00.000) of that day. */
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

/** UTC date from the picker → local end of day (23:59:59.999) of that day. */
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