package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TimeManagerApplication
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.DailyReviewEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.domain.focus.FocusEngine
import com.example.domain.focus.ReconciledFocusState
import com.example.domain.time.DayTimeReport
import com.example.domain.time.TimeAccountingEngine
import com.example.export.BackupRestoreManager
import com.example.export.RestoreResult
import com.example.export.XlsxWorkbookWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TimeManagerApplication).repository
    private val backupManager = BackupRestoreManager(application, (application as TimeManagerApplication).database)
    private val xlsxWriter = XlsxWorkbookWriter()

    suspend fun getDayReport(dateStr: String): DayTimeReport {
        return repository.getDayReport(dateStr, System.currentTimeMillis())
    }

    suspend fun getMultiDayReports(startDate: LocalDate, endDate: LocalDate): List<DayTimeReport> {
        return repository.getMultiDayReports(startDate, endDate, System.currentTimeMillis())
    }

    // Current ticker for smooth UI clock and countdown updates
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    // Selected timetable / day date
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Activities
    val activeActivities: StateFlow<List<ActivityEntity>> = repository.getAllActiveActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityEntity>> = repository.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteActivities: StateFlow<List<ActivityEntity>> = repository.getFavoriteActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active session
    val activeSession: StateFlow<ActivitySessionEntity?> = repository.getActiveSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All sessions
    val allSessions: StateFlow<List<ActivitySessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = repository.getAllRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Focus mode
    val activeFocusRun: StateFlow<FocusRunEntity?> = repository.getActiveFocusRunFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _focusState = MutableStateFlow<ReconciledFocusState?>(null)
    val focusState: StateFlow<ReconciledFocusState?> = _focusState.asStateFlow()

    // Settings
    val userSettings: StateFlow<UserSettingsEntity?> = repository.getUserSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Day reports for selected date
    private val _selectedDateReport = MutableStateFlow<DayTimeReport?>(null)
    val selectedDateReport: StateFlow<DayTimeReport?> = _selectedDateReport.asStateFlow()

    private val _todayReport = MutableStateFlow<DayTimeReport?>(null)
    val todayReport: StateFlow<DayTimeReport?> = _todayReport.asStateFlow()

    // Timetable tasks for selected date
    private val _selectedDateTasks = MutableStateFlow<List<TimetableTaskEntity>>(emptyList())
    val selectedDateTasks: StateFlow<List<TimetableTaskEntity>> = _selectedDateTasks.asStateFlow()

    // Daily review for selected date
    private val _selectedDateReview = MutableStateFlow<DailyReviewEntity?>(null)
    val selectedDateReview: StateFlow<DailyReviewEntity?> = _selectedDateReview.asStateFlow()

    init {
        // Periodic ticker for elapsed time calculation & focus countdown
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _currentTimeMillis.value = now
                val run = activeFocusRun.value
                if (run != null && !run.isFinished) {
                    _focusState.value = FocusEngine.reconcile(run, now)
                } else {
                    _focusState.value = null
                }
                delay(1000L)
            }
        }

        // Refresh reports when sessions or selected date change
        viewModelScope.launch {
            allSessions.collect {
                refreshReports()
            }
        }

        viewModelScope.launch {
            selectedDate.collect { date ->
                loadTasksForDate(date)
                refreshReports()
                loadReviewForDate(date)
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousDate() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDate() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    private suspend fun loadTasksForDate(date: LocalDate) {
        val dateStr = TimeAccountingEngine.formatDate(date)
        _selectedDateTasks.value = repository.getTasksForDate(dateStr)
    }

    private suspend fun loadReviewForDate(date: LocalDate) {
        val dateStr = TimeAccountingEngine.formatDate(date)
        repository.getReviewForDateFlow(dateStr).collect {
            _selectedDateReview.value = it
        }
    }

    fun refreshReports() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val todayStr = TimeAccountingEngine.formatDate(LocalDate.now())
            _todayReport.value = repository.getDayReport(todayStr, now)

            val selectedStr = TimeAccountingEngine.formatDate(_selectedDate.value)
            _selectedDateReport.value = repository.getDayReport(selectedStr, now)
            loadTasksForDate(_selectedDate.value)
        }
    }

    // Tracking
    fun startTracking(activity: ActivityEntity, isBreak: Boolean = false, notes: String = "") {
        viewModelScope.launch {
            repository.startTracking(activity, isBreak, notes)
            refreshReports()
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            repository.stopTracking()
            refreshReports()
        }
    }

    // Activity Management
    fun saveActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.saveActivity(activity)
        }
    }

    fun softDeleteActivity(id: Long) {
        viewModelScope.launch {
            repository.softDeleteActivity(id)
            refreshReports()
        }
    }

    fun archiveActivity(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveActivity(id, isArchived)
        }
    }

    fun toggleFavorite(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.saveActivity(activity.copy(isFavorite = !activity.isFavorite))
        }
    }

    // Manual session entry & edit
    fun saveManualSession(
        activityId: Long,
        activityName: String,
        category: String,
        colorHex: String,
        startTime: Long,
        endTime: Long,
        isBreak: Boolean,
        notes: String,
        sessionId: Long = 0L,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.saveManualSession(
                    activityId = activityId,
                    activityName = activityName,
                    category = category,
                    colorHex = colorHex,
                    startTime = startTime,
                    endTime = endTime,
                    isBreak = isBreak,
                    notes = notes,
                    sessionId = sessionId
                )
                refreshReports()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save session")
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            refreshReports()
        }
    }

    // Timetable
    fun saveTimetableTask(task: TimetableTaskEntity) {
        viewModelScope.launch {
            repository.saveTimetableTask(task)
            loadTasksForDate(_selectedDate.value)
            refreshReports()
        }
    }

    fun deleteTimetableTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTimetableTask(taskId)
            loadTasksForDate(_selectedDate.value)
            refreshReports()
        }
    }

    fun splitTaskSlot(task: TimetableTaskEntity) {
        viewModelScope.launch {
            try {
                repository.splitTaskSlot(task)
                loadTasksForDate(_selectedDate.value)
                refreshReports()
            } catch (_: Exception) {}
        }
    }

    fun copyTimetableFromPreviousDay() {
        viewModelScope.launch {
            val toDateStr = TimeAccountingEngine.formatDate(_selectedDate.value)
            val fromDateStr = TimeAccountingEngine.formatDate(_selectedDate.value.minusDays(1))
            repository.copyTimetable(fromDateStr, toDateStr)
            loadTasksForDate(_selectedDate.value)
            refreshReports()
        }
    }

    fun applyTemplateToSelectedDay(isWeekend: Boolean) {
        viewModelScope.launch {
            val toDateStr = TimeAccountingEngine.formatDate(_selectedDate.value)
            repository.applyTemplate(toDateStr, isWeekend)
            loadTasksForDate(_selectedDate.value)
            refreshReports()
        }
    }

    // Reminders
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
        }
    }

    fun dismissReminder(id: Long) {
        viewModelScope.launch {
            repository.cancelOrDismissReminder(id, isDismiss = true)
        }
    }

    fun cancelReminder(id: Long) {
        viewModelScope.launch {
            repository.cancelOrDismissReminder(id, isDismiss = false)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    // Focus Mode
    fun startFocus(
        linkedActivityId: Long? = null,
        linkedActivityName: String? = null,
        focusDurationMinutes: Int = 25,
        breakDurationMinutes: Int = 5,
        warningSeconds: Int = 30
    ) {
        viewModelScope.launch {
            val run = repository.startFocusRun(
                linkedActivityId = linkedActivityId,
                linkedActivityName = linkedActivityName,
                focusDurationMinutes = focusDurationMinutes,
                breakDurationMinutes = breakDurationMinutes,
                warningSeconds = warningSeconds
            )
            _focusState.value = FocusEngine.reconcile(run, System.currentTimeMillis())
        }
    }

    fun stopFocus() {
        viewModelScope.launch {
            val run = activeFocusRun.value
            if (run != null) {
                repository.stopFocusRun(run.id)
                _focusState.value = null
            }
        }
    }

    // Daily Review
    fun saveDailyReview(rating: Int, notes: String) {
        viewModelScope.launch {
            val dateStr = TimeAccountingEngine.formatDate(_selectedDate.value)
            repository.saveDailyReview(dateStr, notes, rating)
            loadReviewForDate(_selectedDate.value)
        }
    }

    // Settings
    fun saveUserSettings(settings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.saveUserSettings(settings)
        }
    }

    // Excel Export
    suspend fun exportToXlsx(
        startDate: LocalDate,
        endDate: LocalDate,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val reports = repository.getMultiDayReports(startDate, endDate, now)
        val sessions = repository.sessionDao.getAllSessionsList()
        val tasks = repository.timetableDao.getAllTasks()
        val focusRuns = repository.focusDao.getAllRunsList()
        val reminders = repository.reminderDao.getAllRemindersList()

        xlsxWriter.writeWorkbook(
            out = outputStream,
            dailyReports = reports,
            sessions = sessions,
            tasks = tasks,
            focusRuns = focusRuns,
            reminders = reminders
        )
    }

    // Backup & Restore
    suspend fun createBackup(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        backupManager.writeBackup(outputStream)
    }

    suspend fun validateBackupJson(json: String) = withContext(Dispatchers.Default) {
        backupManager.validateBackup(json)
    }

    suspend fun restoreBackup(inputStream: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val result = backupManager.restoreBackup(inputStream)
        refreshReports()
        result
    }
}
