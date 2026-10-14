package com.example.domain.time

import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.TimetableTaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

data class TimeInterval(
    val start: Long,
    val end: Long
) {
    val duration: Long get() = max(0L, end - start)
}

data class DaySessionSegment(
    val sessionId: Long,
    val activityId: Long,
    val activityName: String,
    val category: String,
    val colorHex: String,
    val segmentStart: Long,
    val segmentEnd: Long,
    val isBreak: Boolean,
    val notes: String
) {
    val durationMillis: Long get() = max(0L, segmentEnd - segmentStart)
}

data class ActivityBreakdown(
    val activityId: Long,
    val activityName: String,
    val category: String,
    val colorHex: String,
    val durationMillis: Long,
    val sessionCount: Int,
    val percentageOfTracked: Float
)

data class DayTimeReport(
    val dateStr: String,
    val startOfDayMillis: Long,
    val endOfDayMillis: Long,
    val effectiveWindowMillis: Long,
    val trackedMillis: Long,
    val breakMillis: Long,
    val untrackedMillis: Long,
    val plannedMillis: Long,
    val actualLinkedPlannedMillis: Long,
    val unfulfilledPlannedMillis: Long,
    val trackingCompletenessPercent: Float,
    val activityBreakdowns: List<ActivityBreakdown>,
    val segments: List<DaySessionSegment>
)

