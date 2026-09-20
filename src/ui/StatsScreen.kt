package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val yearlyStats by viewModel.yearlyStats.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val filterStart by viewModel.filterStart.collectAsStateWithLifecycle()
    val filterEnd by viewModel.filterEnd.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var cumulative by remember { mutableStateOf(false) }

    val privateColor = MaterialTheme.colorScheme.primary
    val businessColor = MaterialTheme.colorScheme.tertiary
    val totalColor = MaterialTheme.colorScheme.secondary

    // When cumulative is on, replace each month's value with the running total
    // up to and including that month. Accumulation happens after filtering, so
    // changing the date range resets the running total from the new start.
    val chartData = remember(monthlyStats, cumulative) {
        if (!cumulative) {
            monthlyStats
        } else {
            var runningPrivate = 0.0
            var runningBusiness = 0.0
            monthlyStats.map { m ->
                runningPrivate += m.privateMileage
                runningBusiness += m.businessMileage
                MonthlyStats(
                    year = m.year,
                    month = m.month,
                    privateMileage = runningPrivate,
                    businessMileage = runningBusiness,
                )
            }
        }
    }

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

            if (monthlyStats.isEmpty()) {
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

            // ── Chart header + cumulative toggle ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (cumulative) "Cumulative mileage" else "Mileage over time",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cumulative",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = cumulative,
                        onCheckedChange = { cumulative = it },
                    )
                }
            }

            MonthlyLineChart(
                stats = chartData,
                privateColor = privateColor,
                businessColor = businessColor,
                totalColor = totalColor,
            )

            HorizontalDivider()

            // ── Table ────────────────────────────────────────────────
            Text("Yearly breakdown", style = MaterialTheme.typography.titleMedium)

            StatsTable(stats = yearlyStats)
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
private fun MonthlyLineChart(
    stats: List<MonthlyStats>,
    privateColor: Color,
    businessColor: Color,
    totalColor: Color,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        if (stats.isEmpty()) return@Canvas

        val leftPad = 48.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 40.dp.toPx()

        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        val maxValue = stats.maxOfOrNull { it.totalMileage }
            ?.coerceAtLeast(1.0) ?: 1.0

        // ── Gridlines + Y-axis labels ────────────────────────────
        val gridLines = 4
        for (i in 0..gridLines) {
            val fraction = i.toFloat() / gridLines
            val y = topPad + chartHeight * (1 - fraction)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
            val layout = textMeasurer.measure(
                text = formatAxisValue(maxValue * fraction),
                style = TextStyle(fontSize = 10.sp, color = labelColor),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    leftPad - layout.size.width - 6.dp.toPx(),
                    y - layout.size.height / 2f,
                ),
            )
        }

        // ── Coordinate helpers ───────────────────────────────────
        val n = stats.size
        val stepX = if (n > 1) chartWidth / (n - 1) else 0f

        fun xFor(i: Int): Float = leftPad + stepX * i
        fun yFor(value: Double): Float =
            topPad + chartHeight *
                (1f - (value / maxValue).toFloat().coerceIn(0f, 1f))

        // ── X-axis labels (skip some if crowded) ─────────────────
        val maxLabels = 6
        val labelStep = maxOf(1, (n + maxLabels - 1) / maxLabels)
        stats.forEachIndexed { i, month ->
            if (i % labelStep == 0 || i == n - 1) {
                val layout = textMeasurer.measure(
                    text = month.label,
                    style = TextStyle(fontSize = 10.sp, color = labelColor),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        xFor(i) - layout.size.width / 2f,
                        topPad + chartHeight + 8.dp.toPx(),
                    ),
                )
            }
        }

        // ── Series ───────────────────────────────────────────────
        fun drawSeries(values: List<Double>, color: Color) {
            if (values.isEmpty()) return

            if (values.size == 1) {
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = Offset(xFor(0), yFor(values[0])),
                )
                return
            }

            val path = Path()
            values.forEachIndexed { i, v ->
                val x = xFor(i)
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))

            values.forEachIndexed { i, v ->
                drawCircle(
                    color = color,
                    radius = 2.5.dp.toPx(),
                    center = Offset(xFor(i), yFor(v)),
                )
            }
        }

        // Draw total first so private/business overlay it.
        drawSeries(stats.map { it.totalMileage }, totalColor)
        drawSeries(stats.map { it.privateMileage }, privateColor)
        drawSeries(stats.map { it.businessMileage }, businessColor)
    }
}

/** Compact label for the y-axis: 0, 500, 1k, 2k, … */
private fun formatAxisValue(value: Double): String = when {
    value <= 0.0 -> "0"
    value >= 1000 -> "${(value / 1000).toInt()}k"
    value >= 100 -> value.toInt().toString()
    else -> String.format(Locale.ROOT, "%.0f", value)
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