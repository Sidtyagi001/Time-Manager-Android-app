package com.example.ui.timetable

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
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.local.entity.TimetableTaskEntity
import com.example.domain.time.TimeAccountingEngine
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
    val dayReport by viewModel.selectedDateReport.collectAsState()
    val currentTime by viewModel.currentTimeMillis.collectAsState()

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TimetableTaskEntity?>(null) }
    var defaultSlotForNewTask by remember { mutableIntStateOf(18) } // 09:00 default

    val isToday = selectedDate == LocalDate.now()
    val currentLocalTime = LocalTime.now()
    val currentSlotIndex = (currentLocalTime.hour * 2) + (currentLocalTime.minute / 30)

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("timetable_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    defaultSlotForNewTask = currentSlotIndex
                    showTaskDialog = true
                },
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
            // 1. Date Navigation Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                                fontWeight = FontWeight.Bold
                            )
                            if (isToday) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.nextDate() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }

                    // Action buttons
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isToday) {
                            item {
                                OutlinedButton(onClick = { viewModel.goToToday() }) {
                                    Text("Go to Today")
                                }
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.copyTimetableFromPreviousDay() },
                                modifier = Modifier.testTag("copy_prev_day_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Yesterday")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.applyTemplateToSelectedDay(isWeekend = false) }
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weekday Routine")
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.applyTemplateToSelectedDay(isWeekend = true) }
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weekend Routine")
                            }
                        }
                    }
                }
            }

            // 2. 24-Hour Timetable List (00:00 to 23:59)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Iterate 24 hours (0 to 23)
                for (hour in 0..23) {
                    val hourSlot0 = hour * 2 // e.g. 10:00
                    val hourSlot1 = hour * 2 + 1 // e.g. 10:30

                    val tasksInHour = tasks.filter { it.slotIndex == hourSlot0 || it.slotIndex == hourSlot1 }

                    // Current Time Indicator
                    if (isToday && currentLocalTime.hour == hour) {
                        item {
                            CurrentTimeIndicator(currentLocalTime = currentLocalTime)
                        }
                    }

                    item(key = "hour_$hour") {
                        HourBlockView(
                            hour = hour,
                            tasksInHour = tasksInHour,
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

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showTaskDialog) {
        TimetableTaskDialog(
            initial = taskToEdit,
            defaultSlot = defaultSlotForNewTask,
            dateStr = TimeAccountingEngine.formatDate(selectedDate),
            activities = activeActivities,
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
fun CurrentTimeIndicator(currentLocalTime: LocalTime) {
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
                text = String.format("%02d:%02d NOW", currentLocalTime.hour, currentLocalTime.minute),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    onAddTask: (slot: Int) -> Unit,
    onEditTask: (TimetableTaskEntity) -> Unit,
    onDeleteTask: (TimetableTaskEntity) -> Unit,
    onSplitTask: (TimetableTaskEntity) -> Unit,
    onToggleComplete: (TimetableTaskEntity) -> Unit
) {
    val timeLabel = String.format("%02d:00", hour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Hour Label
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )

        // Right Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (tasksInHour.isEmpty()) {
                // Empty 1-hour slot
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unassigned (60m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        TextButton(onClick = { onAddTask(hour * 2) }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Plan Task")
                        }
                    }
                }
            } else {
                tasksInHour.forEach { task ->
                    TaskCard(
                        task = task,
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onSplit = { onSplitTask(task) },
                        onToggleComplete = { onToggleComplete(task) }
                    )
                }

                // If only 1 30-min task occupies the hour, offer to plan the other 30-min slot
                if (tasksInHour.size == 1 && tasksInHour[0].durationMinutes == 30) {
                    val occupiedSlot = tasksInHour[0].slotIndex
                    val emptySlot = if (occupiedSlot % 2 == 0) occupiedSlot + 1 else occupiedSlot - 1
                    val emptySlotMin = if (emptySlot % 2 == 0) "00" else "30"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                                text = String.format("%02d:%s Empty (30m)", hour, emptySlotMin),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            TextButton(onClick = { onAddTask(emptySlot) }) {
                                Text("+ 30m Task")
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSplit: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_task_${task.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleComplete, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${task.durationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (task.category.isNotBlank() || task.description.isNotBlank()) {
                    Text(
                        text = listOf(task.category, task.description).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Split 60m into two 30m blocks
            if (task.durationMinutes == 60) {
                IconButton(onClick = onSplit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.CallSplit,
                        contentDescription = "Split into two 30-min blocks",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TimetableTaskDialog(
    initial: TimetableTaskEntity?,
    defaultSlot: Int,
    dateStr: String,
    activities: List<com.example.data.local.entity.ActivityEntity>,
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
        title = { Text(if (initial == null) "Plan Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Start time slot picker
                val hour = slotIndex / 2
                val min = (slotIndex % 2) * 30
                Text("Start Time: ${String.format("%02d:%02d", hour, min)}", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(24) { h ->
                        val slot = h * 2
                        FilledTonalButton(
                            onClick = { slotIndex = slot },
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(String.format("%02d:00", h))
                        }
                    }
                }

                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { durationMinutes = 30 },
                        colors = if (durationMinutes == 30) androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("30 Minutes")
                    }
                    FilledTonalButton(
                        onClick = { durationMinutes = 60 },
                        colors = if (durationMinutes == 60) androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("60 Minutes")
                    }
                }

                if (activities.isNotEmpty()) {
                    Text("Link Activity (Optional)", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            androidx.compose.material3.FilterChip(
                                selected = linkedActivityId == null,
                                onClick = { linkedActivityId = null },
                                label = { Text("None") }
                            )
                        }
                        activities.forEach { act ->
                            item {
                                androidx.compose.material3.FilterChip(
                                    selected = linkedActivityId == act.id,
                                    onClick = { linkedActivityId = act.id; category = act.category },
                                    label = { Text(act.name) }
                                )
                            }
                        }
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
                modifier = Modifier.testTag("save_task_button")
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
