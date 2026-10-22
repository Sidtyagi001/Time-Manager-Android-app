package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ActivitySessionEntity
import com.example.domain.time.TimeFormatUtils
import com.example.ui.MainViewModel
import java.time.Instant
import java.time.LocalDate
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

    // Group sessions chronologically by date
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)

    val groupedSessions = remember(filteredSessions) {
        filteredSessions.groupBy { session ->
            Instant.ofEpochMilli(session.startTime).atZone(zone).toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("history_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
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
            // Search Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("search_history_input"),
                    placeholder = { Text("Search sessions by activity, category, or note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recorded sessions found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedSessions.forEach { (date, sessionsForDate) ->
                        val dateHeader = when (date) {
                            today -> "Today"
                            yesterday -> "Yesterday"
                            else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                        }

                        val totalDayMins = sessionsForDate.sumOf { s ->
                            val dur = if (s.endTime > 0) max(0L, s.endTime - s.startTime) else max(0L, System.currentTimeMillis() - s.startTime)
                            dur / 60000L
                        }
                        val dayHours = totalDayMins / 60.0

                        item(key = "header_$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${String.format("%.1f", dayHours)}h tracked",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        items(sessionsForDate, key = { it.id }) { session ->
                            SessionTimelineCard(
                                session = session,
                                is24Hour = is24Hour,
                                onEdit = { sessionToEdit = session },
                                onDelete = { sessionToDelete = session }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(84.dp)) }
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
            title = { Text("Delete Session Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this ${target.activityNameSnapshot} session?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(target.id)
                        sessionToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
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
fun SessionTimelineCard(
    session: ActivitySessionEntity,
    is24Hour: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val startStr = TimeFormatUtils.formatTime(session.startTime, is24Hour)
    val endStr = if (session.endTime > 0) TimeFormatUtils.formatTime(session.endTime, is24Hour) else "Now"
    val durationMillis = if (session.endTime > 0) max(0L, session.endTime - session.startTime) else (System.currentTimeMillis() - session.startTime)
    val mins = durationMillis / 60000
    val hours = mins / 60
    val remMins = mins % 60
    val durationFormatted = if (hours > 0) "${hours}h ${remMins}m" else "${mins}m"

    val color = try {
        Color(android.graphics.Color.parseColor(session.colorHexSnapshot))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_item_${session.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.endTime == 0L) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (session.endTime == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity color dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.activityNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = session.categorySnapshot,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (session.isBreak) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(
                                "Break",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (session.endTime == 0L) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$startStr → $endStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = durationFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit Session") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Session", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
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
        title = { Text(if (initial == null) "Log Missed Session" else "Edit Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            shape = RoundedCornerShape(12.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("session_duration_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isBreak = !isBreak }
                ) {
                    Checkbox(checked = isBreak, onCheckedChange = { isBreak = it })
                    Spacer(modifier = Modifier.width(4.dp))
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
                shape = RoundedCornerShape(10.dp),
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
