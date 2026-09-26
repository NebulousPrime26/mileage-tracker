package com.nebulousprime26.mileage_tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.FilterChip
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

// ── Chart palette ────────────────────────────────────────────────────
private val PrivateLight = Color(0xFF1976D2)
private val PrivateDark  = Color(0xFF64B5F6)
private val BusinessLight = Color(0xFFF57C00)
private val BusinessDark  = Color(0xFFFFB74D)
private val TotalLight = Color(0xFF424242)
private val TotalDark  = Color(0xFFE0E0E0)

private const val MILLIS_PER_HOUR = 3_600_000.0

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
    var metric by remember { mutableStateOf(StatsMetric.MILEAGE) }

    var cumulativeMileage by remember { mutableStateOf(false) }
    var averageDuration by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val privateColor = if (isDark) PrivateDark else PrivateLight
    val businessColor = if (isDark) BusinessDark else BusinessLight
    val totalColor = if (isDark) TotalDark else TotalLight

    val chartData = remember(monthlyStats, cumulativeMileage, metric) {
        if (metric == StatsMetric.MILEAGE && cumulativeMileage) {
            var runningPrivate = 0.0
            var runningBusiness = 0.0
            monthlyStats.map { m ->
                runningPrivate += m.privateMileage
                runningBusiness += m.businessMileage
                m.copy(
                    privateMileage = runningPrivate,
                    businessMileage = runningBusiness,
                )
            }
        } else {
            monthlyStats
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
            // ── Metric selector ──────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = metric == StatsMetric.MILEAGE,
                    onClick = { metric = StatsMetric.MILEAGE },
                    label = { Text("Mileage") },
                )
                FilterChip(
                    selected = metric == StatsMetric.DURATION,
                    onClick = { metric = StatsMetric.DURATION },
                    label = { Text("Travel time") },
                )
            }

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

            // ── Chart header + metric-specific toggle ────────────────
            val isAverageMode = metric == StatsMetric.DURATION && averageDuration
            ChartHeader(
                title = if (metric == StatsMetric.MILEAGE) "Mileage" else "Travel Time",
                leftLabel = if (metric == StatsMetric.MILEAGE) "Incremental" else "Sum",
                rightLabel = if (metric == StatsMetric.MILEAGE) "Cumulative" else "Average",
                isRightSelected = if (metric == StatsMetric.MILEAGE) cumulativeMileage else averageDuration,
                onToggle = {
                    if (metric == StatsMetric.MILEAGE) cumulativeMileage = it
                    else averageDuration = it
                },
            )

            // ── Line chart ───────────────────────────────────────────
            MonthlyLineChart(
                stats = chartData,
                metric = metric,
                showAverage = isAverageMode,
                privateColor = privateColor,
                businessColor = businessColor,
                totalColor = totalColor,
            )

            // ── Legend (below the chart) ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem("Private", privateColor)
                    LegendItem("Business", businessColor)
                    LegendItem("Total", totalColor)
                }
            }

            HorizontalDivider()

            // ── Table ────────────────────────────────────────────────
            Text("Yearly breakdown", style = MaterialTheme.typography.titleMedium)

            StatsTable(
                stats = yearlyStats,
                metric = metric,
                showAverage = isAverageMode,
            )
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

/**
 * The chart title on the left and a "Label [switch] Label" toggle
 * cluster on the right, mirroring the PositionSetting rows on the
 * settings screen. The active label is drawn in the primary colour
 * so the current mode reads at a glance.
 */
