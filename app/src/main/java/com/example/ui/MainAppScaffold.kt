package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.activities.ActivitiesScreen
import com.example.ui.backup.BackupRestoreScreen
import com.example.ui.export.ExportScreen
import com.example.ui.focus.FocusScreen
import com.example.ui.history.SessionHistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Screen
import com.example.ui.reminders.RemindersScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.timetable.TimetableScreen

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activeSession by viewModel.activeSession.collectAsState()
    val currentTime by viewModel.currentTimeMillis.collectAsState()

    val navItems = listOf(
        NavItem(Screen.Home, Icons.Default.Home, "Home"),
        NavItem(Screen.Activities, Icons.Default.ListAlt, "Activities"),
        NavItem(Screen.Timetable, Icons.Default.CalendarMonth, "Timetable"),
        NavItem(Screen.Focus, Icons.Default.HourglassTop, "Focus"),
        NavItem(Screen.Reports, Icons.Default.Assessment, "Reports")
    )

    val isTopLevel = navItems.any { it.screen.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            Screen.Home.route -> "Time Manager"
                            Screen.Activities.route -> "Activities"
                            Screen.Timetable.route -> "24-Hour Timetable"
                            Screen.Focus.route -> "Focus Mode"
                            Screen.Reports.route -> "Productivity Reports"
                            Screen.Reminders.route -> "Reminders & Alarms"
                            Screen.History.route -> "Session History"
                            Screen.Backup.route -> "Backup & Restore"
                            Screen.Export.route -> "Excel Export"
                            Screen.Settings.route -> "Settings"
                            else -> "Time Manager"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.Reminders.route) },
                        modifier = Modifier.testTag("nav_reminders_button")
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = "Reminders")
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.History.route) },
                        modifier = Modifier.testTag("nav_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.Backup.route) },
                        modifier = Modifier.testTag("nav_backup_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Backup")
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.Export.route) },
                        modifier = Modifier.testTag("nav_export_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                // Persistent Active Session Mini Banner if tracking and not on Home
                if (activeSession != null && currentRoute != Screen.Home.route) {
                    val session = activeSession!!
                    val elapsedSeconds = (currentTime - session.startTime).coerceAtLeast(0L) / 1000L
                    val hours = elapsedSeconds / 3600
                    val mins = (elapsedSeconds % 3600) / 60
                    val secs = elapsedSeconds % 60
                    val elapsedStr = String.format("%02d:%02d:%02d", hours, mins, secs)

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = session.activityNameSnapshot,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = elapsedStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.stopTracking() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }

                // Bottom Navigation Bar
                NavigationBar {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_tab_${item.screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToActivities = { navController.navigate(Screen.Activities.route) },
                    onNavigateToTimetable = { navController.navigate(Screen.Timetable.route) },
                    onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                    onNavigateToReminders = { navController.navigate(Screen.Reminders.route) }
                )
            }
            composable(Screen.Activities.route) {
                ActivitiesScreen(viewModel = viewModel)
            }
            composable(Screen.Timetable.route) {
                TimetableScreen(viewModel = viewModel)
            }
            composable(Screen.Focus.route) {
                FocusScreen(viewModel = viewModel)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateToExport = { navController.navigate(Screen.Export.route) }
                )
            }
            composable(Screen.Reminders.route) {
                RemindersScreen(viewModel = viewModel)
            }
            composable(Screen.History.route) {
                SessionHistoryScreen(viewModel = viewModel)
            }
            composable(Screen.Backup.route) {
                BackupRestoreScreen(viewModel = viewModel)
            }
            composable(Screen.Export.route) {
                ExportScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
