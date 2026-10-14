# Time Manager — Offline Personal Productivity Android Application

Time Manager is a 100% offline, privacy-first native Android application built with Kotlin and Jetpack Compose. It combines continuous activity time tracking, automatic untracked-time accounting, a 24-hour daily timetable, native alarms and reminders, recurring 25/5 Pomodoro focus mode, productivity analytics, local backup/restore, and genuine multi-sheet Excel (.xlsx) export.

The application requires **zero accounts, zero cloud servers, zero analytics, zero internet permissions, and zero tracking SDKs**. All user data is persisted strictly on the device using SQLite and Room.

---

## Architecture and Core Modules

```
com.example/
├── MainActivity.kt                      # Edge-to-edge Compose container & permission flows
├── TimeManagerApplication.kt            # Notification channels & dependency singletons
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt               # Room database configuration & initial seed
│   │   ├── dao/
│   │   │   └── Daos.kt                  # Activity, Session, Timetable, Reminder, Focus DAOs
│   │   └── entity/
│   │       └── Entities.kt              # Room entities with indices and relational snapshots
│   └── repository/
│       └── TimeManagerRepository.kt     # Transactional repository bridging Room and Alarms
├── domain/
│   ├── time/
│   │   └── TimeAccountingEngine.kt      # Interval union, midnight splitting, untracked calculation
│   └── focus/
│       └── FocusEngine.kt               # Deterministic phase reconciliation from timestamps
├── alarm/
│   ├── AlarmScheduler.kt                # Native AlarmManager (Exact & Inexact scheduling)
│   └── NotificationHelper.kt            # Notification channels, heads-up alerts & action buttons
├── receiver/
│   └── Receivers.kt                     # BootReceiver, AlarmReceiver, FocusReceiver, ActionReceiver
├── export/
│   ├── XlsxWorkbookWriter.kt            # Genuine OpenXML (.xlsx) multi-sheet generator
│   └── BackupRestoreManager.kt          # Transactional JSON backup/restore with integrity checks
└── ui/
    ├── MainAppScaffold.kt               # Navigation Compose, TopAppBar, active session banner
    ├── MainViewModel.kt                 # StateFlows, reactive timers, accounting queries
    ├── theme/                           # Material 3 colors, typography, shapes, light/dark themes
    ├── home/HomeScreen.kt               # Live clock, active tracker, accounting stats, quick favorites
    ├── activities/ActivitiesScreen.kt   # Activity catalog, soft-delete safety, categories
    ├── timetable/TimetableScreen.kt     # 24h schedule, 30m splitting, current-time indicator, templates
    ├── focus/FocusScreen.kt             # Circular Pomodoro dial, 25/5 cycles, 30s warnings
    ├── reports/ReportsScreen.kt         # Daily, Weekly, Monthly dashboards with custom Canvas charts
    ├── reminders/RemindersScreen.kt     # One-time alarms, snooze, dismiss, permission checks
    ├── history/SessionHistoryScreen.kt  # Search, filter, and manual missed-session logging
    ├── backup/BackupRestoreScreen.kt    # Storage Access Framework JSON backup & restore
    ├── export/ExportScreen.kt           # Genuine multi-sheet .xlsx generation via SAF
    └── settings/SettingsScreen.kt       # Theme, time format, focus defaults, permission shortcuts
```

---

## Key Features & Technical Specifications

### 1. Robust Activity Time Tracking
- **Timestamp-Based Accuracy:** Start and end timestamps are persisted immediately in Room. Duration is calculated from timestamp differences, ensuring 100% accuracy if the app is killed, the device restarts, or the phone sits idle for hours.
- **Single Active Session Rule:** Starting a new activity automatically wraps up any previous in-progress activity.
- **Historical Data Protection:** Deleting an activity uses soft-deletion (`isDeleted = 1`). Historical sessions preserve an immutable snapshot (`activityNameSnapshot`, `categorySnapshot`, `colorHexSnapshot`) so historical reports never change or break when activities are renamed or deleted.
- **Manual Session Correction:** Add forgotten sessions or edit existing records with validation ensuring `endTime >= startTime`.

### 2. Automatic Untracked-Time Accounting
- **Interval Union Sweep-Line Algorithm:** Merges overlapping intervals to prevent double-counting.
- **Midnight Splitting:** Correctly splits sessions spanning midnight across local calendar boundaries (e.g. 23:00 to 01:30 is split into 60m for Day 1 and 90m for Day 2).
- **Categorization:** Clearly distinguishes between:
  1. Tracked activity time
  2. Explicitly recorded break time
  3. Untracked time (unrecorded gaps between sessions)
  4. Planned timetable time
  5. Actual time linked to planned tasks

