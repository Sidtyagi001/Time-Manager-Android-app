package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.DailyReviewEntity
import com.example.data.local.entity.FocusPhaseEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.data.local.entity.TimetableTemplateEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isFavorite DESC, name ASC")
    fun getAllActive(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isDeleted = 0 ORDER BY isArchived ASC, isFavorite DESC, name ASC")
    fun getAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isDeleted = 0 AND isFavorite = 1 AND isArchived = 0 ORDER BY name ASC")
    fun getFavorites(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY id ASC")
    fun getAllRaw(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Update
    suspend fun update(activity: ActivityEntity)

    @Query("UPDATE activities SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE activities SET isArchived = :isArchived, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: Long, isArchived: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}

@Dao
interface ActivitySessionDao {
    @Query("SELECT * FROM activity_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsList(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE endTime == 0 LIMIT 1")
    suspend fun getActiveSession(): ActivitySessionEntity?

    @Query("SELECT * FROM activity_sessions WHERE endTime == 0 LIMIT 1")
    fun getActiveSessionFlow(): Flow<ActivitySessionEntity?>

    @Query("SELECT * FROM activity_sessions WHERE startTime < :endTime AND (endTime > :startTime OR endTime == 0) ORDER BY startTime ASC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE startTime < :endTime AND (endTime > :startTime OR endTime == 0) ORDER BY startTime ASC")
    fun getSessionsBetweenFlow(startTime: Long, endTime: Long): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActivitySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ActivitySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<ActivitySessionEntity>)

    @Update
    suspend fun update(session: ActivitySessionEntity)

    @Delete
    suspend fun delete(session: ActivitySessionEntity)

    @Query("DELETE FROM activity_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM activity_sessions")
    suspend fun deleteAll()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_tasks WHERE dateStr = :dateStr ORDER BY slotIndex ASC")
    fun getTasksForDate(dateStr: String): Flow<List<TimetableTaskEntity>>

    @Query("SELECT * FROM timetable_tasks WHERE dateStr = :dateStr ORDER BY slotIndex ASC")
    suspend fun getTasksForDateSync(dateStr: String): List<TimetableTaskEntity>

    @Query("SELECT * FROM timetable_tasks ORDER BY dateStr ASC, slotIndex ASC")
    suspend fun getAllTasks(): List<TimetableTaskEntity>

    @Query("SELECT * FROM timetable_tasks ORDER BY dateStr ASC, slotIndex ASC")
    fun getAllTasksFlow(): Flow<List<TimetableTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TimetableTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TimetableTaskEntity>)

    @Update
    suspend fun updateTask(task: TimetableTaskEntity)

    @Delete
    suspend fun deleteTask(task: TimetableTaskEntity)

    @Query("DELETE FROM timetable_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM timetable_tasks WHERE dateStr = :dateStr")
    suspend fun deleteTasksForDate(dateStr: String)

    @Query("DELETE FROM timetable_tasks")
    suspend fun deleteAllTasks()

    // Templates
    @Query("SELECT * FROM timetable_templates WHERE isWeekend = :isWeekend ORDER BY slotIndex ASC")
    suspend fun getTemplatesForWeekend(isWeekend: Boolean): List<TimetableTemplateEntity>

    @Query("SELECT * FROM timetable_templates ORDER BY isWeekend ASC, slotIndex ASC")
    suspend fun getAllTemplates(): List<TimetableTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TimetableTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TimetableTemplateEntity>)

    @Query("DELETE FROM timetable_templates WHERE isWeekend = :isWeekend")
    suspend fun deleteTemplatesForType(isWeekend: Boolean)

    @Query("DELETE FROM timetable_templates")
    suspend fun deleteAllTemplates()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    suspend fun getAllRemindersList(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE triggerTime > :currentTime AND isCompleted = 0 AND isCancelled = 0 ORDER BY triggerTime ASC")
    suspend fun getUpcomingActiveReminders(currentTime: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE triggerTime > :currentTime AND isCompleted = 0 AND isCancelled = 0 ORDER BY triggerTime ASC")
    fun getUpcomingRemindersFlow(currentTime: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_runs WHERE isFinished = 0 LIMIT 1")
    suspend fun getActiveRun(): FocusRunEntity?

    @Query("SELECT * FROM focus_runs WHERE isFinished = 0 LIMIT 1")
    fun getActiveRunFlow(): Flow<FocusRunEntity?>

    @Query("SELECT * FROM focus_runs WHERE id = :id LIMIT 1")
    suspend fun getRunById(id: Long): FocusRunEntity?

    @Query("SELECT * FROM focus_runs ORDER BY startTime DESC")
    fun getAllRunsFlow(): Flow<List<FocusRunEntity>>

    @Query("SELECT * FROM focus_runs ORDER BY startTime DESC")
    suspend fun getAllRunsList(): List<FocusRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: FocusRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuns(runs: List<FocusRunEntity>)

    @Update
    suspend fun updateRun(run: FocusRunEntity)

    @Query("DELETE FROM focus_runs")
    suspend fun deleteAllRuns()

    // Phases
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhase(phase: FocusPhaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhases(phases: List<FocusPhaseEntity>)

    @Update
    suspend fun updatePhase(phase: FocusPhaseEntity)

    @Query("SELECT * FROM focus_phases WHERE runId = :runId ORDER BY startedAt ASC")
    suspend fun getPhasesForRun(runId: Long): List<FocusPhaseEntity>

    @Query("SELECT * FROM focus_phases ORDER BY startedAt ASC")
    suspend fun getAllPhasesList(): List<FocusPhaseEntity>

    @Query("DELETE FROM focus_phases")
    suspend fun deleteAllPhases()
}

@Dao
interface DailyReviewDao {
    @Query("SELECT * FROM daily_reviews WHERE dateStr = :dateStr LIMIT 1")
    fun getReviewForDate(dateStr: String): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getReviewForDateSync(dateStr: String): DailyReviewEntity?

    @Query("SELECT * FROM daily_reviews ORDER BY dateStr DESC")
    suspend fun getAllReviews(): List<DailyReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(review: DailyReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<DailyReviewEntity>)

    @Query("DELETE FROM daily_reviews")
    suspend fun deleteAll()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)
}
