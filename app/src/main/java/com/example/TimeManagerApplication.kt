package com.example

import android.app.Application
import com.example.alarm.NotificationHelper
import com.example.data.local.AppDatabase
import com.example.data.repository.TimeManagerRepository

class TimeManagerApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: TimeManagerRepository by lazy { TimeManagerRepository(this, database) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
