package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ActivityDao
import com.example.data.local.dao.ActivitySessionDao
import com.example.data.local.dao.DailyReviewDao
import com.example.data.local.dao.FocusDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.TimetableDao
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.entity.ActivityEntity
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.DailyReviewEntity
import com.example.data.local.entity.FocusPhaseEntity
import com.example.data.local.entity.FocusRunEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TimetableTaskEntity
import com.example.data.local.entity.TimetableTemplateEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ActivityEntity::class,
        ActivitySessionEntity::class,
        TimetableTaskEntity::class,
        TimetableTemplateEntity::class,
        ReminderEntity::class,
        FocusRunEntity::class,
        FocusPhaseEntity::class,
        DailyReviewEntity::class,
        UserSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun timetableDao(): TimetableDao
    abstract fun reminderDao(): ReminderDao
    abstract fun focusDao(): FocusDao
    abstract fun dailyReviewDao(): DailyReviewDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN trackingStartTimestamp INTEGER NOT NULL DEFAULT 0")
                val now = System.currentTimeMillis()
                db.execSQL("UPDATE user_settings SET trackingStartTimestamp = COALESCE((SELECT MIN(startTime) FROM activity_sessions), $now) WHERE trackingStartTimestamp = 0")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN alarmRingtoneEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN themeId TEXT NOT NULL DEFAULT 'SYSTEM'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_manager.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }
    }
}

suspend fun populateInitialData(db: AppDatabase) {
    val activityDao = db.activityDao()
    val initialActivities = listOf(
        ActivityEntity(name = "Studying", category = "Education", colorHex = "#2563EB", iconName = "school", isFavorite = true),
        ActivityEntity(name = "Work", category = "Career", colorHex = "#4F46E5", iconName = "work", isFavorite = true),
        ActivityEntity(name = "Exercise", category = "Health", colorHex = "#059669", iconName = "fitness_center", isFavorite = true),
        ActivityEntity(name = "Reading", category = "Personal", colorHex = "#0D9488", iconName = "menu_book", isFavorite = true),
        ActivityEntity(name = "Cooking", category = "Life", colorHex = "#D97706", iconName = "restaurant", isFavorite = false),
        ActivityEntity(name = "Painting", category = "Creative", colorHex = "#9333EA", iconName = "palette", isFavorite = false),
        ActivityEntity(name = "Household Tasks", category = "Chores", colorHex = "#64748B", iconName = "home", isFavorite = false),
        ActivityEntity(name = "Travel", category = "Transit", colorHex = "#0284C7", iconName = "commute", isFavorite = false),
        ActivityEntity(name = "Leisure", category = "Rest", colorHex = "#EA580C", iconName = "sports_esports", isFavorite = false),
        ActivityEntity(name = "Break", category = "Rest", colorHex = "#E11D48", iconName = "coffee", isFavorite = false)
    )
    activityDao.insertAll(initialActivities)

    val userSettingsDao = db.userSettingsDao()
    userSettingsDao.insertOrUpdate(UserSettingsEntity(trackingStartTimestamp = System.currentTimeMillis()))

    // Initial timetable templates
    val timetableDao = db.timetableDao()
    val weekdayTemplates = listOf(
        TimetableTemplateEntity(templateName = "Weekday Routine", isWeekend = false, slotIndex = 18, durationMinutes = 60, title = "Morning Work Session", category = "Career"),
        TimetableTemplateEntity(templateName = "Weekday Routine", isWeekend = false, slotIndex = 20, durationMinutes = 60, title = "Focused Deep Work", category = "Career"),
        TimetableTemplateEntity(templateName = "Weekday Routine", isWeekend = false, slotIndex = 24, durationMinutes = 60, title = "Lunch & Walk", category = "Health"),
        TimetableTemplateEntity(templateName = "Weekday Routine", isWeekend = false, slotIndex = 28, durationMinutes = 60, title = "Study & Upskilling", category = "Education"),
        TimetableTemplateEntity(templateName = "Weekday Routine", isWeekend = false, slotIndex = 36, durationMinutes = 60, title = "Evening Reading", category = "Personal")
    )
    timetableDao.insertTemplates(weekdayTemplates)
}
