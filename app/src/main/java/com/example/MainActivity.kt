package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MainAppScaffold
import com.example.ui.MainViewModel
import com.example.ui.theme.TimeManagerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAlarmAudioIntent(intent)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.userSettings.collectAsState()
            val appTheme = com.example.ui.theme.AppTheme.fromId(settings?.themeId)
            val isDarkTheme = when {
                appTheme.isDarkOnly -> true
                appTheme.isLightOnly -> false
                settings?.isDarkMode != null -> settings?.isDarkMode == true
                else -> isSystemInDarkTheme()
            }

            // Notification permission request for Android 13+ (API 33+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            TimeManagerTheme(
                appTheme = appTheme,
                darkTheme = isDarkTheme
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmAudioIntent(intent)
    }

    private fun handleAlarmAudioIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(com.example.alarm.NotificationHelper.EXTRA_STOP_ALARM_AUDIO, false) == true) {
            val reminderId = intent.getLongExtra(com.example.alarm.NotificationHelper.EXTRA_REMINDER_ID, -1L)
            com.example.alarm.AlarmAudioPlayer.stop(if (reminderId > 0) reminderId else null)
        }
    }
}
