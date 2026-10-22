package com.example.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated audio playback manager for scheduled alarms and user-created reminders.
 *
 * Requirements:
 * - Plays the built-in ringtone (reminder_alarm.mp3) for ~12 seconds.
 * - Stops automatically after 12 seconds.
 * - Stops immediately when dismissed or snoozed.
 * - Prevents duplicate or overlapping playback.
 * - Works when screen is closed or device is locked (using Partial WakeLock).
 * - Routes via USAGE_ALARM to respect the alarm volume and policies.
 * - Respects Do Not Disturb and audio settings.
 */
object AlarmAudioPlayer {

    private const val TAG = "AlarmAudioPlayer"
    const val PLAYBACK_DURATION_MS = 12_000L

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var stopRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentReminderId: Long? = null
    private var isTestingSound: Boolean = false

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    // Deduplication tracking: maps reminderId to timestamp of trigger
    private val recentTriggers = ConcurrentHashMap<Long, Long>()

    @Synchronized
    fun isPlaying(): Boolean {
        return _isPlayingState.value
    }

    @Synchronized
    fun getCurrentReminderId(): Long? = currentReminderId

    @Synchronized
    fun isTestPlaying(): Boolean = isTestingSound && _isPlayingState.value

    /**
     * Trigger playback for a scheduled reminder/alarm.
     */
    @Synchronized
    fun playAlarm(context: Context, reminderId: Long) {
        val now = System.currentTimeMillis()

        // 1. Duplicate check: if the exact same reminder is already playing or triggered in last 3 seconds
        if (currentReminderId == reminderId && isPlaying()) {
            Log.d(TAG, "Alarm $reminderId is already playing; ignoring duplicate broadcast.")
            return
        }
        val lastTrigger = recentTriggers[reminderId]
        if (lastTrigger != null && (now - lastTrigger) < 3_000L && isPlaying()) {
            Log.d(TAG, "Alarm $reminderId received repeatedly within 3 seconds; ignoring.")
            return
        }
        recentTriggers[reminderId] = now

        // Clean up old recent triggers older than 60s
        recentTriggers.entries.removeIf { (now - it.value) > 60_000L }

        // 2. Overlap check: stop any current playback cleanly before starting new one
        stopInternal()

        // 3. Respect system audio settings & Do Not Disturb
        if (isSystemMutedOrDnd(context)) {
            Log.d(TAG, "System DND / Alarm stream is muted; skipping audible playback.")
            return
        }

        // 4. Start playback with 12s auto-stop
        startPlayback(context = context, reminderId = reminderId, isTest = false)
    }

    /**
     * Trigger manual test of the dedicated ringtone from Settings.
     * Stops automatically after ~12 seconds or can be stopped manually.
     */
    @Synchronized
    fun playTest(context: Context) {
        if (isTestPlaying()) {
            stop()
            return
        }
        stopInternal()
        startPlayback(context = context, reminderId = null, isTest = true)
    }

    /**
     * Stop dedicated ringtone playback.
     * If reminderId is provided, only stops if the active playback matches this reminder.
     */
    @Synchronized
    fun stop(reminderId: Long? = null) {
        if (reminderId != null && currentReminderId != null && currentReminderId != reminderId) {
            // Dismissed or snoozed a different reminder than what is actively playing
            return
        }
        stopInternal()
    }

    @Synchronized
    private fun startPlayback(context: Context, reminderId: Long?, isTest: Boolean) {
        currentReminderId = reminderId
        isTestingSound = isTest

        // Acquire WakeLock to ensure audio continues even if screen is off or device is locked
        acquireWakeLock(context)

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val player = try {
                MediaPlayer.create(context.applicationContext, R.raw.reminder_alarm)
            } catch (_: Exception) {
                null
            } ?: try {
                MediaPlayer()
            } catch (_: Exception) {
                null
            }

            if (player != null) {
                try {
                    player.setAudioAttributes(audioAttributes)
                    player.isLooping = true
                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        stopInternal()
                        true
                    }
                    player.start()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaPlayer start exception: ${e.message}")
                }
                mediaPlayer = player
            }

            _isPlayingState.value = true

            // Schedule auto-stop after ~12 seconds
            val runnable = Runnable {
                synchronized(this@AlarmAudioPlayer) {
                    Log.d(TAG, "12 seconds elapsed, auto-stopping alarm audio.")
                    stopInternal()
                }
            }
            stopRunnable = runnable
            mainHandler.postDelayed(runnable, PLAYBACK_DURATION_MS)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm audio playback", e)
            stopInternal()
        }
    }

    @Synchronized
    private fun stopInternal() {
        stopRunnable?.let {
            mainHandler.removeCallbacks(it)
            stopRunnable = null
        }

        try {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (_: Exception) {}
                try { player.reset() } catch (_: Exception) {}
                try { player.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception while stopping MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }

        releaseWakeLock()
        currentReminderId = null
        isTestingSound = false
        _isPlayingState.value = false
    }

    private fun acquireWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                val lock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "TimeManager:AlarmAudioWakeLock"
                )
                // Set safety timeout slightly higher than 12s
                lock.acquire(PLAYBACK_DURATION_MS + 2_000L)
                wakeLock = lock
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire partial wakelock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    private fun isSystemMutedOrDnd(context: Context): Boolean {
        try {
            // Check Do Not Disturb (NotificationManager)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val filter = notificationManager?.currentInterruptionFilter
                if (filter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                    // Total silence mode in DND: respect strict silence
                    return true
                }
            }

            // Check Audio stream volume
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                if (maxVol > 0 && alarmVolume == 0) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking system sound state", e)
        }
        return false
    }

    /**
     * For unit tests only: reset state
     */
    fun resetForTesting() {
        stopInternal()
        recentTriggers.clear()
    }
}