### 3. 24-Hour Timetable & Planned vs Actual
- **Full Day Coverage:** Covers 00:00 to 24:00 with default 1-hour slots.
- **30-Minute Splitting:** Split any 60-minute task into two exact 30-minute blocks (e.g. 09:00–09:30 and 09:30–10:00) with zero time loss, gaps, or overlap.
- **Current-Time Indicator:** Displays a live marker indicating the current time in the schedule.
- **Templates & Routines:** Apply pre-configured Weekday and Weekend routines or copy yesterday's schedule with one tap.
- **Planned vs Actual Comparison:** Automatically compares planned blocks with linked tracked activity duration.

### 4. Native Reminders & Alarms
- **Native Android AlarmManager:** Uses `setExactAndAllowWhileIdle()` on supported versions.
- **Snooze & Dismiss:** Actions available directly from system notifications (`Snooze 10m` or `Dismiss`).
- **Boot Recovery:** `BootReceiver` listens to `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, and `TIME_SET` to automatically reschedule eligible future alarms without duplicates. Expired alarms are safely ignored.

### 5. Persistent 25/5 Pomodoro Focus Mode
- **Deterministic Reconciliation:** Current phase (Focus vs Break), cycle count, and countdown are reconciled mathematically from `now - startTime`.
- **Zero Cascading Alarms:** If the device was powered off during several phase boundaries, reopening the app instantly reconciles to the exact current phase and cycle count without firing backlogged alerts.
- **30-Second Warning:** Configurable notification and vibration 30 seconds before interval transitions.
- **Custom Durations:** Configurable focus (15, 20, 25, 30, 45, 50 min) and break (3, 5, 10, 15 min) times.

### 6. Local Multi-Sheet Excel Export (.xlsx)
- **Genuine OpenXML Workbook:** Generates a real ZIP archive formatted according to ISO/IEC 29500 (OpenXML) with frozen header rows, bold headings, and column auto-widths.
- **8 Comprehensive Sheets:**
  1. Daily Summary
  2. Weekly Summary
  3. Monthly Summary
  4. Activity Breakdown
  5. Activity Sessions
  6. Timetable & Planned vs Actual
  7. Focus Sessions
  8. Reminder History

### 7. Manual Local Backup & Restore
- **Portable JSON Format:** Contains schema version, timestamp, metadata counts, and all database tables.
- **Atomic Transaction Safety:** Restores execute within a Room database transaction. If any error occurs or the file is corrupted, the transaction rolls back completely, keeping current data intact.

---

## Build & Installation Instructions

### Prerequisites
- JDK 17 or higher
- Android SDK 34 / 36

### Build Commands
To compile the application:
```bash
gradle assembleDebug
```

To run the automated unit and Robolectric test suite:
```bash
gradle :app:testDebugUnitTest
```

### Generated APK Locations
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Permission Handling & System Settings

1. **Notifications (`POST_NOTIFICATIONS`):**
   - Required on Android 13+ (API 33+) to display scheduled reminders and focus transition alerts.
   - The app prompts for permission on first launch and provides a direct shortcut in Settings.
2. **Exact Alarms (`SCHEDULE_EXACT_ALARM`):**
   - On Android 12+ (API 31+), exact alarms can be managed in system settings.
   - If exact alarm permission is denied or restricted, Time Manager gracefully falls back to `setAndAllowWhileIdle()`.
3. **Storage Access Framework (SAF):**
   - Backup files and Excel spreadsheets are saved via Android's zero-permission Storage Access Framework (`ACTION_CREATE_DOCUMENT`), giving the user complete control over where files are stored.

---

## Manufacturer-Specific Battery Optimization Notes

Certain Android manufacturers (e.g., Xiaomi MIUI/HyperOS, Samsung OneUI, Huawei EMUI) employ aggressive background app restrictions that can delay background alarms.

To ensure timely delivery of alarms when the device is locked:
1. **Disable Battery Optimization:** In system settings, set Time Manager's battery usage to **"Unrestricted"**.
2. **Auto-Start / Background Execution:** On Xiaomi / Oppo devices, enable "Auto-start" in the device app management settings.
3. **Do Not Disturb:** Reminders use the Alarm audio category; ensure Alarm volume is turned on.
