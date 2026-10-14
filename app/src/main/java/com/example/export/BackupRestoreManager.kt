package com.example.export

import android.content.Context
import androidx.room.withTransaction
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.DailyReviewEntity
import com.example.data.local.entity.FocusPhaseEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.data.local.entity.TimetableTemplateEntity
import com.example.data.local.entity.UserSettingsEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val schemaVersion: Int = 0,
    val backupTimestamp: Long = 0L,
    val activityCount: Int = 0,
    val sessionCount: Int = 0,
    val taskCount: Int = 0,
    val templateCount: Int = 0,
    val reminderCount: Int = 0,
    val focusRunCount: Int = 0
)

data class RestoreResult(
    val isSuccess: Boolean,
    val message: String,
    val restoredCounts: Map<String, Int> = emptyMap()
)

class BackupRestoreManager(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context)
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_IDENTIFIER = "TimeManager"
    }

    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        val now = System.currentTimeMillis()
        root.put("backupTimestamp", now)

        // 1. Activities
        val activities = db.activityDao().getAllRaw()
        val actArray = JSONArray()
        for (a in activities) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("name", a.name)
            obj.put("category", a.category)
            obj.put("description", a.description)
            obj.put("colorHex", a.colorHex)
            obj.put("iconName", a.iconName)
            obj.put("isFavorite", a.isFavorite)
            obj.put("isArchived", a.isArchived)
            obj.put("isDeleted", a.isDeleted)
            obj.put("createdAt", a.createdAt)
            obj.put("updatedAt", a.updatedAt)
            actArray.put(obj)
        }
        root.put("activities", actArray)

        // 2. Activity Sessions
        val sessions = db.activitySessionDao().getAllSessionsList()
        val sessArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("activityId", s.activityId)
            obj.put("activityNameSnapshot", s.activityNameSnapshot)
            obj.put("categorySnapshot", s.categorySnapshot)
            obj.put("colorHexSnapshot", s.colorHexSnapshot)
            obj.put("startTime", s.startTime)
            obj.put("endTime", s.endTime)
            obj.put("isBreak", s.isBreak)
            obj.put("notes", s.notes)
            if (s.timetableTaskId != null) obj.put("timetableTaskId", s.timetableTaskId)
            obj.put("createdAt", s.createdAt)
            sessArray.put(obj)
        }
        root.put("activitySessions", sessArray)

        // 3. Timetable Tasks
        val tasks = db.timetableDao().getAllTasks()
        val taskArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("dateStr", t.dateStr)
            obj.put("slotIndex", t.slotIndex)
            obj.put("durationMinutes", t.durationMinutes)
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("category", t.category)
            if (t.linkedActivityId != null) obj.put("linkedActivityId", t.linkedActivityId)
            obj.put("isCompleted", t.isCompleted)
            obj.put("hasReminder", t.hasReminder)
            obj.put("reminderMinutesBefore", t.reminderMinutesBefore)
            obj.put("isRecurring", t.isRecurring)
            obj.put("recurrenceDays", t.recurrenceDays)
            obj.put("createdAt", t.createdAt)
            taskArray.put(obj)
        }
        root.put("timetableTasks", taskArray)

        // 4. Timetable Templates
        val templates = db.timetableDao().getAllTemplates()
        val tmplArray = JSONArray()
        for (t in templates) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("templateName", t.templateName)
            obj.put("isWeekend", t.isWeekend)
            obj.put("slotIndex", t.slotIndex)
            obj.put("durationMinutes", t.durationMinutes)
            obj.put("title", t.title)
            obj.put("category", t.category)
            if (t.linkedActivityId != null) obj.put("linkedActivityId", t.linkedActivityId)
            tmplArray.put(obj)
        }
        root.put("timetableTemplates", tmplArray)

        // 5. Reminders
        val reminders = db.reminderDao().getAllRemindersList()
        val remArray = JSONArray()
        for (r in reminders) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("title", r.title)
            obj.put("description", r.description)
            obj.put("triggerTime", r.triggerTime)
            obj.put("isSoundEnabled", r.isSoundEnabled)
            obj.put("isVibrationEnabled", r.isVibrationEnabled)
            obj.put("isCompleted", r.isCompleted)
            obj.put("isCancelled", r.isCancelled)
            obj.put("snoozeCount", r.snoozeCount)
            obj.put("createdAt", r.createdAt)
            remArray.put(obj)
        }
        root.put("reminders", remArray)

        // 6. Focus Runs
        val focusRuns = db.focusDao().getAllRunsList()
        val focusArray = JSONArray()
        for (f in focusRuns) {
            val obj = JSONObject()
            obj.put("id", f.id)
            if (f.linkedActivityId != null) obj.put("linkedActivityId", f.linkedActivityId)
            if (f.linkedActivityName != null) obj.put("linkedActivityName", f.linkedActivityName)
            obj.put("focusDurationMinutes", f.focusDurationMinutes)
            obj.put("breakDurationMinutes", f.breakDurationMinutes)
            obj.put("warningSeconds", f.warningSeconds)
            obj.put("startTime", f.startTime)
            if (f.endTime != null) obj.put("endTime", f.endTime)
            obj.put("totalFocusSecondsCompleted", f.totalFocusSecondsCompleted)
            obj.put("totalBreakSecondsCompleted", f.totalBreakSecondsCompleted)
            obj.put("cyclesCompleted", f.cyclesCompleted)
            obj.put("isFinished", f.isFinished)
            focusArray.put(obj)
        }
        root.put("focusRuns", focusArray)

        // 7. Focus Phases
        val phases = db.focusDao().getAllPhasesList()
        val phaseArray = JSONArray()
        for (p in phases) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("runId", p.runId)
            obj.put("phaseType", p.phaseType)
            obj.put("targetDurationSeconds", p.targetDurationSeconds)
            obj.put("startedAt", p.startedAt)
            if (p.endedAt != null) obj.put("endedAt", p.endedAt)
            obj.put("isCompleted", p.isCompleted)
            phaseArray.put(obj)
        }
        root.put("focusPhases", phaseArray)

        // 8. Daily Reviews
        val reviews = db.dailyReviewDao().getAllReviews()
        val reviewArray = JSONArray()
        for (r in reviews) {
            val obj = JSONObject()
            obj.put("dateStr", r.dateStr)
            obj.put("notes", r.notes)
            obj.put("rating", r.rating)
            obj.put("createdAt", r.createdAt)
            obj.put("updatedAt", r.updatedAt)
            reviewArray.put(obj)
        }
        root.put("dailyReviews", reviewArray)

        // 9. User Settings
        val settings = db.userSettingsDao().getSettings() ?: UserSettingsEntity()
        root.put("trackingStartTimestamp", settings.trackingStartTimestamp)
        val settingsObj = JSONObject()
        if (settings.isDarkMode != null) settingsObj.put("isDarkMode", settings.isDarkMode)
        settingsObj.put("focusDurationMinutes", settings.focusDurationMinutes)
        settingsObj.put("breakDurationMinutes", settings.breakDurationMinutes)
        settingsObj.put("warningDurationSeconds", settings.warningDurationSeconds)
        settingsObj.put("soundEnabled", settings.soundEnabled)
        settingsObj.put("vibrationEnabled", settings.vibrationEnabled)
        settingsObj.put("weekStartDay", settings.weekStartDay)
        settingsObj.put("is24HourFormat", settings.is24HourFormat)
        settingsObj.put("trackingStartTimestamp", settings.trackingStartTimestamp)
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    suspend fun writeBackup(outputStream: OutputStream) {
        val json = createBackupJson()
        outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    fun validateBackup(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            val app = root.optString("app")
            if (app != APP_IDENTIFIER) {
                return BackupValidationResult(isValid = false, errorMessage = "Unsupported application backup: $app")
            }
            val schemaVersion = root.optInt("schemaVersion", 0)
            if (schemaVersion != CURRENT_SCHEMA_VERSION) {
                return BackupValidationResult(isValid = false, errorMessage = "Incompatible backup version: $schemaVersion (expected $CURRENT_SCHEMA_VERSION)")
            }

            val timestamp = root.optLong("backupTimestamp", 0L)
            val actCount = root.optJSONArray("activities")?.length() ?: 0
            val sessCount = root.optJSONArray("activitySessions")?.length() ?: 0
            val taskCount = root.optJSONArray("timetableTasks")?.length() ?: 0
            val tmplCount = root.optJSONArray("timetableTemplates")?.length() ?: 0
            val remCount = root.optJSONArray("reminders")?.length() ?: 0
            val focusCount = root.optJSONArray("focusRuns")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                schemaVersion = schemaVersion,
                backupTimestamp = timestamp,
                activityCount = actCount,
                sessionCount = sessCount,
                taskCount = taskCount,
                templateCount = tmplCount,
                reminderCount = remCount,
                focusRunCount = focusCount
            )
        } catch (e: Exception) {
            BackupValidationResult(isValid = false, errorMessage = "Corrupt JSON: ${e.message}")
        }
    }

    suspend fun restoreBackup(inputStream: InputStream): RestoreResult {
        val jsonString = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        return restoreFromJsonString(jsonString)
    }

    suspend fun restoreFromJsonString(jsonString: String): RestoreResult {
        val validation = validateBackup(jsonString)
        if (!validation.isValid) {
            return RestoreResult(isSuccess = false, message = validation.errorMessage ?: "Invalid backup file")
        }

        val root = JSONObject(jsonString)

        // Parse entities before touching the database
        val actList = mutableListOf<ActivityEntity>()
        val actArray = root.optJSONArray("activities") ?: JSONArray()
        for (i in 0 until actArray.length()) {
            val obj = actArray.getJSONObject(i)
            actList.add(
                ActivityEntity(
                    id = obj.optLong("id"),
                    name = obj.getString("name"),
                    category = obj.getString("category"),
                    description = obj.optString("description", ""),
                    colorHex = obj.optString("colorHex", "#2563EB"),
                    iconName = obj.optString("iconName", "timer"),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val sessList = mutableListOf<ActivitySessionEntity>()
        val sessArray = root.optJSONArray("activitySessions") ?: JSONArray()
        for (i in 0 until sessArray.length()) {
            val obj = sessArray.getJSONObject(i)
            sessList.add(
                ActivitySessionEntity(
                    id = obj.optLong("id"),
                    activityId = obj.getLong("activityId"),
                    activityNameSnapshot = obj.getString("activityNameSnapshot"),
                    categorySnapshot = obj.getString("categorySnapshot"),
                    colorHexSnapshot = obj.optString("colorHexSnapshot", "#2563EB"),
                    startTime = obj.getLong("startTime"),
                    endTime = obj.getLong("endTime"),
                    isBreak = obj.optBoolean("isBreak", false),
                    notes = obj.optString("notes", ""),
                    timetableTaskId = if (obj.has("timetableTaskId")) obj.getLong("timetableTaskId") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val taskList = mutableListOf<TimetableTaskEntity>()
        val taskArray = root.optJSONArray("timetableTasks") ?: JSONArray()
        for (i in 0 until taskArray.length()) {
            val obj = taskArray.getJSONObject(i)
            taskList.add(
                TimetableTaskEntity(
                    id = obj.optLong("id"),
                    dateStr = obj.getString("dateStr"),
                    slotIndex = obj.getInt("slotIndex"),
                    durationMinutes = obj.optInt("durationMinutes", 60),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "General"),
                    linkedActivityId = if (obj.has("linkedActivityId")) obj.getLong("linkedActivityId") else null,
                    isCompleted = obj.optBoolean("isCompleted", false),
                    hasReminder = obj.optBoolean("hasReminder", false),
                    reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 0),
                    isRecurring = obj.optBoolean("isRecurring", false),
                    recurrenceDays = obj.optString("recurrenceDays", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val tmplList = mutableListOf<TimetableTemplateEntity>()
        val tmplArray = root.optJSONArray("timetableTemplates") ?: JSONArray()
        for (i in 0 until tmplArray.length()) {
            val obj = tmplArray.getJSONObject(i)
            tmplList.add(
                TimetableTemplateEntity(
                    id = obj.optLong("id"),
                    templateName = obj.optString("templateName", "Default"),
                    isWeekend = obj.optBoolean("isWeekend", false),
                    slotIndex = obj.getInt("slotIndex"),
                    durationMinutes = obj.optInt("durationMinutes", 60),
                    title = obj.getString("title"),
                    category = obj.optString("category", "General"),
                    linkedActivityId = if (obj.has("linkedActivityId")) obj.getLong("linkedActivityId") else null
                )
            )
        }

        val remList = mutableListOf<ReminderEntity>()
        val remArray = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until remArray.length()) {
            val obj = remArray.getJSONObject(i)
            remList.add(
                ReminderEntity(
                    id = obj.optLong("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    triggerTime = obj.getLong("triggerTime"),
                    isSoundEnabled = obj.optBoolean("isSoundEnabled", true),
                    isVibrationEnabled = obj.optBoolean("isVibrationEnabled", true),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isCancelled = obj.optBoolean("isCancelled", false),
                    snoozeCount = obj.optInt("snoozeCount", 0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val focusList = mutableListOf<FocusRunEntity>()
        val focusArray = root.optJSONArray("focusRuns") ?: JSONArray()
        for (i in 0 until focusArray.length()) {
            val obj = focusArray.getJSONObject(i)
            focusList.add(
                FocusRunEntity(
                    id = obj.optLong("id"),
                    linkedActivityId = if (obj.has("linkedActivityId")) obj.getLong("linkedActivityId") else null,
                    linkedActivityName = if (obj.has("linkedActivityName")) obj.getString("linkedActivityName") else null,
                    focusDurationMinutes = obj.optInt("focusDurationMinutes", 25),
                    breakDurationMinutes = obj.optInt("breakDurationMinutes", 5),
                    warningSeconds = obj.optInt("warningSeconds", 30),
                    startTime = obj.getLong("startTime"),
                    endTime = if (obj.has("endTime")) obj.getLong("endTime") else null,
                    totalFocusSecondsCompleted = obj.optLong("totalFocusSecondsCompleted", 0L),
                    totalBreakSecondsCompleted = obj.optLong("totalBreakSecondsCompleted", 0L),
                    cyclesCompleted = obj.optInt("cyclesCompleted", 0),
                    isFinished = obj.optBoolean("isFinished", true)
                )
            )
        }

        val phaseList = mutableListOf<FocusPhaseEntity>()
        val phaseArray = root.optJSONArray("focusPhases") ?: JSONArray()
        for (i in 0 until phaseArray.length()) {
            val obj = phaseArray.getJSONObject(i)
            phaseList.add(
                FocusPhaseEntity(
                    id = obj.optLong("id"),
                    runId = obj.getLong("runId"),
                    phaseType = obj.getString("phaseType"),
                    targetDurationSeconds = obj.getInt("targetDurationSeconds"),
                    startedAt = obj.getLong("startedAt"),
                    endedAt = if (obj.has("endedAt")) obj.getLong("endedAt") else null,
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
            )
        }

        val reviewList = mutableListOf<DailyReviewEntity>()
        val reviewArray = root.optJSONArray("dailyReviews") ?: JSONArray()
        for (i in 0 until reviewArray.length()) {
            val obj = reviewArray.getJSONObject(i)
            reviewList.add(
                DailyReviewEntity(
                    dateStr = obj.getString("dateStr"),
                    notes = obj.optString("notes", ""),
                    rating = obj.optInt("rating", 0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val settingsObj = root.optJSONObject("settings")
        val trackingStart = if (settingsObj != null && settingsObj.has("trackingStartTimestamp")) {
            settingsObj.getLong("trackingStartTimestamp")
        } else if (root.has("trackingStartTimestamp")) {
            root.getLong("trackingStartTimestamp")
        } else if (sessList.isNotEmpty()) {
            sessList.minOf { it.startTime }
        } else {
            root.optLong("backupTimestamp", System.currentTimeMillis())
        }

        val restoredSettings = UserSettingsEntity(
            id = 1,
            isDarkMode = if (settingsObj?.has("isDarkMode") == true) settingsObj.getBoolean("isDarkMode") else null,
            focusDurationMinutes = settingsObj?.optInt("focusDurationMinutes", 25) ?: 25,
            breakDurationMinutes = settingsObj?.optInt("breakDurationMinutes", 5) ?: 5,
            warningDurationSeconds = settingsObj?.optInt("warningDurationSeconds", 30) ?: 30,
            soundEnabled = settingsObj?.optBoolean("soundEnabled", true) ?: true,
            vibrationEnabled = settingsObj?.optBoolean("vibrationEnabled", true) ?: true,
            weekStartDay = settingsObj?.optInt("weekStartDay", 1) ?: 1,
            is24HourFormat = settingsObj?.optBoolean("is24HourFormat", true) ?: true,
            trackingStartTimestamp = trackingStart
        )

        // Execute transactional restore
        return try {
            db.withTransaction {
                // Cancel old pending alarms
                val oldReminders = db.reminderDao().getAllRemindersList()
                for (old in oldReminders) {
                    AlarmScheduler.cancelReminder(context, old.id)
                }

                // Delete current records
                db.activityDao().deleteAll()
                db.activitySessionDao().deleteAll()
                db.timetableDao().deleteAllTasks()
                db.timetableDao().deleteAllTemplates()
                db.reminderDao().deleteAll()
                db.focusDao().deleteAllRuns()
                db.focusDao().deleteAllPhases()
                db.dailyReviewDao().deleteAll()

                // Insert restored records
                db.activityDao().insertAll(actList)
                db.activitySessionDao().insertAll(sessList)
                db.timetableDao().insertTasks(taskList)
                db.timetableDao().insertTemplates(tmplList)
                db.reminderDao().insertAll(remList)
                db.focusDao().insertRuns(focusList)
                db.focusDao().insertPhases(phaseList)
                db.dailyReviewDao().insertAll(reviewList)
                if (restoredSettings != null) {
                    db.userSettingsDao().insertOrUpdate(restoredSettings)
                }
            }

            // Reconcile pending reminders (only schedule FUTURE, uncompleted, non-cancelled reminders)
            val now = System.currentTimeMillis()
            val futureReminders = db.reminderDao().getUpcomingActiveReminders(now)
            for (r in futureReminders) {
                AlarmScheduler.scheduleReminder(context, r)
            }

            RestoreResult(
                isSuccess = true,
                message = "Backup restored successfully!",
                restoredCounts = mapOf(
                    "activities" to actList.size,
                    "sessions" to sessList.size,
                    "tasks" to taskList.size,
                    "reminders" to remList.size,
                    "focusRuns" to focusList.size
                )
            )
        } catch (e: Exception) {
            RestoreResult(isSuccess = false, message = "Restore failed: ${e.message}")
        }
    }
}
