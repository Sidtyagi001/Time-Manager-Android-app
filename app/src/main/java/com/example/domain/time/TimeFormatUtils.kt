package com.example.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatUtils {

    fun formatTime(millis: Long, is24Hour: Boolean, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern).withZone(zoneId).format(Instant.ofEpochMilli(millis))
    }

    fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String {
        val localTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return localTime.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun formatTimeWithSeconds(millis: Long, is24Hour: Boolean, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val pattern = if (is24Hour) "HH:mm:ss" else "h:mm:ss a"
        return DateTimeFormatter.ofPattern(pattern).withZone(zoneId).format(Instant.ofEpochMilli(millis))
    }

    fun formatDateTime(millis: Long, is24Hour: Boolean, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy • $timePattern").withZone(zoneId).format(Instant.ofEpochMilli(millis))
    }

    fun formatShortDateTime(millis: Long, is24Hour: Boolean, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern("MMM d • $timePattern").withZone(zoneId).format(Instant.ofEpochMilli(millis))
    }

    fun formatScheduledSummary(millis: Long, is24Hour: Boolean, now: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): String {
        val scheduledDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val timeStr = formatTime(millis, is24Hour, zoneId)

        return when (scheduledDate) {
            today -> "Today at $timeStr"
            today.plusDays(1) -> "Tomorrow at $timeStr"
            else -> "${scheduledDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} at $timeStr"
        }
    }
}