object TimeAccountingEngine {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getStartOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getEndOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    }

    fun parseDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DATE_FORMATTER)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    /**
     * Merge overlapping or adjacent intervals to prevent double-counting.
     */
    fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals
            .filter { it.end > it.start }
            .sortedBy { it.start }

        if (sorted.isEmpty()) return emptyList()

        val merged = mutableListOf<TimeInterval>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.start <= current.end) {
                current = TimeInterval(current.start, max(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /**
     * Compute total unique duration covered by intervals, merging overlaps.
     */
    fun computeUniqueCoveredDuration(intervals: List<TimeInterval>): Long {
        return mergeIntervals(intervals).sumOf { it.duration }
    }

    /**
     * Calculates the daily time breakdown for a given date.
     * Correctly clips sessions that cross midnight boundaries.
     * Prevents duplicate counting of overlapping intervals.
     */
    fun calculateDayReport(
        dateStr: String,
        allSessions: List<ActivitySessionEntity>,
        timetableTasks: List<TimetableTaskEntity>,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DayTimeReport {
        val targetDate = parseDate(dateStr)
        val dayStart = getStartOfDayMillis(targetDate, zoneId)
        val dayEnd = getEndOfDayMillis(targetDate, zoneId) + 1 // start of next day for exact boundary

        val todayDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val isToday = targetDate == todayDate
        val isFuture = targetDate.isAfter(todayDate)

        val effectiveDayEnd = when {
            isFuture -> dayStart
            isToday -> min(dayEnd, now)
            else -> dayEnd
        }

        val effectiveWindowMillis = max(0L, effectiveDayEnd - dayStart)

        // Find and clip sessions that overlap with this calendar day
        val daySegments = mutableListOf<DaySessionSegment>()
        for (session in allSessions) {
            val sessionEnd = if (session.endTime <= 0) now else session.endTime
            if (sessionEnd <= dayStart || session.startTime >= dayEnd) {
                continue // Does not touch this day
            }

            // Clip boundaries to this day
            val clippedStart = max(dayStart, session.startTime)
            val clippedEnd = min(effectiveDayEnd, sessionEnd)

            if (clippedEnd > clippedStart) {
                daySegments.add(
                    DaySessionSegment(
                        sessionId = session.id,
                        activityId = session.activityId,
                        activityName = session.activityNameSnapshot,
                        category = session.categorySnapshot,
                        colorHex = session.colorHexSnapshot,
                        segmentStart = clippedStart,
                        segmentEnd = clippedEnd,
                        isBreak = session.isBreak,
                        notes = session.notes
                    )
                )
            }
        }

        // Separate non-break and break intervals
        val trackedIntervals = daySegments
            .filter { !it.isBreak }
            .map { TimeInterval(it.segmentStart, it.segmentEnd) }

        val breakIntervals = daySegments
            .filter { it.isBreak }
            .map { TimeInterval(it.segmentStart, it.segmentEnd) }

        val allRecordedIntervals = daySegments
            .map { TimeInterval(it.segmentStart, it.segmentEnd) }

        val uniqueTrackedDuration = computeUniqueCoveredDuration(trackedIntervals)
        val uniqueBreakDuration = computeUniqueCoveredDuration(breakIntervals)
        val uniqueTotalCovered = computeUniqueCoveredDuration(allRecordedIntervals)

        val untrackedMillis = max(0L, effectiveWindowMillis - uniqueTotalCovered)

        val trackingCompletenessPercent = if (effectiveWindowMillis > 0) {
            (uniqueTotalCovered.toFloat() / effectiveWindowMillis.toFloat()).coerceIn(0f, 1f) * 100f
        } else {
            0f
        }

        // Activity breakdown
        val activityGroups = daySegments
            .filter { !it.isBreak }
            .groupBy { it.activityId }

        val breakdowns = activityGroups.map { (actId, segs) ->
            val first = segs.first()
            val intervals = segs.map { TimeInterval(it.segmentStart, it.segmentEnd) }
            val actDuration = computeUniqueCoveredDuration(intervals)
            ActivityBreakdown(
                activityId = actId,
                activityName = first.activityName,
                category = first.category,
                colorHex = first.colorHex,
                durationMillis = actDuration,
                sessionCount = segs.map { it.sessionId }.distinct().size,
                percentageOfTracked = if (uniqueTrackedDuration > 0) (actDuration.toFloat() / uniqueTrackedDuration) * 100f else 0f
            )
        }.sortedByDescending { it.durationMillis }

        // Planned vs Actual
        val plannedTasksForDay = timetableTasks.filter { it.dateStr == dateStr }
        val plannedMillis = plannedTasksForDay.sumOf { it.durationMinutes * 60 * 1000L }

        // Compare actual tracked intervals with planned tasks without double-counting
        val actualLinkedIntervals = mutableListOf<TimeInterval>()
        for (task in plannedTasksForDay) {
            val taskStart = dayStart + (task.slotIndex * 30 * 60 * 1000L)
            val taskEnd = taskStart + (task.durationMinutes * 60 * 1000L)

            // Look for matching segments: linked directly by id, or by linkedActivityId overlapping slot
            val matchingSegments = daySegments.filter { seg ->
                !seg.isBreak && (
                    (task.linkedActivityId != null && seg.activityId == task.linkedActivityId) ||
                    (seg.segmentStart < taskEnd && seg.segmentEnd > taskStart)
                )
            }

            for (seg in matchingSegments) {
                val matchStart = max(taskStart, seg.segmentStart)
                val matchEnd = min(taskEnd, seg.segmentEnd)
                if (matchEnd > matchStart) {
                    actualLinkedIntervals.add(TimeInterval(matchStart, matchEnd))
                }
            }
        }

        val actualLinkedPlannedMillis = computeUniqueCoveredDuration(actualLinkedIntervals)
        val unfulfilledPlannedMillis = max(0L, plannedMillis - actualLinkedPlannedMillis)

        return DayTimeReport(
            dateStr = dateStr,
            startOfDayMillis = dayStart,
            endOfDayMillis = dayEnd - 1,
            effectiveWindowMillis = effectiveWindowMillis,
            trackedMillis = uniqueTrackedDuration,
            breakMillis = uniqueBreakDuration,
            untrackedMillis = untrackedMillis,
            plannedMillis = plannedMillis,
            actualLinkedPlannedMillis = actualLinkedPlannedMillis,
            unfulfilledPlannedMillis = unfulfilledPlannedMillis,
            trackingCompletenessPercent = trackingCompletenessPercent,
            activityBreakdowns = breakdowns,
            segments = daySegments.sortedBy { it.segmentStart }
        )
    }

    /**
     * Compute multi-day reports (e.g. for Weekly or Monthly views).
     */
    fun calculateMultiDayReports(
        startDate: LocalDate,
        endDate: LocalDate,
        allSessions: List<ActivitySessionEntity>,
        timetableTasks: List<TimetableTaskEntity>,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DayTimeReport> {
        val reports = mutableListOf<DayTimeReport>()
        var cur = startDate
        while (!cur.isAfter(endDate)) {
            val dateStr = formatDate(cur)
            reports.add(calculateDayReport(dateStr, allSessions, timetableTasks, now, zoneId))
            cur = cur.plusDays(1)
        }
        return reports
    }
}
