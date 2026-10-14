package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.repository.TimeManagerRepository
import com.example.domain.focus.FocusEngine
import com.example.domain.focus.FocusPhaseType
import com.example.domain.time.TimeAccountingEngine
import com.example.domain.time.TimeInterval
import com.example.export.BackupRestoreManager
import com.example.export.XlsxWorkbookWriter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimeManagerAcceptanceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TimeManagerRepository
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TimeManagerRepository(context, db)
        backupManager = BackupRestoreManager(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1. A session from 10:00 to 11:20 calculates as 80 minutes.
    @Test
    fun testSession1000To1120CalculatesAs80Minutes() {
        val date = LocalDate.of(2026, 10, 14)
        val zone = ZoneId.systemDefault()
        val start1000 = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val end1120 = date.atTime(11, 20).atZone(zone).toInstant().toEpochMilli()

        val interval = TimeInterval(start1000, end1120)
        val minutes = interval.duration / 60000L
        assertEquals(80L, minutes)
    }

    // 2. An active session remains accurate after the app is closed and reopened.
    @Test
    fun testActiveSessionRemainsAccurateFromTimestamps() = runBlocking {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 3600 * 1000L)

        val activeSession = ActivitySessionEntity(
            activityId = 1,
            activityNameSnapshot = "Studying",
            categorySnapshot = "Education",
            startTime = twoHoursAgo,
            endTime = 0L // active
        )
        val id = db.activitySessionDao().insert(activeSession)

        val retrieved = db.activitySessionDao().getActiveSession()
        assertNotNull(retrieved)
        assertEquals(id, retrieved!!.id)

        // Elapsed time is calculated dynamically from current time - startTime
        val elapsedMillis = System.currentTimeMillis() - retrieved.startTime
        assertTrue("Elapsed time should be >= 2 hours", elapsedMillis >= 2 * 3600 * 1000L)
    }

    // 3. Deleting an activity does not delete its historical sessions.
    @Test
    fun testDeletingActivityDoesNotDeleteHistoricalSessions() = runBlocking {
        val activity = ActivityEntity(name = "Painting", category = "Creative", colorHex = "#9333EA")
        val actId = db.activityDao().insert(activity)

        val session = ActivitySessionEntity(
            activityId = actId,
            activityNameSnapshot = "Painting",
            categorySnapshot = "Creative",
            startTime = 100000L,
            endTime = 105000L
        )
        val sessId = db.activitySessionDao().insert(session)

        // Soft delete activity
        repository.softDeleteActivity(actId)

        // Activity is marked deleted
        val actAfter = db.activityDao().getById(actId)
        assertTrue(actAfter!!.isDeleted)

        // Historical session remains completely preserved with name snapshot
        val sessAfter = db.activitySessionDao().getById(sessId)
        assertNotNull(sessAfter)
        assertEquals("Painting", sessAfter!!.activityNameSnapshot)
    }

    // 4. A 40-minute gap between sessions is calculated as untracked time.
    @Test
    fun testUntrackedTimeCalculationWith40MinGap() = runBlocking {
        val date = LocalDate.of(2026, 10, 14)
        val zone = ZoneId.systemDefault()
        val start1000 = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val end1120 = date.atTime(11, 20).atZone(zone).toInstant().toEpochMilli() // 80 min
        val start1200 = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli() // 40 min gap
        val end1330 = date.atTime(13, 30).atZone(zone).toInstant().toEpochMilli() // 90 min

        val sessions = listOf(
            ActivitySessionEntity(activityId = 1, activityNameSnapshot = "Painting", categorySnapshot = "Creative", startTime = start1000, endTime = end1120),
            ActivitySessionEntity(activityId = 2, activityNameSnapshot = "Studying", categorySnapshot = "Education", startTime = start1200, endTime = end1330)
        )

        // Evaluate day report up to end1330
        val report = TimeAccountingEngine.calculateDayReport(
            dateStr = "2026-10-14",
            allSessions = sessions,
            timetableTasks = emptyList(),
            now = end1330,
            zoneId = zone
        )

        assertEquals("Tracked duration should be 80m + 90m = 170m", 170L * 60000L, report.trackedMillis)

        // Within the 10:00 to 13:30 window (210 minutes total), covered is 170 min, untracked is 40 min gap (+ before 10:00)
        val windowBefore1000 = start1000 - TimeAccountingEngine.getStartOfDayMillis(date, zone)
        val expectedUntracked = windowBefore1000 + (40L * 60000L)
        assertEquals(expectedUntracked, report.untrackedMillis)
    }

    // 5. A session spanning midnight is split correctly across reporting days.
    @Test
    fun testSessionSpanningMidnightIsSplitCorrectly() {
        val day1 = LocalDate.of(2026, 10, 14)
        val day2 = LocalDate.of(2026, 10, 15)
        val zone = ZoneId.systemDefault()

        val start2300 = day1.atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
        val end0130 = day2.atTime(1, 30).atZone(zone).toInstant().toEpochMilli()

        val midnightSession = ActivitySessionEntity(
            activityId = 1,
            activityNameSnapshot = "Midnight Work",
            categorySnapshot = "Career",
            startTime = start2300,
            endTime = end0130
        )

        val reportDay1 = TimeAccountingEngine.calculateDayReport(
            dateStr = "2026-10-14",
            allSessions = listOf(midnightSession),
            timetableTasks = emptyList(),
            now = end0130 + 1000000L,
            zoneId = zone
        )

        val reportDay2 = TimeAccountingEngine.calculateDayReport(
            dateStr = "2026-10-15",
            allSessions = listOf(midnightSession),
            timetableTasks = emptyList(),
            now = end0130 + 1000000L,
            zoneId = zone
        )

        // Day 1 has 23:00 to 24:00 = 60 minutes
        assertEquals(60L * 60000L, reportDay1.trackedMillis)
        // Day 2 has 00:00 to 01:30 = 90 minutes
        assertEquals(90L * 60000L, reportDay2.trackedMillis)
    }

    // 6. Overlapping sessions never cause double-counted totals.
    @Test
    fun testOverlappingSessionsNeverDoubleCount() {
        val intervals = listOf(
            TimeInterval(1000L, 2000L), // 1000 duration
            TimeInterval(1500L, 2500L), // overlaps by 500
            TimeInterval(2400L, 3000L)  // overlaps by 100
        )
        // Combined non-overlapping interval is 1000L to 3000L = 2000L
        val covered = TimeAccountingEngine.computeUniqueCoveredDuration(intervals)
        assertEquals(2000L, covered)
    }

    // 7. Splitting one hour creates exactly two 30-minute slots.
    @Test
    fun testSplittingOneHourCreatesTwo30MinSlots() = runBlocking {
        val task = TimetableTaskEntity(
            dateStr = "2026-10-14",
            slotIndex = 18, // 09:00
            durationMinutes = 60,
            title = "Morning Deep Work",
            category = "Career"
        )
        val taskId = db.timetableDao().insertTask(task)
        val savedTask = task.copy(id = taskId)

        val (part1, part2) = repository.splitTaskSlot(savedTask)

        assertEquals(30, part1.durationMinutes)
        assertEquals(18, part1.slotIndex)

        assertEquals(30, part2.durationMinutes)
        assertEquals(19, part2.slotIndex) // exactly next 30m slot
        assertEquals("Morning Deep Work (Part 2)", part2.title)

        // Verify total duration equals exactly 60m
        assertEquals(60, part1.durationMinutes + part2.durationMinutes)
    }

    // 8. Editing tomorrow's timetable does not alter yesterday's records.
    @Test
    fun testEditingTomorrowDoesNotAlterYesterday() = runBlocking {
        val yesterday = "2026-10-13"
        val tomorrow = "2026-10-15"

        val taskYesterday = TimetableTaskEntity(dateStr = yesterday, slotIndex = 18, durationMinutes = 60, title = "Yesterday Task")
        val taskTomorrow = TimetableTaskEntity(dateStr = tomorrow, slotIndex = 18, durationMinutes = 60, title = "Tomorrow Task")

        val id1 = db.timetableDao().insertTask(taskYesterday)
        val id2 = db.timetableDao().insertTask(taskTomorrow)

        // Edit tomorrow's task
        db.timetableDao().updateTask(taskTomorrow.copy(id = id2, title = "Modified Tomorrow Task"))

        // Yesterday remains unchanged
        val yesterdayTasks = db.timetableDao().getTasksForDateSync(yesterday)
        assertEquals(1, yesterdayTasks.size)
        assertEquals("Yesterday Task", yesterdayTasks[0].title)
    }

    // 9. Planned 120-minute study block linked to a 75-minute actual session.
    @Test
    fun testPlannedVsActualMatching() {
        val date = LocalDate.of(2026, 10, 14)
        val zone = ZoneId.systemDefault()
        val start1000 = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val end1115 = date.atTime(11, 15).atZone(zone).toInstant().toEpochMilli() // 75 minutes

        val session = ActivitySessionEntity(
            id = 1,
            activityId = 10,
            activityNameSnapshot = "Studying",
            categorySnapshot = "Education",
            startTime = start1000,
            endTime = end1115
        )

        // 120 minute planned task at slotIndex 20 (10:00)
        val task = TimetableTaskEntity(
            id = 100,
            dateStr = "2026-10-14",
            slotIndex = 20,
            durationMinutes = 120,
            title = "Study Session",
            linkedActivityId = 10
        )

        val report = TimeAccountingEngine.calculateDayReport(
            dateStr = "2026-10-14",
            allSessions = listOf(session),
            timetableTasks = listOf(task),
            now = end1115 + 100000L,
            zoneId = zone
        )

        assertEquals(120L * 60000L, report.plannedMillis)
        assertEquals(75L * 60000L, report.actualLinkedPlannedMillis)
        assertEquals(45L * 60000L, report.unfulfilledPlannedMillis)
    }

    // 10. Focus mode transitions from 25-minute focus to 5-minute break and back.
    @Test
    fun testFocusModeDeterministicReconciliation() {
        val now = 1000000000L
        val run = FocusRunEntity(
            startTime = now,
            focusDurationMinutes = 25,
            breakDurationMinutes = 5,
            warningSeconds = 30
        )

        // At 10 minutes in -> Phase should be FOCUS, cycle 1
        val state10m = FocusEngine.reconcile(run, now + (10 * 60 * 1000L))
        assertEquals(FocusPhaseType.FOCUS, state10m.phase)
        assertEquals(1, state10m.cycleNumber)
        assertEquals(15 * 60 * 1000L, state10m.remainingMillis)

        // At 24m 45s in -> Phase FOCUS, 30s warning is active!
        val state24m45s = FocusEngine.reconcile(run, now + (24 * 60 * 1000L) + 45000L)
        assertEquals(FocusPhaseType.FOCUS, state24m45s.phase)
        assertTrue("Warning should be active within 30s", state24m45s.isWarningActive)

        // At 27 minutes in -> Phase should be BREAK, cycle 1
        val state27m = FocusEngine.reconcile(run, now + (27 * 60 * 1000L))
        assertEquals(FocusPhaseType.BREAK, state27m.phase)
        assertEquals(1, state27m.cycleNumber)
        assertEquals(3 * 60 * 1000L, state27m.remainingMillis)

        // At 32 minutes in -> Transitioned to Cycle 2 FOCUS!
        val state32m = FocusEngine.reconcile(run, now + (32 * 60 * 1000L))
        assertEquals(FocusPhaseType.FOCUS, state32m.phase)
        assertEquals(2, state32m.cycleNumber)
        assertEquals(23 * 60 * 1000L, state32m.remainingMillis)
    }

    // 11. Focus state recovers from process death using persisted timestamps.
    @Test
    fun testFocusRecoversFromProcessDeathWithoutCascades() {
        val startTime = 1000000000L
        val run = FocusRunEntity(
            startTime = startTime,
            focusDurationMinutes = 25,
            breakDurationMinutes = 5
        )
        // Device reboots 95 minutes later (Cycle = 30m, 95m = 3 full cycles + 5 min into cycle 4 focus)
        val state = FocusEngine.reconcile(run, startTime + (95 * 60 * 1000L))
        assertEquals(4, state.cycleNumber)
        assertEquals(FocusPhaseType.FOCUS, state.phase)
        assertEquals(20 * 60 * 1000L, state.remainingMillis)
    }

    // 12. Backup and restore preserves records and settings.
    @Test
    fun testBackupAndRestorePreservesRecordsAndSettings() = runBlocking {
        // Insert sample records
        db.activityDao().insert(ActivityEntity(name = "Cooking", category = "Life", colorHex = "#D97706"))
        db.activitySessionDao().insert(ActivitySessionEntity(activityId = 1, activityNameSnapshot = "Cooking", categorySnapshot = "Life", startTime = 1000L, endTime = 2000L))
        db.userSettingsDao().insertOrUpdate(UserSettingsEntity(focusDurationMinutes = 30, breakDurationMinutes = 7))

        // Create backup
        val json = backupManager.createBackupJson()
        val validation = backupManager.validateBackup(json)
        assertTrue(validation.isValid)
        assertEquals(1, validation.activityCount)
        assertEquals(1, validation.sessionCount)

        // Clear and restore
        val result = backupManager.restoreFromJsonString(json)
        assertTrue(result.isSuccess)

        val restoredActs = db.activityDao().getAllRaw()
        assertEquals(1, restoredActs.size)
        assertEquals("Cooking", restoredActs[0].name)

        val restoredSettings = db.userSettingsDao().getSettings()
        assertNotNull(restoredSettings)
        assertEquals(30, restoredSettings!!.focusDurationMinutes)
        assertEquals(7, restoredSettings.breakDurationMinutes)
    }

    // 13. Corrupted backup does not destroy existing data.
    @Test
    fun testCorruptedBackupDoesNotDestroyExistingData() = runBlocking {
        db.activityDao().insert(ActivityEntity(name = "Safe Activity", category = "Safe", colorHex = "#059669"))

        val corruptedJson = "{ invalid json content: 123 "
        val result = backupManager.restoreFromJsonString(corruptedJson)
        assertFalse(result.isSuccess)

        // Existing data must remain 100% intact
        val acts = db.activityDao().getAllRaw()
        assertEquals(1, acts.size)
        assertEquals("Safe Activity", acts[0].name)
    }

    // 14. Genuine XLSX export generates a valid ZIP workbook containing all 8 sheets.
    @Test
    fun testXlsxExportGeneratesValidWorkbookWithAllSheets() {
        val writer = XlsxWorkbookWriter()
        val baos = ByteArrayOutputStream()

        writer.writeWorkbook(
            out = baos,
            dailyReports = emptyList(),
            sessions = emptyList(),
            tasks = emptyList(),
            focusRuns = emptyList(),
            reminders = emptyList()
        )

        val bytes = baos.toByteArray()
        assertTrue("XLSX output should have non-zero bytes", bytes.isNotEmpty())

        // Read back as ZIP and verify entries
        val zis = ZipInputStream(ByteArrayInputStream(bytes))
        val entryNames = mutableListOf<String>()
        var entry = zis.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zis.closeEntry()
            entry = zis.nextEntry
        }

        assertTrue(entryNames.contains("[Content_Types].xml"))
        assertTrue(entryNames.contains("xl/workbook.xml"))
        assertTrue(entryNames.contains("xl/styles.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet1.xml")) // Daily Summary
        assertTrue(entryNames.contains("xl/worksheets/sheet2.xml")) // Weekly Summary
        assertTrue(entryNames.contains("xl/worksheets/sheet3.xml")) // Monthly Summary
        assertTrue(entryNames.contains("xl/worksheets/sheet4.xml")) // Activity Breakdown
        assertTrue(entryNames.contains("xl/worksheets/sheet5.xml")) // Activity Sessions
        assertTrue(entryNames.contains("xl/worksheets/sheet6.xml")) // Timetable
        assertTrue(entryNames.contains("xl/worksheets/sheet7.xml")) // Focus Sessions
        assertTrue(entryNames.contains("xl/worksheets/sheet8.xml")) // Reminders
    }

    // 15. Verify App Name in resources matches Time Manager
    @Test
    fun testAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Time Manager", appName)
    }
}
