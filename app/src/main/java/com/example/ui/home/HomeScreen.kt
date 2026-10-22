package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.domain.focus.FocusPhaseType
import com.example.domain.focus.ReconciledFocusState
import com.example.domain.time.TimeFormatUtils
import com.example.ui.MainViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToActivities: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTime by viewModel.currentTimeMillis.collectAsState()
    val todayReport by viewModel.todayReport.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val favorites by viewModel.favoriteActivities.collectAsState()
    val activeActivities by viewModel.activeActivities.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()
    val focusState by viewModel.focusState.collectAsState()
    val activeFocusRun by viewModel.activeFocusRun.collectAsState()
    val todayTasks by viewModel.selectedDateTasks.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val is24Hour = userSettings?.is24HourFormat ?: true

    val nextReminder = remember(reminders, currentTime) {
        reminders
            .filter { it.triggerTime > currentTime && !it.isCompleted && !it.isCancelled }
            .minByOrNull { it.triggerTime }
    }

    val currentLocalTime = LocalTime.now()
    val greeting = when (currentLocalTime.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val currentSlotIndex = (currentLocalTime.hour * 2) + (currentLocalTime.minute / 30)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Editorial Header with Greeting, Date, Live Clock & Quick Actions
        item {
            HomeHeaderSection(
                greeting = greeting,
                dateText = LocalDate.now().format(dateFormatter),
                timeText = TimeFormatUtils.formatTimeWithSeconds(currentTime, is24Hour),
                onOpenReminders = onNavigateToReminders,
                onOpenHistory = onNavigateToHistory,
                onOpenSettings = onNavigateToSettings
            )
        }

        // 2. Main Focus Hero: Active Tracking or Ready State
        item {
            HomeActiveSessionHero(
                activeSession = activeSession,
                currentTime = currentTime,
                onStopTracking = { viewModel.stopTracking() },
                onNavigateToActivities = onNavigateToActivities
            )
        }

        // 3. Quick-Start Favorites Strip
        item {
            HomeQuickStartSection(
                favorites = favorites,
                allActivities = activeActivities,
                onSelectActivity = { activity -> viewModel.startTracking(activity) },
                onBrowseAll = onNavigateToActivities
            )
        }

        // 4. Daily Time Allocation Composition (Tonal Cards + Segmented Bar)
        item {
            HomeTimeAllocationCard(
                todayReport = todayReport,
                onViewReports = {}
            )
        }

        // 5. Pomodoro Focus Quick Widget
        item {
            HomeFocusWidget(
                activeFocusRun = activeFocusRun,
                focusState = focusState,
                onStartFocus = { viewModel.startFocus() },
                onStopFocus = { viewModel.stopFocus() },
                onOpenFocusScreen = onNavigateToFocus
            )
        }

        // 6. Next Upcoming Reminder (if present)
        if (nextReminder != null) {
            item {
                HomeUpcomingReminderCard(
                    reminder = nextReminder,
                    is24Hour = is24Hour,
                    onOpenReminders = onNavigateToReminders
                )
            }
        }

        // 7. Today's Schedule (Vertical Timeline)
        item {
            HomeScheduleTimeline(
                todayTasks = todayTasks,
                activities = activeActivities,
                currentSlotIndex = currentSlotIndex,
                is24Hour = is24Hour,
                onOpenTimetable = onNavigateToTimetable
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun HomeHeaderSection(
    greeting: String,
    dateText: String,
    timeText: String,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("home_live_clock")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = onOpenReminders,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = "Reminders", modifier = Modifier.size(18.dp))
                }
                FilledTonalIconButton(
                    onClick = onOpenHistory,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(18.dp))
                }
                FilledTonalIconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun HomeActiveSessionHero(
    activeSession: com.example.data.local.entity.ActivitySessionEntity?,
    currentTime: Long,
    onStopTracking: () -> Unit,
    onNavigateToActivities: () -> Unit
) {
    val isTracking = activeSession != null
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_session_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isTracking) 2.dp else 1.dp,
            color = if (isTracking) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (activeSession != null) {
                val elapsedSeconds = (currentTime - activeSession.startTime).coerceAtLeast(0L) / 1000L
                val hours = elapsedSeconds / 3600
                val mins = (elapsedSeconds % 3600) / 60
                val secs = elapsedSeconds % 60
                val elapsedFormatted = String.format("%02d:%02d:%02d", hours, mins, secs)

                val sessionColor = try {
                    Color(android.graphics.Color.parseColor(activeSession.colorHexSnapshot))
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(sessionColor)
                                .alpha(pulseAlpha)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = sessionColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "RECORDING ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = sessionColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (activeSession.isBreak) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Break Session",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = activeSession.activityNameSnapshot,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = activeSession.categorySnapshot,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = elapsedFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStopTracking,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("stop_tracking_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Activity", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToActivities,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Switch Activity")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time is flowing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No activity currently tracked. Tap below to capture your focused time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToActivities,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_activity_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Tracking")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Tracking Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun HomeQuickStartSection(
    favorites: List<ActivityEntity>,
    allActivities: List<ActivityEntity>,
    onSelectActivity: (ActivityEntity) -> Unit,
    onBrowseAll: () -> Unit
) {
    val items = if (favorites.isNotEmpty()) favorites else allActivities.take(6)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (favorites.isNotEmpty()) "Favorite Activities" else "Quick Start Activities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = onBrowseAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Browse All", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (items.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No activities created yet. Tap 'Browse All' to create your first activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { activity ->
                    HomeQuickStartChip(
                        activity = activity,
                        onClick = { onSelectActivity(activity) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeQuickStartChip(
    activity: ActivityEntity,
    onClick: () -> Unit
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(activity.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, parsedColor.copy(alpha = 0.35f)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("quick_start_${activity.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = parsedColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = activity.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HomeTimeAllocationCard(
    todayReport: com.example.domain.time.DayTimeReport?,
    onViewReports: () -> Unit
) {
    val trackedHrs = (todayReport?.trackedMillis ?: 0L) / 3600000.0
    val untrackedHrs = (todayReport?.untrackedMillis ?: 0L) / 3600000.0
    val breakHrs = (todayReport?.breakMillis ?: 0L) / 3600000.0
    val plannedHrs = (todayReport?.plannedMillis ?: 0L) / 3600000.0
    val actualHrs = (todayReport?.actualLinkedPlannedMillis ?: 0L) / 3600000.0
    val completeness = todayReport?.trackingCompletenessPercent ?: 0f

    val totalDayMillis = ((todayReport?.trackedMillis ?: 0L) + (todayReport?.untrackedMillis ?: 0L) + (todayReport?.breakMillis ?: 0L)).coerceAtLeast(1L)
    val trackedFraction = ((todayReport?.trackedMillis ?: 0L).toFloat() / totalDayMillis).coerceIn(0f, 1f)
    val untrackedFraction = ((todayReport?.untrackedMillis ?: 0L).toFloat() / totalDayMillis).coerceIn(0f, 1f)
    val breakFraction = ((todayReport?.breakMillis ?: 0L).toFloat() / totalDayMillis).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Time Accounting",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Accurate breakdown of active vs untracked hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${String.format("%.0f", completeness)}% Logged",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Proportional horizontal timeline bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (trackedFraction > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(trackedFraction.coerceAtLeast(0.01f))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (breakFraction > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(breakFraction.coerceAtLeast(0.01f))
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                }
                if (untrackedFraction > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(untrackedFraction.coerceAtLeast(0.01f))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Coordinated Tonal Stat Blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeStatCard(
                    label = "Tracked",
                    hours = trackedHrs,
                    icon = Icons.Default.Timer,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                HomeStatCard(
                    label = "Untracked",
                    hours = untrackedHrs,
                    icon = Icons.Default.HelpOutline,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                HomeStatCard(
                    label = "Breaks",
                    hours = breakHrs,
                    icon = Icons.Default.Coffee,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (plannedHrs > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timetable Planned vs Logged:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", actualHrs)}h of ${String.format("%.1f", plannedHrs)}h planned",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HomeStatCard(
    label: String,
    hours: Double,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = accentColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format("%.1f", hours)}h",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

@Composable
fun HomeFocusWidget(
    activeFocusRun: com.example.data.local.entity.FocusRunEntity?,
    focusState: ReconciledFocusState?,
    onStartFocus: () -> Unit,
    onStopFocus: () -> Unit,
    onOpenFocusScreen: () -> Unit
) {
    val isRunning = activeFocusRun != null && !activeFocusRun.isFinished && focusState != null
    val phase = focusState?.phase ?: FocusPhaseType.FOCUS

    val phaseColor = if (phase == FocusPhaseType.FOCUS) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFocusScreen() }
            .testTag("focus_widget_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(phaseColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = phaseColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Focus Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = phaseColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isRunning) {
                                    if (phase == FocusPhaseType.FOCUS) "FOCUSING" else "REST BREAK"
                                } else "25 / 5m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = phaseColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isRunning && focusState != null) {
                        val remSecs = (focusState.remainingMillis / 1000L).coerceAtLeast(0L)
                        val mins = remSecs / 60
                        val secs = remSecs % 60
                        Text(
                            text = "${String.format("%02d:%02d", mins, secs)} • Cycle #${focusState.cycleNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                    } else {
                        Text(
                            text = "Start recurring cycles of deep work & rest",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isRunning) {
                FilledTonalButton(
                    onClick = onStopFocus,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = onStartFocus,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Start")
                }
            }
        }
    }
}

@Composable
fun HomeUpcomingReminderCard(
    reminder: com.example.data.local.entity.ReminderEntity,
    is24Hour: Boolean,
    onOpenReminders: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReminders() }
            .testTag("next_reminder_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UPCOMING ALARM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "12s Ringtone",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val schedTime = TimeFormatUtils.formatTime(reminder.triggerTime, is24Hour)
                Text(
                    text = "Scheduled for $schedTime (12s dedicated ringtone)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HomeScheduleTimeline(
    todayTasks: List<TimetableTaskEntity>,
    activities: List<ActivityEntity>,
    currentSlotIndex: Int,
    is24Hour: Boolean,
    onOpenTimetable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onOpenTimetable,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Open Planner (${todayTasks.size})", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (todayTasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No timetable tasks planned for today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onOpenTimetable) {
                            Text("+ Plan Your 24 Hours")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    todayTasks.sortedBy { it.slotIndex }.take(4).forEach { task ->
                        val taskHour = task.slotIndex / 2
                        val taskMin = (task.slotIndex % 2) * 30
                        val endMinutes = (task.slotIndex * 30) + task.durationMinutes
                        val endHour = (endMinutes / 60) % 24
                        val endMin = endMinutes % 60

                        val startStr = TimeFormatUtils.formatTime(taskHour, taskMin, is24Hour)
                        val endStr = TimeFormatUtils.formatTime(endHour, endMin, is24Hour)

                        val isCurrent = currentSlotIndex >= task.slotIndex && currentSlotIndex < (task.slotIndex + (task.durationMinutes / 30))
                        val isPast = (task.slotIndex + (task.durationMinutes / 30)) <= currentSlotIndex

                        val taskColor = remember(task, activities) {
                            val matched = activities.find { it.id == task.linkedActivityId }
                            if (matched != null) {
                                try { Color(android.graphics.Color.parseColor(matched.colorHex)) } catch (_: Exception) { null }
                            } else null
                        } ?: MaterialTheme.colorScheme.primary

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                                .then(
                                    if (isCurrent) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(taskColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$startStr - $endStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(88.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            if (task.isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "NOW",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
