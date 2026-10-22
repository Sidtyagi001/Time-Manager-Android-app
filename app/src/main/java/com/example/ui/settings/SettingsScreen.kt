package com.example.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.RadioButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.alarm.AlarmScheduler
import com.example.data.local.entity.UserSettingsEntity
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsState by viewModel.userSettings.collectAsState()
    val settings = settingsState ?: UserSettingsEntity()

    val areNotificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val canScheduleExact = remember {
        AlarmScheduler.canScheduleExactAlarms(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences & Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Theme and Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance & Time Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // 10-Theme Palette Selector
                    ThemeSelectorSection(
                        currentThemeId = settings.themeId,
                        onSelectTheme = { theme ->
                            viewModel.saveUserSettings(settings.copy(themeId = theme.id))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("24-Hour Time Format", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val previewTime = com.example.domain.time.TimeFormatUtils.formatTime(14, 30, settings.is24HourFormat)
                            Text("Example: $previewTime (Display ${if (settings.is24HourFormat) "00:00 - 23:59" else "12-hour AM/PM"})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.is24HourFormat,
                            onCheckedChange = { viewModel.saveUserSettings(settings.copy(is24HourFormat = it)) },
                            modifier = Modifier.testTag("switch_24_hour_format")
                        )
                    }
                }
            }
        }

        // 2. Focus Mode Defaults
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Default Focus Cycle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Default Focus Duration: ${settings.focusDurationMinutes} min", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(20, 25, 30, 45, 50).forEach { mins ->
                            FilterChip(
                                selected = settings.focusDurationMinutes == mins,
                                onClick = { viewModel.saveUserSettings(settings.copy(focusDurationMinutes = mins)) },
                                label = { Text("$mins m") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Default Rest Break: ${settings.breakDurationMinutes} min", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10, 15).forEach { mins ->
                            FilterChip(
                                selected = settings.breakDurationMinutes == mins,
                                onClick = { viewModel.saveUserSettings(settings.copy(breakDurationMinutes = mins)) },
                                label = { Text("$mins m") }
                            )
                        }
                    }
                }
            }
        }

        // 3. Sound and Vibration
        item {
            val isAlarmPlaying by com.example.alarm.AlarmAudioPlayer.isPlayingState.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sound & Vibration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Normal notifications sound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Normal Notifications Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Plays the Android system-default notification sound for general alerts and focus transitions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = { viewModel.saveUserSettings(settings.copy(soundEnabled = it)) },
                            modifier = Modifier.testTag("switch_normal_notifications_sound")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dedicated alarm & reminder ringtone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Dedicated Alarm & Reminder Ringtone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Plays the built-in 12-second ringtone (reminder_alarm.mp3) exclusively for scheduled alarms and reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.alarmRingtoneEnabled,
                            onCheckedChange = { viewModel.saveUserSettings(settings.copy(alarmRingtoneEnabled = it)) },
                            modifier = Modifier.testTag("switch_alarm_ringtone")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Alarm Sound button
                    OutlinedButton(
                        onClick = {
                            if (isAlarmPlaying) {
                                com.example.alarm.AlarmAudioPlayer.stop()
                            } else {
                                com.example.alarm.AlarmAudioPlayer.playTest(context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_test_alarm_sound")
                    ) {
                        Icon(
                            if (isAlarmPlaying) Icons.Default.Close else Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAlarmPlaying) "Stop Test Sound" else "Test Alarm Sound (12s Ringtone)")
                    }

                    if (isAlarmPlaying) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Playing dedicated reminder_alarm.mp3 (auto-stops in ~12 seconds)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vibration alerts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Vibration Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Vibrates device when alarms and notifications trigger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.vibrationEnabled,
                            onCheckedChange = { viewModel.saveUserSettings(settings.copy(vibrationEnabled = it)) },
                            modifier = Modifier.testTag("switch_vibration_alerts")
                        )
                    }
                }
            }
        }

        // 4. Permissions Status & Shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Permissions Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Notifications status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (areNotificationsEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (areNotificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (areNotificationsEnabled) "Granted" else "Disabled in system settings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!areNotificationsEnabled) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Settings")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Exact alarm status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (canScheduleExact) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (canScheduleExact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Exact Alarms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (canScheduleExact) "Available" else "Requires permission on Android 12+",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            }
        }

        // 5. Offline & Privacy Guarantee Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100% Offline & Private", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Time Manager contains zero cloud accounts, external APIs, telemetry, or network calls. All tracking and schedule records remain strictly on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Version 1.0 • Build ID: personal.timemanager",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
fun ThemeSelectorSection(
    currentThemeId: String,
    onSelectTheme: (AppTheme) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Theme Gallery (${AppTheme.entries.size} Palettes)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap a palette to restyle the entire app immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 2-column or list gallery
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTheme.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { theme ->
                        val isSelected = theme.id.equals(currentThemeId, ignoreCase = true)
                        ThemeGalleryCard(
                            theme = theme,
                            isSelected = isSelected,
                            onClick = { onSelectTheme(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeGalleryCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("theme_card_${theme.id.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Miniature App UI Preview Tile
            MiniAppMockup(
                primaryColor = theme.primaryPreview,
                secondaryColor = theme.secondaryPreview,
                backgroundColor = theme.backgroundPreview
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (theme.isDarkOnly) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("DARK", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                } else if (theme.isLightOnly) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("LIGHT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                        Text("AUTO", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Mini Color Swatches
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.primaryPreview))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.secondaryPreview))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.backgroundPreview).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape))
                }
            }
        }
    }
}

@Composable
fun MiniAppMockup(
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header bar simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                )
            }

            // Hero card simulation
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = primaryColor.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(primaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(primaryColor)
                    )
                }
            }

            // Chart bars simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(modifier = Modifier.width(5.dp).height(8.dp).clip(RoundedCornerShape(1.dp)).background(primaryColor))
                Box(modifier = Modifier.width(5.dp).height(14.dp).clip(RoundedCornerShape(1.dp)).background(primaryColor))
                Box(modifier = Modifier.width(5.dp).height(6.dp).clip(RoundedCornerShape(1.dp)).background(secondaryColor))
                Box(modifier = Modifier.width(5.dp).height(16.dp).clip(RoundedCornerShape(1.dp)).background(primaryColor))
            }
        }
    }
}
