package com.example.ui.reminders

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmScheduler
import com.example.data.local.entity.ReminderEntity
import com.example.domain.time.TimeFormatUtils
import com.example.ui.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReminders by viewModel.allReminders.collectAsState()
    val currentTime by viewModel.currentTimeMillis.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val is24Hour = userSettings?.is24HourFormat ?: true

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Upcoming, 1: Past / Completed
    var showCreateDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }

    val canExactAlarm = remember { AlarmScheduler.canScheduleExactAlarms(context) }

    val upcomingReminders = remember(allReminders, currentTime) {
        allReminders.filter { it.triggerTime > currentTime && !it.isCompleted && !it.isCancelled }
    }
    val pastReminders = remember(allReminders, currentTime) {
        allReminders.filter { it.triggerTime <= currentTime || it.isCompleted || it.isCancelled }
    }

    val displayList = if (selectedTab == 0) upcomingReminders else pastReminders

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("reminders_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    reminderToEdit = null
                    showCreateDialog = true
                },
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Permission explanation banner if exact alarms are disabled
            if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact alarms require system permission",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Alarms will use standard background scheduling until exact alarm permission is granted.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Upcoming (${upcomingReminders.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History (${pastReminders.size})") }
                )
            }

            if (displayList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTab == 0) "No upcoming reminders scheduled." else "No past reminder history.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayList, key = { it.id }) { reminder ->
                        ReminderItemCard(
                            reminder = reminder,
                            isUpcoming = selectedTab == 0,
                            is24Hour = is24Hour,
                            onEdit = { reminderToEdit = reminder },
                            onComplete = { viewModel.dismissReminder(reminder.id) },
                            onCancel = { viewModel.cancelReminder(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showCreateDialog || reminderToEdit != null) {
        ReminderScheduleDialog(
            initial = reminderToEdit,
            is24Hour = is24Hour,
            onDismiss = {
                showCreateDialog = false
                reminderToEdit = null
            },
            onSave = { entity ->
                viewModel.saveReminder(entity)
                showCreateDialog = false
                reminderToEdit = null
            }
        )
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    isUpcoming: Boolean,
    is24Hour: Boolean,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = !reminder.isCompleted && !reminder.isCancelled && reminder.triggerTime <= now
    val formattedTime = TimeFormatUtils.formatDateTime(reminder.triggerTime, is24Hour)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_item_${reminder.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                reminder.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                reminder.isCompleted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status Badge
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        reminder.isCompleted -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("COMPLETED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        reminder.isCancelled -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("CANCELLED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        isOverdue -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.error) {
                                Text("OVERDUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        else -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("UPCOMING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    if (reminder.snoozeCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Snoozed ${reminder.snoozeCount}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            if (reminder.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.isSoundEnabled) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "12s sound", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("12s Alarm Tone", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (reminder.isVibrationEnabled) {
                        Icon(Icons.Default.Vibration, contentDescription = "Vibrate on", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isUpcoming && !reminder.isCompleted && !reminder.isCancelled) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onComplete) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Done")
                    }
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

data class QuickDuration(val label: String, val minutes: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderScheduleDialog(
    initial: ReminderEntity?,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var soundEnabled by remember { mutableStateOf(initial?.isSoundEnabled ?: true) }
    var vibrationEnabled by remember { mutableStateOf(initial?.isVibrationEnabled ?: true) }

    // 0: Choose clock time, 1: Remind me in
    var scheduleMode by remember { mutableIntStateOf(0) }

    // Mode 0: Clock Time Picker state
    val nowInstant = Instant.now()
    val zone = ZoneId.systemDefault()
    val nowLocalTime = LocalTime.now(zone)
    val nowLocalDate = LocalDate.now(zone)

    val initialTime = if (initial != null) {
        Instant.ofEpochMilli(initial.triggerTime).atZone(zone).toLocalTime()
    } else {
        nowLocalTime.plusMinutes(15)
    }

    val initialDate = if (initial != null) {
        Instant.ofEpochMilli(initial.triggerTime).atZone(zone).toLocalDate()
    } else {
        if (nowLocalTime.plusMinutes(15).isBefore(nowLocalTime)) nowLocalDate.plusDays(1) else nowLocalDate
    }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var useDialPicker by remember { mutableStateOf(true) }

    // Native Material 3 TimePicker state (keyed to is24Hour to ensure format consistency)
    val timePickerState = key(is24Hour) {
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = is24Hour
        )
    }

    // Mode 1: Quick Durations
    val quickDurations = remember {
        listOf(
            QuickDuration("10 min", 10L),
            QuickDuration("15 min", 15L),
            QuickDuration("30 min", 30L),
            QuickDuration("45 min", 45L),
            QuickDuration("1 hour", 60L),
            QuickDuration("2 hours", 120L)
        )
    }
    var selectedQuickMinutes by remember { mutableLongStateOf(30L) }
    var quickSelectionBaseTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val calculatedTargetMillis = remember(
        scheduleMode,
        selectedDate,
        timePickerState.hour,
        timePickerState.minute,
        selectedQuickMinutes,
        quickSelectionBaseTime
    ) {
        if (scheduleMode == 0) {
            val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
            selectedDate.atTime(localTime).atZone(zone).toInstant().toEpochMilli()
        } else {
            // Remind me in: calculate from the actual current instant when selected
            quickSelectionBaseTime + (selectedQuickMinutes * 60 * 1000L)
        }
    }

    val isPastTime = scheduleMode == 0 && calculatedTargetMillis <= System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Schedule Reminder" else "Edit Reminder")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Mode Tabs: 1. Choose clock time, 2. Remind me in
                TabRow(selectedTabIndex = scheduleMode) {
                    Tab(
                        selected = scheduleMode == 0,
                        onClick = { scheduleMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clock time")
                            }
                        },
                        modifier = Modifier.testTag("tab_choose_clock_time")
                    )
                    Tab(
                        selected = scheduleMode == 1,
                        onClick = { scheduleMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remind me in")
                            }
                        },
                        modifier = Modifier.testTag("tab_remind_me_in")
                    )
                }

                if (scheduleMode == 0) {
                    // Date selector: Today, Tomorrow, Day After
                    Text("Select Date", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDate == nowLocalDate,
                            onClick = { selectedDate = nowLocalDate },
                            label = { Text("Today") },
                            modifier = Modifier.testTag("date_chip_today")
                        )
                        FilterChip(
                            selected = selectedDate == nowLocalDate.plusDays(1),
                            onClick = { selectedDate = nowLocalDate.plusDays(1) },
                            label = { Text("Tomorrow") },
                            modifier = Modifier.testTag("date_chip_tomorrow")
                        )
                        FilterChip(
                            selected = selectedDate == nowLocalDate.plusDays(2),
                            onClick = { selectedDate = nowLocalDate.plusDays(2) },
                            label = { Text(nowLocalDate.plusDays(2).dayOfWeek.name.take(3)) }
                        )
                    }

                    // Clock Time Header with clear formatted time and mode switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time: ${TimeFormatUtils.formatTime(timePickerState.hour, timePickerState.minute, is24Hour)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("selected_clock_time_label")
                        )
                        IconButton(
                            onClick = { useDialPicker = !useDialPicker },
                            modifier = Modifier.testTag("toggle_picker_mode_button")
                        ) {
                            Icon(
                                if (useDialPicker) Icons.Default.Keyboard else Icons.Default.Schedule,
                                contentDescription = if (useDialPicker) "Switch to keyboard input" else "Switch to clock dial"
                            )
                        }
                    }

                    // Native Material 3 Time Picker (Dial or Keyboard Input)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (useDialPicker) {
                            TimePicker(
                                state = timePickerState,
                                modifier = Modifier.testTag("time_picker_dial")
                            )
                        } else {
                            TimeInput(
                                state = timePickerState,
                                modifier = Modifier.testTag("time_picker_input")
                            )
                        }
                    }

                    // Past time warning
                    if (isPastTime) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Selected time is in the past. Choose a future time or select Tomorrow.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                } else {
                    // Remind me in: 6 Quick Duration Buttons
                    Text("Select Quick Duration", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickDurations.forEach { qd ->
                            FilterChip(
                                selected = selectedQuickMinutes == qd.minutes,
                                onClick = {
                                    selectedQuickMinutes = qd.minutes
                                    quickSelectionBaseTime = System.currentTimeMillis()
                                },
                                label = { Text(qd.label) },
                                modifier = Modifier.testTag("quick_duration_${qd.minutes}")
                            )
                        }
                    }
                }

                // Summary of resulting scheduled alert
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Scheduled Alert Time:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = TimeFormatUtils.formatScheduledSummary(calculatedTargetMillis, is24Hour),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Sound & Vibration controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                    Text("Sound Alert")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                    Text("Vibration Alert")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && !isPastTime) {
                        val toSave = initial?.copy(
                            title = title.trim(),
                            description = description.trim(),
                            triggerTime = calculatedTargetMillis,
                            isSoundEnabled = soundEnabled,
                            isVibrationEnabled = vibrationEnabled,
                            isCompleted = false,
                            isCancelled = false
                        ) ?: ReminderEntity(
                            title = title.trim(),
                            description = description.trim(),
                            triggerTime = calculatedTargetMillis,
                            isSoundEnabled = soundEnabled,
                            isVibrationEnabled = vibrationEnabled
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank() && !isPastTime,
                modifier = Modifier.testTag("save_reminder_button")
            ) {
                Text(if (initial == null) "Schedule" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
