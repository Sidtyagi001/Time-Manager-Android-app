package com.example.ui.history

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.example.data.local.entity.ActivitySessionEntity
import com.example.domain.time.TimeFormatUtils
import com.example.ui.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allSessions by viewModel.allSessions.collectAsState()
    val activities by viewModel.activeActivities.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val is24Hour = userSettings?.is24HourFormat ?: true

    var searchQuery by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ActivitySessionEntity?>(null) }
    var sessionToDelete by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    val filteredSessions = remember(allSessions, searchQuery) {
        allSessions.filter { s ->
            s.activityNameSnapshot.contains(searchQuery, ignoreCase = true) ||
            s.categorySnapshot.contains(searchQuery, ignoreCase = true) ||
            s.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("history_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualDialog = true },
                modifier = Modifier.testTag("add_manual_session_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Missed Session")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("search_history_input"),
                    placeholder = { Text("Search sessions by activity or note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded sessions found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        SessionItemCard(
                            session = session,
                            is24Hour = is24Hour,
                            onEdit = { sessionToEdit = session },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showManualDialog || sessionToEdit != null) {
        ManualSessionDialog(
            initial = sessionToEdit,
            activities = activities,
            onDismiss = {
                showManualDialog = false
                sessionToEdit = null
            },
            onSave = { actId, actName, cat, colorHex, start, end, isBreak, notes, id ->
                viewModel.saveManualSession(
                    activityId = actId,
                    activityName = actName,
                    category = cat,
                    colorHex = colorHex,
                    startTime = start,
                    endTime = end,
                    isBreak = isBreak,
                    notes = notes,
                    sessionId = id
                )
                showManualDialog = false
                sessionToEdit = null
            }
        )
    }

    if (sessionToDelete != null) {
        val target = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session Record") },
            text = { Text("Are you sure you want to permanently delete this ${target.activityNameSnapshot} session?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(target.id)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionItemCard(
    session: ActivitySessionEntity,
    is24Hour: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val startStr = TimeFormatUtils.formatDateTime(session.startTime, is24Hour)
    val endStr = if (session.endTime > 0) TimeFormatUtils.formatDateTime(session.endTime, is24Hour) else "In Progress"
    val durationMillis = if (session.endTime > 0) max(0L, session.endTime - session.startTime) else (System.currentTimeMillis() - session.startTime)
    val mins = durationMillis / 60000

    val color = try {
        Color(android.graphics.Color.parseColor(session.colorHexSnapshot))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_item_${session.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.activityNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (session.isBreak) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                "Break",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Text(
                    text = "$startStr → $endStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.notes.isNotBlank()) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "${mins}m",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSessionDialog(
    initial: ActivitySessionEntity?,
    activities: List<com.example.data.local.entity.ActivityEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Long, Long, Boolean, String, Long) -> Unit
) {
    var selectedActivity by remember {
        mutableStateOf(activities.firstOrNull { it.id == initial?.activityId } ?: activities.firstOrNull())
    }
    var durationMinutes by remember {
        mutableIntStateOf(
            if (initial != null && initial.endTime > initial.startTime) {
                ((initial.endTime - initial.startTime) / 60000).toInt()
            } else 45
        )
    }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var isBreak by remember { mutableStateOf(initial?.isBreak ?: false) }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Log Missed Session" else "Edit Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Activity selector dropdown
                if (activities.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedActivity?.name ?: "Select Activity",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Activity") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            activities.forEach { act ->
                                DropdownMenuItem(
                                    text = { Text(act.name) },
                                    onClick = {
                                        selectedActivity = act
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = durationMinutes.toString(),
                    onValueChange = { durationMinutes = it.toIntOrNull() ?: 1 },
                    label = { Text("Duration (Minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("session_duration_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBreak, onCheckedChange = { isBreak = it })
                    Text("Explicit Break / Rest Time")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val act = selectedActivity
                    if (act != null && durationMinutes > 0) {
                        val end = if (initial != null && initial.endTime > 0) initial.endTime else System.currentTimeMillis()
                        val start = end - (durationMinutes * 60 * 1000L)
                        onSave(
                            act.id,
                            act.name,
                            act.category,
                            act.colorHex,
                            start,
                            end,
                            isBreak,
                            notes.trim(),
                            initial?.id ?: 0L
                        )
                    }
                },
                enabled = selectedActivity != null && durationMinutes > 0,
                modifier = Modifier.testTag("save_manual_session_button")
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
