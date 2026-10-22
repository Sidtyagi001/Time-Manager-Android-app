package com.example.ui.timetable

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.domain.time.TimeAccountingEngine
import com.example.domain.time.TimeFormatUtils
import com.example.ui.MainViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.selectedDateTasks.collectAsState()
    val activeActivities by viewModel.activeActivities.collectAsState()
    val currentTime by viewModel.currentTimeMillis.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val is24Hour = userSettings?.is24HourFormat ?: true

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TimetableTaskEntity?>(null) }
    var defaultSlotForNewTask by remember { mutableIntStateOf(18) } // 09:00 default

    val isToday = selectedDate == LocalDate.now()
    val currentLocalTime = LocalTime.now()
    val currentSlotIndex = (currentLocalTime.hour * 2) + (currentLocalTime.minute / 30)

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("timetable_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    defaultSlotForNewTask = currentSlotIndex
                    showTaskDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_timetable_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Date Navigation & Quick Action Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousDate() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedDate.format(dateFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isToday) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "TODAY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.nextDate() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Actions Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isToday) {
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.goToToday() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Go to Today")
                                }
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.copyTimetableFromPreviousDay() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("copy_prev_day_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Yesterday")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.applyTemplateToSelectedDay(isWeekend = false) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weekday Routine")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.applyTemplateToSelectedDay(isWeekend = true) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weekend Routine")
                            }
                        }
                    }
                }
            }

            // 2. 24-Hour Timeline Planner List (00:00 to 23:59)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (hour in 0..23) {
                    val hourSlot0 = hour * 2 // e.g. 10:00
                    val hourSlot1 = hour * 2 + 1 // e.g. 10:30

                    val tasksInHour = tasks.filter { it.slotIndex == hourSlot0 || it.slotIndex == hourSlot1 }

                    // High-visibility Current Time Indicator Line
                    if (isToday && currentLocalTime.hour == hour) {
                        item(key = "now_indicator_$hour") {
                            CurrentTimeIndicator(currentLocalTime = currentLocalTime, is24Hour = is24Hour)
                        }
                    }

                    item(key = "hour_$hour") {
                        HourBlockView(
                            hour = hour,
                            tasksInHour = tasksInHour,
                            activities = activeActivities,
                            currentSlotIndex = currentSlotIndex,
                            isToday = isToday,
                            is24Hour = is24Hour,
                            onAddTask = { slot ->
                                defaultSlotForNewTask = slot
                                taskToEdit = null
                                showTaskDialog = true
                            },
                            onEditTask = { task ->
                                taskToEdit = task
                                showTaskDialog = true
                            },
                            onDeleteTask = { task -> viewModel.deleteTimetableTask(task.id) },
                            onSplitTask = { task -> viewModel.splitTaskSlot(task) },
                            onToggleComplete = { task ->
                                viewModel.saveTimetableTask(task.copy(isCompleted = !task.isCompleted))
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }

    if (showTaskDialog) {
        TimetableTaskDialog(
            initial = taskToEdit,
            defaultSlot = defaultSlotForNewTask,
            dateStr = TimeAccountingEngine.formatDate(selectedDate),
            activities = activeActivities,
            is24Hour = is24Hour,
            onDismiss = { showTaskDialog = false; taskToEdit = null },
            onSave = { task ->
                viewModel.saveTimetableTask(task)
                showTaskDialog = false
                taskToEdit = null
            }
        )
    }
}

@Composable
fun CurrentTimeIndicator(currentLocalTime: LocalTime, is24Hour: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.error
        ) {
            Text(
                text = "${TimeFormatUtils.formatTime(currentLocalTime.hour, currentLocalTime.minute, is24Hour)} NOW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.error,
            thickness = 2.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun HourBlockView(
    hour: Int,
    tasksInHour: List<TimetableTaskEntity>,
    activities: List<ActivityEntity>,
    currentSlotIndex: Int,
    isToday: Boolean,
    is24Hour: Boolean,
    onAddTask: (slot: Int) -> Unit,
    onEditTask: (TimetableTaskEntity) -> Unit,
    onDeleteTask: (TimetableTaskEntity) -> Unit,
    onSplitTask: (TimetableTaskEntity) -> Unit,
    onToggleComplete: (TimetableTaskEntity) -> Unit
) {
    val timeLabel = TimeFormatUtils.formatTime(hour, 0, is24Hour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Hour Ruler with Node Indicator
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Right Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (tasksInHour.isEmpty()) {
                // Empty 1-Hour Slot
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unplanned (60m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        TextButton(
                            onClick = { onAddTask(hour * 2) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Plan Task", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                tasksInHour.forEach { task ->
                    TaskCard(
                        task = task,
                        activities = activities,
                        currentSlotIndex = currentSlotIndex,
                        isToday = isToday,
                        is24Hour = is24Hour,
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onSplit = { onSplitTask(task) },
                        onToggleComplete = { onToggleComplete(task) }
                    )
                }

                // If only one 30-min task occupies the hour, offer to plan the other 30m slot
                if (tasksInHour.size == 1 && tasksInHour[0].durationMinutes == 30) {
                    val occupiedSlot = tasksInHour[0].slotIndex
                    val emptySlot = if (occupiedSlot % 2 == 0) occupiedSlot + 1 else occupiedSlot - 1
                    val emptySlotMinute = if (emptySlot % 2 == 0) 0 else 30
                    val emptyTimeStr = TimeFormatUtils.formatTime(hour, emptySlotMinute, is24Hour)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$emptyTimeStr Empty (30m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            TextButton(
                                onClick = { onAddTask(emptySlot) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("+ 30m Task", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TimetableTaskEntity,
    activities: List<ActivityEntity>,
    currentSlotIndex: Int,
    isToday: Boolean,
    is24Hour: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSplit: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val startHour = task.slotIndex / 2
    val startMin = (task.slotIndex % 2) * 30
    val endMinutesTotal = (task.slotIndex * 30) + task.durationMinutes
    val endHour = (endMinutesTotal / 60) % 24
    val endMin = endMinutesTotal % 60
    val timeRangeStr = "${TimeFormatUtils.formatTime(startHour, startMin, is24Hour)} - ${TimeFormatUtils.formatTime(endHour, endMin, is24Hour)}"

    val isCurrent = isToday && currentSlotIndex >= task.slotIndex && currentSlotIndex < (task.slotIndex + (task.durationMinutes / 30))

    val taskColor = remember(task, activities) {
        val matched = activities.find { it.id == task.linkedActivityId }
        if (matched != null) {
            try { Color(android.graphics.Color.parseColor(matched.colorHex)) } catch (_: Exception) { null }
        } else {
            when (task.category.lowercase()) {
                "work", "career" -> Color(0xFF4F46E5)
                "study", "education" -> Color(0xFF2563EB)
                "exercise", "health" -> Color(0xFF059669)
                "reading", "personal" -> Color(0xFF0D9488)
                "cooking", "life" -> Color(0xFFD97706)
                "creative", "art" -> Color(0xFF9333EA)
                "chores" -> Color(0xFF64748B)
                "transit", "travel" -> Color(0xFF0284C7)
                "rest", "leisure" -> Color(0xFFEA580C)
                else -> null
            }
        }
    } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_task_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) taskColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task category indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(taskColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox for completion
            IconButton(onClick = onToggleComplete, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = taskColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${task.durationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = taskColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(4.dp))
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
                Text(
                    text = listOf(timeRangeStr, task.category, task.description).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Split 60m into two 30m blocks action
            if (task.durationMinutes == 60) {
                IconButton(onClick = onSplit, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.CallSplit,
                        contentDescription = "Split into two 30-min blocks",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TimetableTaskDialog(
    initial: TimetableTaskEntity?,
    defaultSlot: Int,
    dateStr: String,
    activities: List<ActivityEntity>,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (TimetableTaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Work") }
    var slotIndex by remember { mutableIntStateOf(initial?.slotIndex ?: defaultSlot) }
    var durationMinutes by remember { mutableIntStateOf(initial?.durationMinutes ?: 60) }
    var linkedActivityId by remember { mutableStateOf<Long?>(initial?.linkedActivityId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Plan Task" else "Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Slot start time
                val startHour = slotIndex / 2
                val startMin = (slotIndex % 2) * 30
                Text(
                    text = "Scheduled Start: ${TimeFormatUtils.formatTime(startHour, startMin, is24Hour)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Duration Selector: 30m vs 60m
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duration:", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { durationMinutes = 30 },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (durationMinutes == 30) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (durationMinutes == 30) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("30 min")
                    }
                    Button(
                        onClick = { durationMinutes = 60 },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (durationMinutes == 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (durationMinutes == 60) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("60 min")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val toSave = initial?.copy(
                            title = title.trim(),
                            description = description.trim(),
                            category = category.trim(),
                            slotIndex = slotIndex,
                            durationMinutes = durationMinutes,
                            linkedActivityId = linkedActivityId
                        ) ?: TimetableTaskEntity(
                            dateStr = dateStr,
                            slotIndex = slotIndex,
                            durationMinutes = durationMinutes,
                            title = title.trim(),
                            description = description.trim(),
                            category = category.trim(),
                            linkedActivityId = linkedActivityId
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_timetable_task_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
