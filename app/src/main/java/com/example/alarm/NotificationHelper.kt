package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {

    const val CHANNEL_REMINDERS_ID = "channel_time_manager_reminders"
    const val CHANNEL_FOCUS_ID = "channel_time_manager_focus"

    const val NOTIFICATION_ID_FOCUS_RUNNING = 99990
    const val NOTIFICATION_ID_FOCUS_TRANSITION = 99991
    const val NOTIFICATION_ID_FOCUS_WARNING = 99992

    const val EXTRA_STOP_ALARM_AUDIO = "com.example.EXTRA_STOP_ALARM_AUDIO"
    const val EXTRA_REMINDER_ID = "com.example.EXTRA_REMINDER_ID"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // Reminder Channel (Silent channel sound so dedicated AlarmAudioPlayer handles 12s reminder_alarm.mp3)
            val existingReminderChannel = notificationManager.getNotificationChannel(CHANNEL_REMINDERS_ID)
            if (existingReminderChannel != null && existingReminderChannel.sound != null) {
                notificationManager.deleteNotificationChannel(CHANNEL_REMINDERS_ID)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(null, null)
            }

            // Focus Channel
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_ID,
                context.getString(R.string.channel_focus_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_focus_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(reminderSound, audioAttributes)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(focusChannel)
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_STOP_ALARM_AUDIO, true)
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10 minutes action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_REMINDER
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 200000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS_REMINDER
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 300000).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Swipe-away / delete action to stop ringtone immediately
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS_REMINDER_SOUND
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 350000).toInt(),
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(if (description.isNotBlank()) description else "Scheduled reminder alert")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        if (vibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        // Dedicated ringtone (reminder_alarm.mp3) is managed exclusively via AlarmAudioPlayer.
        // We strictly setSound(null) here so Android's NotificationManager does NOT play the default notification sound.
        builder.setSound(null)

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun showFocusTransitionNotification(
        context: Context,
        runId: Long,
        title: String,
        message: String,
        nextPhaseType: String,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            runId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP_FOCUS
            putExtra(NotificationActionReceiver.EXTRA_RUN_ID, runId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            (runId + 400000).toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Focus", stopPendingIntent)

        if (vibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }
        if (soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FOCUS_TRANSITION, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun showFocusWarningNotification(
        context: Context,
        message: String
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Mode")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FOCUS_WARNING, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
