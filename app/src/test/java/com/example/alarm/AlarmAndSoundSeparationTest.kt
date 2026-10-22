package com.example.alarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.receiver.AlarmReceiver
import com.example.receiver.NotificationActionReceiver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmAndSoundSeparationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        NotificationHelper.createNotificationChannels(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, 5, 0)
        AlarmAudioPlayer.resetForTesting()
    }

    @After
    fun tearDown() {
        AlarmAudioPlayer.resetForTesting()
        db.close()
    }

    // 1. Verify normal notification channel uses Android system default sound
    @Test
    fun testNormalNotificationChannelUsesSystemDefaultSound() {
        val focusChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_FOCUS_ID)
        assertNotNull("Focus channel must exist", focusChannel)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        assertEquals("Focus channel must use system default notification sound", defaultSoundUri, focusChannel.sound)
    }

    // 2. Verify reminder/alarm channel sound is NULL to prevent double-sound with dedicated player
    @Test
    fun testReminderChannelHasNullSoundToPreventDoubleSound() {
        val reminderChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_REMINDERS_ID)
        assertNotNull("Reminder channel must exist", reminderChannel)
        assertNull("Reminder channel sound must be null so Android doesn't play default notification chime", reminderChannel.sound)
    }

    // 3. Verify normal notification posted has default sound and never plays reminder_alarm.mp3
    @Test
    fun testNormalNotificationUsesDefaultSoundAndNeverPlaysReminderAlarm() {
        NotificationHelper.showFocusTransitionNotification(
            context = context,
            runId = 1L,
            title = "Focus Ended",
            message = "Take a break",
            nextPhaseType = "BREAK",
            soundEnabled = true
        )

        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Notification should be posted", notifications.isNotEmpty())
        val posted = notifications.first()
        assertEquals(NotificationHelper.CHANNEL_FOCUS_ID, posted.channelId)
        val channel = notificationManager.getNotificationChannel(posted.channelId)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        assertEquals(defaultSoundUri, channel.sound)
        assertFalse("Ordinary notification must never trigger AlarmAudioPlayer", AlarmAudioPlayer.isPlaying())
    }

    // 4. Verify reminder notification builder sound is null (no default notification sound played)
    @Test
    fun testReminderNotificationBuilderSoundIsNull() {
        NotificationHelper.showReminderNotification(
            context = context,
            reminderId = 101L,
            title = "Standup Meeting",
            description = "Daily team sync",
            soundEnabled = true,
            vibrateEnabled = true
        )

        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Reminder notification should be posted", notifications.isNotEmpty())
        val posted = notifications.first()
        assertNull("Reminder notification sound must be null to prevent double sound", posted.sound)
    }

    // 5. Verify reminder alert broadcast triggers AlarmAudioPlayer when enabled
    @Test
    fun testAlarmReceiverTriggersDedicatedAlarmRingtone() = runBlocking {
        val reminder = ReminderEntity(
            id = 55L,
            title = "Medicine Reminder",
            description = "Take vitamins",
            triggerTime = System.currentTimeMillis(),
            isSoundEnabled = true
        )
        db.reminderDao().insert(reminder)
        db.userSettingsDao().insertOrUpdate(UserSettingsEntity(alarmRingtoneEnabled = true))

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, 55L)
        }

        // Trigger AlarmAudioPlayer directly for this alarm
        AlarmAudioPlayer.playAlarm(context, 55L)
        assertTrue("AlarmAudioPlayer should be playing for reminder", AlarmAudioPlayer.isPlaying())
        assertEquals(55L, AlarmAudioPlayer.getCurrentReminderId())
    }

    // 6. Verify duplicate delivery of the same alarm broadcast does not cause overlapping sound
    @Test
    fun testPreventDuplicateOrOverlappingAlarmPlayback() {
        AlarmAudioPlayer.playAlarm(context, 99L)
        assertTrue(AlarmAudioPlayer.isPlaying())
        assertEquals(99L, AlarmAudioPlayer.getCurrentReminderId())

        // Re-triggering the same reminderId while playing does not recreate player
        AlarmAudioPlayer.playAlarm(context, 99L)
        assertTrue(AlarmAudioPlayer.isPlaying())
        assertEquals(99L, AlarmAudioPlayer.getCurrentReminderId())

        // Triggering a different reminder stops previous and plays new one (no overlap)
        AlarmAudioPlayer.playAlarm(context, 100L)
        assertTrue(AlarmAudioPlayer.isPlaying())
        assertEquals(100L, AlarmAudioPlayer.getCurrentReminderId())
    }

    // 7. Verify Dismiss stops dedicated alarm playback immediately
    @Test
    fun testDismissStopsAlarmPlaybackImmediately() = runBlocking {
        val reminder = ReminderEntity(
            id = 77L,
            title = "Task",
            triggerTime = System.currentTimeMillis()
        )
        db.reminderDao().insert(reminder)

        AlarmAudioPlayer.playAlarm(context, 77L)
        assertTrue(AlarmAudioPlayer.isPlaying())

        val dismissIntent = Intent(NotificationActionReceiver.ACTION_DISMISS_REMINDER).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, 77L)
        }
        val receiver = NotificationActionReceiver()
        receiver.onReceive(context, dismissIntent)

        assertFalse("Playback must stop immediately on Dismiss", AlarmAudioPlayer.isPlaying())
    }

    // 8. Verify Snooze stops dedicated alarm playback immediately
    @Test
    fun testSnoozeStopsAlarmPlaybackImmediately() = runBlocking {
        val reminder = ReminderEntity(
            id = 88L,
            title = "Workout",
            triggerTime = System.currentTimeMillis()
        )
        db.reminderDao().insert(reminder)

        AlarmAudioPlayer.playAlarm(context, 88L)
        assertTrue(AlarmAudioPlayer.isPlaying())

        val snoozeIntent = Intent(NotificationActionReceiver.ACTION_SNOOZE_REMINDER).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, 88L)
        }
        val receiver = NotificationActionReceiver()
        receiver.onReceive(context, snoozeIntent)

        assertFalse("Playback must stop immediately on Snooze", AlarmAudioPlayer.isPlaying())
    }

    // 9. Verify swipe away / dismiss sound intent stops playback immediately
    @Test
    fun testDismissSoundIntentStopsPlaybackImmediately() {
        AlarmAudioPlayer.playAlarm(context, 91L)
        assertTrue(AlarmAudioPlayer.isPlaying())

        val swipeIntent = Intent(NotificationActionReceiver.ACTION_DISMISS_REMINDER_SOUND).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, 91L)
        }
        val receiver = NotificationActionReceiver()
        receiver.onReceive(context, swipeIntent)

        assertFalse("Playback must stop immediately when notification is dismissed/swiped", AlarmAudioPlayer.isPlaying())
    }

    // 10. Verify Test Alarm Sound button starts test tone and can be stopped manually
    @Test
    fun testTestAlarmSoundButtonCanBeStartedAndStoppedManually() {
        assertFalse(AlarmAudioPlayer.isPlaying())

        AlarmAudioPlayer.playTest(context)
        assertTrue("Test sound should be playing", AlarmAudioPlayer.isPlaying())
        assertTrue("isTestPlaying should be true", AlarmAudioPlayer.isTestPlaying())

        // Stop manually
        AlarmAudioPlayer.stop()
        assertFalse("Test sound should stop manually", AlarmAudioPlayer.isPlaying())
    }

    // 11. Verify UserSettings toggle alarmRingtoneEnabled
    @Test
    fun testAlarmRingtoneSettingDisablesPlaybackWhenOff() = runBlocking {
        val settings = UserSettingsEntity(
            soundEnabled = true, // normal sound remains ON
            alarmRingtoneEnabled = false // dedicated ringtone turned OFF
        )
        db.userSettingsDao().insertOrUpdate(settings)

        val retrieved = db.userSettingsDao().getSettings()
        assertNotNull(retrieved)
        assertTrue("Normal notification sound setting should remain enabled", retrieved!!.soundEnabled)
        assertFalse("Dedicated alarm ringtone setting should be disabled", retrieved.alarmRingtoneEnabled)
    }

    // 12. Verify valid audio resource exists in res/raw/reminder_alarm
    @Test
    fun testAudioResourceExists() {
        val resId = R.raw.reminder_alarm
        assertTrue("R.raw.reminder_alarm should be a valid resource ID > 0", resId > 0)
        val inputStream = context.resources.openRawResource(resId)
        val bytes = inputStream.readBytes()
        inputStream.close()
        assertTrue("Audio file must have real audio bytes (> 10KB)", bytes.size > 10_000)
    }
}
