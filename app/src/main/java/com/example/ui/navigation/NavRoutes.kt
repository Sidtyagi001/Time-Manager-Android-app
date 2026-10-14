package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    // Bottom Bar tabs
    object Home : Screen("home", "Home")
    object Activities : Screen("activities", "Activities")
    object Timetable : Screen("timetable", "Timetable")
    object Focus : Screen("focus", "Focus")
    object Reports : Screen("reports", "Reports")

    // Secondary destinations
    object Reminders : Screen("reminders", "Reminders & Alarms")
    object History : Screen("history", "History & Logs")
    object Backup : Screen("backup", "Backup & Restore")
    object Export : Screen("export", "Excel Export")
    object Settings : Screen("settings", "Settings")
}
