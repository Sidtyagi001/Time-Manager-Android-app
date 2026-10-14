package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.entity.ReminderEntity
import com.example.receiver.AlarmReceiver
import com.example.receiver.FocusAlarmReceiver

object AlarmScheduler {

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_RUN_ID = "extra_run_id"
    const val EXTRA_IS_WARNING = "extra_is_warning"
    const val EXTRA_NEXT_PHASE = "extra_next_phase"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        if (reminder.triggerTime <= System.currentTimeMillis() || reminder.isCompleted || reminder.isCancelled) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmInternal(alarmManager, reminder.triggerTime, pendingIntent, context)
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleFocusTransition(
        context: Context,
        runId: Long,
        triggerTime: Long,
        nextPhaseType: String
    ) {
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            putExtra(EXTRA_RUN_ID, runId)
            putExtra(EXTRA_NEXT_PHASE, nextPhaseType)
            putExtra(EXTRA_IS_WARNING, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (500000 + runId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmInternal(alarmManager, triggerTime, pendingIntent, context)
    }

    fun scheduleFocusWarning(
        context: Context,
        runId: Long,
        triggerTime: Long
    ) {
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FocusAlarmReceiver::class.java).apply {
            putExtra(EXTRA_RUN_ID, runId)
            putExtra(EXTRA_IS_WARNING, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (600000 + runId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmInternal(alarmManager, triggerTime, pendingIntent, context)
    }

    fun cancelFocusAlarms(context: Context, runId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Transition pending intent
        val transitionIntent = Intent(context, FocusAlarmReceiver::class.java)
        val transitionPending = PendingIntent.getBroadcast(
            context,
            (500000 + runId).toInt(),
            transitionIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (transitionPending != null) {
            alarmManager.cancel(transitionPending)
            transitionPending.cancel()
        }

        // Warning pending intent
        val warningIntent = Intent(context, FocusAlarmReceiver::class.java)
        val warningPending = PendingIntent.getBroadcast(
            context,
            (600000 + runId).toInt(),
            warningIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (warningPending != null) {
            alarmManager.cancel(warningPending)
            warningPending.cancel()
        }
    }

    private fun scheduleAlarmInternal(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent,
        context: Context
    ) {
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
