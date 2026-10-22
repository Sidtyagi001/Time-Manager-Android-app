package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.local.AppDatabase
import com.example.domain.focus.FocusEngine
import com.example.domain.focus.FocusPhaseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()

                    // Reschedule active future reminders
                    val reminders = db.reminderDao().getUpcomingActiveReminders(now)
                    for (reminder in reminders) {
                        AlarmScheduler.scheduleReminder(context, reminder)
                    }

                    // Reschedule active focus run
                    val activeRun = db.focusDao().getActiveRun()
                    if (activeRun != null && !activeRun.isFinished) {
                        val state = FocusEngine.reconcile(activeRun, now)
                        val nextPhase = if (state.phase == FocusPhaseType.FOCUS) "BREAK" else "FOCUS"
                        AlarmScheduler.scheduleFocusTransition(context, activeRun.id, state.phaseEndTime, nextPhase)
                        if (activeRun.warningSeconds > 0) {
                            val warnTime = state.phaseEndTime - (activeRun.warningSeconds * 1000L)
                            if (warnTime > now) {
                                AlarmScheduler.scheduleFocusWarning(context, activeRun.id, warnTime)
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminder = db.reminderDao().getById(reminderId)
                if (reminder != null && !reminder.isCompleted && !reminder.isCancelled) {
                    NotificationHelper.showReminderNotification(
                        context = context,
                        reminderId = reminder.id,
                        title = reminder.title,
                        description = reminder.description,
                        soundEnabled = false,
                        vibrateEnabled = reminder.isVibrationEnabled
                    )

                    // Trigger dedicated 12-second ringtone if enabled in settings and reminder
                    val settings = db.userSettingsDao().getSettings() ?: com.example.data.local.entity.UserSettingsEntity()
                    if (settings.alarmRingtoneEnabled && reminder.isSoundEnabled) {
                        com.example.alarm.AlarmAudioPlayer.playAlarm(context, reminder.id)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val runId = intent.getLongExtra(AlarmScheduler.EXTRA_RUN_ID, -1L)
        val isWarning = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_WARNING, false)
        if (runId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val run = db.focusDao().getRunById(runId)
                if (run != null && !run.isFinished) {
                    val now = System.currentTimeMillis()
                    if (isWarning) {
                        NotificationHelper.showFocusWarningNotification(
                            context = context,
                            message = "${run.warningSeconds}s remaining in current focus phase"
                        )
                    } else {
                        val state = FocusEngine.reconcile(run, now)
                        val title = if (state.phase == FocusPhaseType.FOCUS) {
                            "Focus Interval Started"
                        } else {
                            "Rest Break Started"
                        }
                        val message = if (state.phase == FocusPhaseType.FOCUS) {
                            "Cycle #${state.cycleNumber}: Time to focus for ${run.focusDurationMinutes} minutes."
                        } else {
                            "Cycle #${state.cycleNumber}: Great job! Take a ${run.breakDurationMinutes}-minute break."
                        }

                        NotificationHelper.showFocusTransitionNotification(
                            context = context,
                            runId = run.id,
                            title = title,
                            message = message,
                            nextPhaseType = if (state.phase == FocusPhaseType.FOCUS) "BREAK" else "FOCUS"
                        )

                        // Schedule the next transition
                        val nextPhase = if (state.phase == FocusPhaseType.FOCUS) "BREAK" else "FOCUS"
                        AlarmScheduler.scheduleFocusTransition(context, run.id, state.phaseEndTime, nextPhase)
                        if (run.warningSeconds > 0) {
                            val warnTime = state.phaseEndTime - (run.warningSeconds * 1000L)
                            if (warnTime > now) {
                                AlarmScheduler.scheduleFocusWarning(context, run.id, warnTime)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.example.ACTION_DISMISS_REMINDER"
        const val ACTION_DISMISS_REMINDER_SOUND = "com.example.ACTION_DISMISS_REMINDER_SOUND"
        const val ACTION_STOP_FOCUS = "com.example.ACTION_STOP_FOCUS"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_RUN_ID = "extra_run_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Stop dedicated alarm ringtone immediately with zero latency
        when (action) {
            ACTION_SNOOZE_REMINDER, ACTION_DISMISS_REMINDER, ACTION_DISMISS_REMINDER_SOUND -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                com.example.alarm.AlarmAudioPlayer.stop(if (reminderId > 0) reminderId else null)
            }
        }

        if (action == ACTION_DISMISS_REMINDER_SOUND) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                when (action) {
                    ACTION_SNOOZE_REMINDER -> {
                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        if (reminderId > 0) {
                            val reminder = db.reminderDao().getById(reminderId)
                            if (reminder != null) {
                                val newTriggerTime = System.currentTimeMillis() + (10 * 60 * 1000L)
                                val updated = reminder.copy(
                                    triggerTime = newTriggerTime,
                                    snoozeCount = reminder.snoozeCount + 1
                                )
                                db.reminderDao().update(updated)
                                AlarmScheduler.scheduleReminder(context, updated)
                                NotificationHelper.cancelNotification(context, reminderId.toInt())
                            }
                        }
                    }
                    ACTION_DISMISS_REMINDER -> {
                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        if (reminderId > 0) {
                            val reminder = db.reminderDao().getById(reminderId)
                            if (reminder != null) {
                                db.reminderDao().update(reminder.copy(isCompleted = true))
                                NotificationHelper.cancelNotification(context, reminderId.toInt())
                            }
                        }
                    }
                    ACTION_STOP_FOCUS -> {
                        val runId = intent.getLongExtra(EXTRA_RUN_ID, -1L)
                        if (runId > 0) {
                            val run = db.focusDao().getRunById(runId)
                            if (run != null) {
                                val now = System.currentTimeMillis()
                                val state = FocusEngine.reconcile(run, now)
                                db.focusDao().updateRun(
                                    run.copy(
                                        isFinished = true,
                                        endTime = now,
                                        totalFocusSecondsCompleted = state.totalFocusSecondsCompleted,
                                        totalBreakSecondsCompleted = state.totalBreakSecondsCompleted,
                                        cyclesCompleted = state.cycleNumber
                                    )
                                )
                                AlarmScheduler.cancelFocusAlarms(context, runId)
                                NotificationHelper.cancelNotification(context, NotificationHelper.NOTIFICATION_ID_FOCUS_TRANSITION)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
