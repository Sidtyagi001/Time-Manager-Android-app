package com.example.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.time.DayTimeReport
import com.example.domain.time.TimeAccountingEngine
import com.example.ui.MainViewModel
import com.example.domain.time.TimeFormatUtils
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily, 1: Weekly, 2: Monthly
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    var dailyReport by remember { mutableStateOf<DayTimeReport?>(null) }
    var periodReports by remember { mutableStateOf<List<DayTimeReport>>(emptyList()) }

    val allSessions by viewModel.allSessions.collectAsState()
    val allTasks by viewModel.selectedDateTasks.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val is24Hour = userSettings?.is24HourFormat ?: true

    // Refresh report on date or tab change
    LaunchedEffect(selectedTab, currentDate, allSessions) {
        val now = System.currentTimeMillis()
        when (selectedTab) {
            0 -> {
                val dateStr = TimeAccountingEngine.formatDate(currentDate)
                dailyReport = viewModel.getDayReport(dateStr)
                periodReports = emptyList()
            }
            1 -> {
                val weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weekEnd = weekStart.plusDays(6)
                periodReports = viewModel.getMultiDayReports(weekStart, weekEnd)
                dailyReport = null
            }
            2 -> {
                val monthStart = currentDate.with(TemporalAdjusters.firstDayOfMonth())
                val monthEnd = currentDate.with(TemporalAdjusters.lastDayOfMonth())
                periodReports = viewModel.getMultiDayReports(monthStart, monthEnd)
                dailyReport = null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab selector
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Daily") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Weekly") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Monthly") })
            }
        }

        // Navigation header
        item {
            PeriodNavHeader(
                selectedTab = selectedTab,
                currentDate = currentDate,
                onPrevious = {
                    currentDate = when (selectedTab) {
                        0 -> currentDate.minusDays(1)
                        1 -> currentDate.minusWeeks(1)
                        else -> currentDate.minusMonths(1)
                    }
                },
                onNext = {
                    currentDate = when (selectedTab) {
                        0 -> currentDate.plusDays(1)
                        1 -> currentDate.plusWeeks(1)
                        else -> currentDate.plusMonths(1)
                    }
                }
            )
        }

        // Summary Metric Cards
        item {
            val totalTracked: Long
            val totalUntracked: Long
            val totalBreak: Long
            val totalPlanned: Long
            val totalActualLinked: Long
            val avgCompleteness: Float

            if (selectedTab == 0) {
                totalTracked = dailyReport?.trackedMillis ?: 0L
                totalUntracked = dailyReport?.untrackedMillis ?: 0L
                totalBreak = dailyReport?.breakMillis ?: 0L
                totalPlanned = dailyReport?.plannedMillis ?: 0L
                totalActualLinked = dailyReport?.actualLinkedPlannedMillis ?: 0L
                avgCompleteness = dailyReport?.trackingCompletenessPercent ?: 0f
            } else {
                totalTracked = periodReports.sumOf { it.trackedMillis }
                totalUntracked = periodReports.sumOf { it.untrackedMillis }
                totalBreak = periodReports.sumOf { it.breakMillis }
                totalPlanned = periodReports.sumOf { it.plannedMillis }
                totalActualLinked = periodReports.sumOf { it.actualLinkedPlannedMillis }
                avgCompleteness = if (periodReports.isNotEmpty()) {
                    periodReports.map { it.trackingCompletenessPercent }.average().toFloat()
                } else 0f
            }

            MetricsGrid(
                trackedMillis = totalTracked,
                untrackedMillis = totalUntracked,
                breakMillis = totalBreak,
                plannedMillis = totalPlanned,
                actualLinkedMillis = totalActualLinked,
                completeness = avgCompleteness,
                daysCount = if (selectedTab == 0) 1 else periodReports.size
            )
        }

        // Daily Trend Bar Chart (for Weekly or Monthly)
        if (selectedTab != 0 && periodReports.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedTab == 1) "Daily Tracked Hours (This Week)" else "Daily Tracked Hours (This Month)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DailyTrendChart(reports = periodReports)
                    }
                }
            }
        }

        // Activity Breakdown Section
        item {
            val breakdowns = if (selectedTab == 0) {
                dailyReport?.activityBreakdowns ?: emptyList()
            } else {
                val map = mutableMapOf<Long, com.example.domain.time.ActivityBreakdown>()
                val totalTracked = periodReports.sumOf { it.trackedMillis }
                for (r in periodReports) {
                    for (b in r.activityBreakdowns) {
                        val existing = map[b.activityId]
                        if (existing == null) {
                            map[b.activityId] = b
                        } else {
                            map[b.activityId] = existing.copy(
                                durationMillis = existing.durationMillis + b.durationMillis,
                                sessionCount = existing.sessionCount + b.sessionCount
                            )
                        }
                    }
                }
                map.values.map {
                    it.copy(
                        percentageOfTracked = if (totalTracked > 0) (it.durationMillis.toFloat() / totalTracked) * 100f else 0f
                    )
                }.sortedByDescending { it.durationMillis }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Activity Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (breakdowns.isEmpty()) {
                        Text(
                            text = "No tracked activities in this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            breakdowns.forEach { b ->
                                val hours = b.durationMillis / 3600000.0
                                val color = try {
                                    Color(android.graphics.Color.parseColor(b.colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = b.activityName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${String.format("%.1f", hours)}h (${String.format("%.0f", b.percentageOfTracked)}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (b.percentageOfTracked / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Activity Timeline (if Daily view)
        if (selectedTab == 0 && dailyReport != null && dailyReport!!.segments.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Day Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dailyReport!!.segments.forEach { seg ->
                                val startStr = TimeFormatUtils.formatTime(seg.segmentStart, is24Hour)
                                val endStr = TimeFormatUtils.formatTime(seg.segmentEnd, is24Hour)
                                val mins = seg.durationMillis / 60000

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$startStr - $endStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(90.dp)
                                        )
                                        Text(
                                            text = seg.activityName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export to Excel Quick Link
        item {
            Button(
                onClick = onNavigateToExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_xlsx_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Period to Excel (.xlsx)")
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
fun PeriodNavHeader(
    selectedTab: Int,
    currentDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val titleText = when (selectedTab) {
        0 -> currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        1 -> {
            val weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(6)
            "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        }
        else -> currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun MetricsGrid(
    trackedMillis: Long,
    untrackedMillis: Long,
    breakMillis: Long,
    plannedMillis: Long,
    actualLinkedMillis: Long,
    completeness: Float,
    daysCount: Int
) {
    val trackedHrs = trackedMillis / 3600000.0
    val untrackedHrs = untrackedMillis / 3600000.0
    val breakHrs = breakMillis / 3600000.0
    val plannedHrs = plannedMillis / 3600000.0
    val actualHrs = actualLinkedMillis / 3600000.0
    val dailyAvg = if (daysCount > 0) trackedHrs / daysCount else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Tracked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", trackedHrs)}h", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (daysCount > 1) {
                        Text("Avg: ${String.format("%.1f", dailyAvg)}h/day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column {
                    Text("Untracked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", untrackedHrs)}h", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("Unlogged time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    Text("Breaks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", breakHrs)}h", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text("Recorded rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Planned vs Actual: ${String.format("%.1f", actualHrs)}h of ${String.format("%.1f", plannedHrs)}h planned",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.0f", completeness)}% completeness",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DailyTrendChart(reports: List<DayTimeReport>) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxHrs = max(1.0, reports.maxOfOrNull { it.trackedMillis / 3600000.0 } ?: 8.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 24.dp.toPx()
            val count = reports.size
            if (count == 0) return@Canvas

            val barWidth = (canvasWidth / count) * 0.6f
            val spacing = canvasWidth / count

            reports.forEachIndexed { index, r ->
                val hrs = r.trackedMillis / 3600000.0
                val barHeight = ((hrs / maxHrs) * canvasHeight).toFloat().coerceAtLeast(4f)
                val x = (index * spacing) + (spacing - barWidth) / 2
                val y = canvasHeight - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