@Composable
private fun ChartHeader(
    title: String,
    leftLabel: String,
    rightLabel: String,
    isRightSelected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = leftLabel,
            style = MaterialTheme.typography.labelLarge,
            color = if (isRightSelected) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Switch(
            checked = isRightSelected,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(
            text = rightLabel,
            style = MaterialTheme.typography.labelLarge,
            color = if (isRightSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

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
    metric: StatsMetric,
    showAverage: Boolean,
    privateColor: Color,
    businessColor: Color,
    totalColor: Color,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val privateValues = stats.map { m ->
        when {
            metric == StatsMetric.MILEAGE -> m.privateMileage
            showAverage -> m.privateAverageDurationMillis / MILLIS_PER_HOUR
            else -> m.privateDurationMillis / MILLIS_PER_HOUR
        }
    }
    val businessValues = stats.map { m ->
        when {
            metric == StatsMetric.MILEAGE -> m.businessMileage
            showAverage -> m.businessAverageDurationMillis / MILLIS_PER_HOUR
            else -> m.businessDurationMillis / MILLIS_PER_HOUR
        }
    }
    val totalValues = stats.map { m ->
        when {
            metric == StatsMetric.MILEAGE -> m.totalMileage
            showAverage -> m.totalAverageDurationMillis / MILLIS_PER_HOUR
            else -> m.totalDurationMillis / MILLIS_PER_HOUR
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        if (stats.isEmpty()) return@Canvas

        val leftPad = 52.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 40.dp.toPx()

        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        val maxValue = totalValues.maxOrNull()?.coerceAtLeast(1e-6) ?: 1.0

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
            val axisLabel = if (metric == StatsMetric.MILEAGE) {
                formatMileageAxis(maxValue * fraction)
            } else {
                formatHoursAxis(maxValue * fraction)
            }
            val layout = textMeasurer.measure(
                text = axisLabel,
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

        // ── X-axis labels ────────────────────────────────────────
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
        fun drawSeries(values: List<Double>, color: Color, strokeWidth: Float) {
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
            drawPath(path, color = color, style = Stroke(width = strokeWidth))

            values.forEachIndexed { i, v ->
                drawCircle(
                    color = color,
                    radius = 2.5.dp.toPx(),
                    center = Offset(xFor(i), yFor(v)),
                )
            }
        }

        drawSeries(totalValues, totalColor, 3.dp.toPx())
        drawSeries(privateValues, privateColor, 2.dp.toPx())
        drawSeries(businessValues, businessColor, 2.dp.toPx())
    }
}

/** Compact label for the mileage y-axis: 0, 500, 1k, 2k, … */
private fun formatMileageAxis(value: Double): String = when {
    value <= 0.0 -> "0"
    value >= 1000 -> "${(value / 1000).toInt()}k"
    value >= 100 -> value.toInt().toString()
    else -> String.format(Locale.ROOT, "%.0f", value)
}

/** Compact label for the hours y-axis: 0, 5, 10, 100, … */
private fun formatHoursAxis(value: Double): String = when {
    value <= 0.0 -> "0"
    value >= 100 -> "${value.toInt()}h"
    value >= 10 -> String.format(Locale.ROOT, "%.0f h", value)
    else -> String.format(Locale.ROOT, "%.1f h", value)
}

/** Formats a duration as hours with one decimal. */
private fun formatHours(millis: Long): String =
    String.format(Locale.ROOT, "%.1f h", millis / MILLIS_PER_HOUR)

@Composable
private fun StatsTable(
    stats: List<YearlyStats>,
    metric: StatsMetric,
    showAverage: Boolean,
) {
    val formatter = remember { java.text.DecimalFormat("#,##0.0") }

    val privateText: (YearlyStats) -> String = when {
        metric == StatsMetric.MILEAGE -> { y -> formatter.format(y.privateMileage) }
        showAverage -> { y -> formatHours(y.privateAverageDurationMillis) }
        else -> { y -> formatHours(y.privateDurationMillis) }
    }
    val businessText: (YearlyStats) -> String = when {
        metric == StatsMetric.MILEAGE -> { y -> formatter.format(y.businessMileage) }
        showAverage -> { y -> formatHours(y.businessAverageDurationMillis) }
        else -> { y -> formatHours(y.businessDurationMillis) }
    }
    val totalText: (YearlyStats) -> String = when {
        metric == StatsMetric.MILEAGE -> { y -> formatter.format(y.totalMileage) }
        showAverage -> { y -> formatHours(y.totalAverageDurationMillis) }
        else -> { y -> formatHours(y.totalDurationMillis) }
    }

    val totalPrivateMileage = stats.sumOf { it.privateMileage }
    val totalBusinessMileage = stats.sumOf { it.businessMileage }
    val totalAllMileage = stats.sumOf { it.totalMileage }

    val totalPrivateDuration = stats.sumOf { it.privateDurationMillis }
    val totalBusinessDuration = stats.sumOf { it.businessDurationMillis }
    val totalAllDuration = stats.sumOf { it.totalDurationMillis }

    val totalPrivateTrips = stats.sumOf { it.privateTripCount }
    val totalBusinessTrips = stats.sumOf { it.businessTripCount }
    val totalAllTrips = totalPrivateTrips + totalBusinessTrips

    val footerPrivate: String = when {
        metric == StatsMetric.MILEAGE -> formatter.format(totalPrivateMileage)
        showAverage -> if (totalPrivateTrips > 0)
            formatHours(totalPrivateDuration / totalPrivateTrips) else "—"
        else -> formatHours(totalPrivateDuration)
    }
    val footerBusiness: String = when {
        metric == StatsMetric.MILEAGE -> formatter.format(totalBusinessMileage)
        showAverage -> if (totalBusinessTrips > 0)
            formatHours(totalBusinessDuration / totalBusinessTrips) else "—"
        else -> formatHours(totalBusinessDuration)
    }
    val footerTotal: String = when {
        metric == StatsMetric.MILEAGE -> formatter.format(totalAllMileage)
        showAverage -> if (totalAllTrips > 0)
            formatHours(totalAllDuration / totalAllTrips) else "—"
        else -> formatHours(totalAllDuration)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                TableCell(privateText(year), weight = 1.4f, align = TextAlign.End)
                TableCell(businessText(year), weight = 1.4f, align = TextAlign.End)
                TableCell(totalText(year), weight = 1.4f, align = TextAlign.End)
            }
            HorizontalDivider()
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            TableCell("All", weight = 1f, header = true, align = TextAlign.Start)
            TableCell(footerPrivate, weight = 1.4f, header = true, align = TextAlign.End)
            TableCell(footerBusiness, weight = 1.4f, header = true, align = TextAlign.End)
            TableCell(footerTotal, weight = 1.4f, header = true, align = TextAlign.End)
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