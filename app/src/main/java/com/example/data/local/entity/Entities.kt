package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activities",
    indices = [Index(value = ["name"], unique = false), Index(value = ["isDeleted"])]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val description: String = "",
    val colorHex: String = "#2563EB",
    val iconName: String = "timer",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "activity_sessions",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["activityId"]),
        Index(value = ["isBreak"])
    ]
)
data class ActivitySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: Long,
    val activityNameSnapshot: String,
    val categorySnapshot: String,
    val colorHexSnapshot: String = "#2563EB",
    val startTime: Long,
    val endTime: Long, // 0 if currently active
    val isBreak: Boolean = false,
    val notes: String = "",
    val timetableTaskId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timetable_tasks",
    indices = [
        Index(value = ["dateStr"]),
        Index(value = ["slotIndex"]),
        Index(value = ["linkedActivityId"])
    ]
)
data class TimetableTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateStr: String, // "YYYY-MM-DD"
    val slotIndex: Int, // 0 to 47 for 30-min blocks (0 = 00:00, 1 = 00:30, 2 = 01:00...)
    val durationMinutes: Int = 60, // 30 or 60
    val title: String,
    val description: String = "",
    val category: String = "General",
    val linkedActivityId: Long? = null,
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 0,
    val isRecurring: Boolean = false,
    val recurrenceDays: String = "", // e.g. "MON,TUE,WED,THU,FRI"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetable_templates")
data class TimetableTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateName: String, // "Weekday Routine", "Weekend Routine"
    val isWeekend: Boolean = false,
    val slotIndex: Int, // 0 to 47
    val durationMinutes: Int = 60,
    val title: String,
    val category: String = "General",
    val linkedActivityId: Long? = null
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["triggerTime"]),
        Index(value = ["isCompleted"]),
        Index(value = ["isCancelled"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerTime: Long, // epoch millis
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    val snoozeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_runs")
data class FocusRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val linkedActivityId: Long? = null,
    val linkedActivityName: String? = null,
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val warningSeconds: Int = 30,
    val startTime: Long,
    val endTime: Long? = null,
    val totalFocusSecondsCompleted: Long = 0,
    val totalBreakSecondsCompleted: Long = 0,
    val cyclesCompleted: Int = 0,
    val isFinished: Boolean = false
)

@Entity(
    tableName = "focus_phases",
    indices = [Index(value = ["runId"])]
)
data class FocusPhaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val phaseType: String, // "FOCUS" or "BREAK"
    val targetDurationSeconds: Int,
    val startedAt: Long,
    val endedAt: Long? = null,
    val isCompleted: Boolean = false
)

@Entity(tableName = "daily_reviews")
data class DailyReviewEntity(
    @PrimaryKey
    val dateStr: String, // "YYYY-MM-DD"
    val notes: String = "",
    val rating: Int = 0, // 1 to 5
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isDarkMode: Boolean? = null, // null for system default
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val warningDurationSeconds: Int = 30,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val weekStartDay: Int = 1, // 1 = Monday, 7 = Sunday (java.time.DayOfWeek)
    val is24HourFormat: Boolean = true,
    val trackingStartTimestamp: Long = 0L,
    val alarmRingtoneEnabled: Boolean = true,
    val themeId: String = "SYSTEM"
)
