package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.DailyReviewEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.data.local.entity.TimetableTemplateEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.domain.focus.FocusEngine
import com.example.domain.focus.FocusPhaseType
import com.example.domain.time.DayTimeReport
import com.example.domain.time.TimeAccountingEngine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.max

class TimeManagerRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context)
) {
    val activityDao = db.activityDao()
    val sessionDao = db.activitySessionDao()
    val timetableDao = db.timetableDao()
    val reminderDao = db.reminderDao()
    val focusDao = db.focusDao()
    val reviewDao = db.dailyReviewDao()
    val settingsDao = db.userSettingsDao()

    // Activities
    fun getAllActiveActivities(): Flow<List<ActivityEntity>> = activityDao.getAllActive()
    fun getAllActivities(): Flow<List<ActivityEntity>> = activityDao.getAll()
    fun getFavoriteActivities(): Flow<List<ActivityEntity>> = activityDao.getFavorites()
    suspend fun getActivityById(id: Long): ActivityEntity? = activityDao.getById(id)

    suspend fun saveActivity(activity: ActivityEntity): Long {
        return if (activity.id == 0L) {
            activityDao.insert(activity.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            activityDao.update(activity.copy(updatedAt = System.currentTimeMillis()))
            activity.id
        }
    }

    suspend fun softDeleteActivity(id: Long) {
        activityDao.softDelete(id)
    }

    suspend fun archiveActivity(id: Long, isArchived: Boolean) {
        activityDao.archive(id, isArchived)
    }

    // Sessions & Tracking
    fun getAllSessions(): Flow<List<ActivitySessionEntity>> = sessionDao.getAllSessions()
    fun getActiveSessionFlow(): Flow<ActivitySessionEntity?> = sessionDao.getActiveSessionFlow()
    suspend fun getActiveSession(): ActivitySessionEntity? = sessionDao.getActiveSession()

    suspend fun startTracking(activity: ActivityEntity, isBreak: Boolean = false, notes: String = ""): ActivitySessionEntity {
        return db.withTransaction {
            val now = System.currentTimeMillis()
            // Stop any currently running session
            val active = sessionDao.getActiveSession()
            if (active != null) {
                sessionDao.update(active.copy(endTime = now))
            }

            val newSession = ActivitySessionEntity(
                activityId = activity.id,
                activityNameSnapshot = activity.name,
                categorySnapshot = activity.category,
                colorHexSnapshot = activity.colorHex,
                startTime = now,
                endTime = 0L,
                isBreak = isBreak,
                notes = notes,
                createdAt = now
            )
            val newId = sessionDao.insert(newSession)
            newSession.copy(id = newId)
        }
    }

    suspend fun stopTracking(): ActivitySessionEntity? {
        return db.withTransaction {
            val active = sessionDao.getActiveSession()
            if (active != null) {
                val now = System.currentTimeMillis()
                val updated = active.copy(endTime = max(active.startTime + 1000L, now))
                sessionDao.update(updated)
                updated
            } else {
                null
            }
        }
    }

    suspend fun saveManualSession(
        activityId: Long,
        activityName: String,
        category: String,
        colorHex: String,
        startTime: Long,
        endTime: Long,
        isBreak: Boolean,
        notes: String,
        sessionId: Long = 0L
    ): Long {
        require(endTime >= startTime) { "End time cannot be before start time" }
        val session = ActivitySessionEntity(
            id = sessionId,
            activityId = activityId,
            activityNameSnapshot = activityName,
            categorySnapshot = category,
            colorHexSnapshot = colorHex,
            startTime = startTime,
            endTime = endTime,
            isBreak = isBreak,
            notes = notes,
            createdAt = if (sessionId == 0L) System.currentTimeMillis() else startTime
        )
        return if (sessionId == 0L) {
            sessionDao.insert(session)
        } else {
            sessionDao.update(session)
            sessionId
        }
    }

    suspend fun deleteSession(id: Long) {
        sessionDao.deleteById(id)
    }

    // Reports & Time Accounting
    suspend fun getDayReport(dateStr: String, now: Long = System.currentTimeMillis()): DayTimeReport {
        val allSessions = sessionDao.getAllSessionsList()
        val tasks = timetableDao.getTasksForDateSync(dateStr)
        val settings = getUserSettings()
        return TimeAccountingEngine.calculateDayReport(
            dateStr = dateStr,
            allSessions = allSessions,
            timetableTasks = tasks,
            now = now,
            trackingStartTimestamp = settings.trackingStartTimestamp
        )
    }

    suspend fun getMultiDayReports(startDate: LocalDate, endDate: LocalDate, now: Long = System.currentTimeMillis()): List<DayTimeReport> {
        val allSessions = sessionDao.getAllSessionsList()
        val tasks = timetableDao.getAllTasks()
        val settings = getUserSettings()
        return TimeAccountingEngine.calculateMultiDayReports(
            startDate = startDate,
            endDate = endDate,
            allSessions = allSessions,
            timetableTasks = tasks,
            now = now,
            trackingStartTimestamp = settings.trackingStartTimestamp
        )
    }

    // Timetable
    fun getTasksForDateFlow(dateStr: String): Flow<List<TimetableTaskEntity>> = timetableDao.getTasksForDate(dateStr)
    suspend fun getTasksForDate(dateStr: String): List<TimetableTaskEntity> = timetableDao.getTasksForDateSync(dateStr)

    suspend fun saveTimetableTask(task: TimetableTaskEntity): Long {
        return if (task.id == 0L) {
            timetableDao.insertTask(task)
        } else {
            timetableDao.updateTask(task)
            task.id
        }
    }

    suspend fun deleteTimetableTask(taskId: Long) {
        timetableDao.deleteTaskById(taskId)
    }

    /**
     * Splits a 1-hour block (durationMinutes == 60) into two 30-minute blocks.
     * Preserves the original hour slot exactly without gaps or overlaps.
     */
    suspend fun splitTaskSlot(task: TimetableTaskEntity): Pair<TimetableTaskEntity, TimetableTaskEntity> {
        require(task.durationMinutes == 60) { "Can only split 60-minute blocks" }
        return db.withTransaction {
            val updatedFirst = task.copy(durationMinutes = 30)
            timetableDao.updateTask(updatedFirst)

            val secondHalf = TimetableTaskEntity(
                dateStr = task.dateStr,
                slotIndex = task.slotIndex + 1,
                durationMinutes = 30,
                title = "${task.title} (Part 2)",
                description = task.description,
                category = task.category,
                linkedActivityId = task.linkedActivityId,
                isCompleted = false,
                hasReminder = task.hasReminder,
                reminderMinutesBefore = task.reminderMinutesBefore,
                isRecurring = task.isRecurring,
                recurrenceDays = task.recurrenceDays
            )
            val secondId = timetableDao.insertTask(secondHalf)
            Pair(updatedFirst, secondHalf.copy(id = secondId))
        }
    }

    suspend fun copyTimetable(fromDateStr: String, toDateStr: String) {
        db.withTransaction {
            val sourceTasks = timetableDao.getTasksForDateSync(fromDateStr)
            val newTasks = sourceTasks.map {
                it.copy(id = 0L, dateStr = toDateStr, isCompleted = false, createdAt = System.currentTimeMillis())
            }
            timetableDao.deleteTasksForDate(toDateStr)
            timetableDao.insertTasks(newTasks)
        }
    }

    suspend fun applyTemplate(toDateStr: String, isWeekend: Boolean) {
        db.withTransaction {
            val templates = timetableDao.getTemplatesForWeekend(isWeekend)
            val tasks = templates.map { t ->
                TimetableTaskEntity(
                    dateStr = toDateStr,
                    slotIndex = t.slotIndex,
                    durationMinutes = t.durationMinutes,
                    title = t.title,
                    category = t.category,
                    linkedActivityId = t.linkedActivityId,
                    createdAt = System.currentTimeMillis()
                )
            }
            timetableDao.deleteTasksForDate(toDateStr)
            timetableDao.insertTasks(tasks)
        }
    }

    suspend fun getTemplatesForWeekend(isWeekend: Boolean): List<TimetableTemplateEntity> {
        return timetableDao.getTemplatesForWeekend(isWeekend)
    }

    suspend fun saveTemplate(template: TimetableTemplateEntity): Long {
        return timetableDao.insertTemplate(template)
    }

    // Reminders
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>> = reminderDao.getAllRemindersFlow()

    suspend fun saveReminder(reminder: ReminderEntity): Long {
        val id = if (reminder.id == 0L) {
            reminderDao.insert(reminder)
        } else {
            AlarmScheduler.cancelReminder(context, reminder.id)
            reminderDao.update(reminder)
            reminder.id
        }
        val saved = reminderDao.getById(id)
        if (saved != null && !saved.isCompleted && !saved.isCancelled && saved.triggerTime > System.currentTimeMillis()) {
            AlarmScheduler.scheduleReminder(context, saved)
        }
        return id
    }

    suspend fun cancelOrDismissReminder(reminderId: Long, isDismiss: Boolean) {
        val reminder = reminderDao.getById(reminderId) ?: return
        AlarmScheduler.cancelReminder(context, reminderId)
        val updated = reminder.copy(
            isCompleted = isDismiss,
            isCancelled = !isDismiss
        )
        reminderDao.update(updated)
    }

    suspend fun deleteReminder(reminderId: Long) {
        AlarmScheduler.cancelReminder(context, reminderId)
        reminderDao.deleteById(reminderId)
    }

    // Focus Mode
    fun getActiveFocusRunFlow(): Flow<FocusRunEntity?> = focusDao.getActiveRunFlow()
    fun getAllFocusRunsFlow(): Flow<List<FocusRunEntity>> = focusDao.getAllRunsFlow()
    suspend fun getActiveFocusRun(): FocusRunEntity? = focusDao.getActiveRun()

    suspend fun startFocusRun(
        linkedActivityId: Long? = null,
        linkedActivityName: String? = null,
        focusDurationMinutes: Int = 25,
        breakDurationMinutes: Int = 5,
        warningSeconds: Int = 30
    ): FocusRunEntity {
        return db.withTransaction {
            val existing = focusDao.getActiveRun()
            if (existing != null) {
                stopFocusRun(existing.id)
            }

            val now = System.currentTimeMillis()
            val run = FocusRunEntity(
                linkedActivityId = linkedActivityId,
                linkedActivityName = linkedActivityName,
                focusDurationMinutes = focusDurationMinutes,
                breakDurationMinutes = breakDurationMinutes,
                warningSeconds = warningSeconds,
                startTime = now,
                isFinished = false
            )
            val runId = focusDao.insertRun(run)
            val savedRun = run.copy(id = runId)

            val state = FocusEngine.reconcile(savedRun, now)
            val nextPhase = if (state.phase == FocusPhaseType.FOCUS) "BREAK" else "FOCUS"
            AlarmScheduler.scheduleFocusTransition(context, runId, state.phaseEndTime, nextPhase)
            if (warningSeconds > 0) {
                val warnTime = state.phaseEndTime - (warningSeconds * 1000L)
                if (warnTime > now) {
                    AlarmScheduler.scheduleFocusWarning(context, runId, warnTime)
                }
            }
            savedRun
        }
    }

    suspend fun stopFocusRun(runId: Long) {
        db.withTransaction {
            val run = focusDao.getRunById(runId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            val state = FocusEngine.reconcile(run, now)
            focusDao.updateRun(
                run.copy(
                    isFinished = true,
                    endTime = now,
                    totalFocusSecondsCompleted = state.totalFocusSecondsCompleted,
                    totalBreakSecondsCompleted = state.totalBreakSecondsCompleted,
                    cyclesCompleted = state.cycleNumber
                )
            )
            AlarmScheduler.cancelFocusAlarms(context, runId)
        }
    }

    // Daily Review
    fun getReviewForDateFlow(dateStr: String): Flow<DailyReviewEntity?> = reviewDao.getReviewForDate(dateStr)
    suspend fun saveDailyReview(dateStr: String, notes: String, rating: Int) {
        reviewDao.insertOrUpdate(
            DailyReviewEntity(
                dateStr = dateStr,
                notes = notes,
                rating = rating,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Settings
    fun getUserSettingsFlow(): Flow<UserSettingsEntity?> = settingsDao.getSettingsFlow()

    suspend fun getUserSettings(): UserSettingsEntity {
        val existing = settingsDao.getSettings()
        return if (existing != null) {
            if (existing.trackingStartTimestamp <= 0L) {
                val earliest = sessionDao.getAllSessionsList().minOfOrNull { it.startTime }
                val startTs = earliest ?: System.currentTimeMillis()
                val updated = existing.copy(trackingStartTimestamp = startTs)
                settingsDao.insertOrUpdate(updated)
                updated
            } else {
                existing
            }
        } else {
            val now = System.currentTimeMillis()
            val defaultSettings = UserSettingsEntity(trackingStartTimestamp = now)
            settingsDao.insertOrUpdate(defaultSettings)
            defaultSettings
        }
    }

    suspend fun saveUserSettings(settings: UserSettingsEntity) = settingsDao.insertOrUpdate(settings)
}
