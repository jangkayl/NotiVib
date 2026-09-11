package com.example.notivib.framework.utils

import android.content.Context
import android.content.SharedPreferences

object EngineState {
    private const val PREFS_NAME = "notivib_engine_prefs"
    private const val KEY_IS_GLOBALLY_ENABLED = "is_globally_enabled"
    private const val KEY_IS_SCHEDULE_ACTIVE = "is_schedule_active"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isGloballyEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_GLOBALLY_ENABLED, true)
    }

    fun setGloballyEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_GLOBALLY_ENABLED, enabled).apply()
    }

    fun isScheduleActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_SCHEDULE_ACTIVE, false)
    }

    fun setScheduleActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_SCHEDULE_ACTIVE, active).apply()
    }
    
    fun getTrackedApps(context: Context): Set<String> {
        return getPrefs(context).getStringSet("tracked_apps", emptySet()) ?: emptySet()
    }

    fun setTrackedApps(context: Context, packages: Set<String>) {
        getPrefs(context).edit().putStringSet("tracked_apps", packages).apply()
    }
    
    fun shouldIntercept(context: Context): Boolean {
        return isGloballyEnabled(context)
    }

    private const val KEY_SHOW_FOREGROUND_NOTIFICATION = "show_foreground_notification"

    fun isShowForegroundNotification(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_FOREGROUND_NOTIFICATION, true)
    }

    fun setShowForegroundNotification(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_FOREGROUND_NOTIFICATION, show).apply()
    }

    private const val KEY_NOTIF_REMINDER_ACKED = "notif_reminder_acked_packages"

    fun isNotificationReminderAcknowledged(context: Context, packageName: String): Boolean {
        return getPrefs(context).getStringSet(KEY_NOTIF_REMINDER_ACKED, emptySet())
            ?.contains(packageName) ?: false
    }

    fun setNotificationReminderAcknowledged(context: Context, packageName: String) {
        val current = getPrefs(context).getStringSet(KEY_NOTIF_REMINDER_ACKED, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        current.add(packageName)
        getPrefs(context).edit().putStringSet(KEY_NOTIF_REMINDER_ACKED, current).apply()
    }

    private const val KEY_BATTERY_OPTIMIZATION_DISMISSED = "battery_optimization_dismissed"

    fun isBatteryOptimizationDismissed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_OPTIMIZATION_DISMISSED, false)
    }

    fun setBatteryOptimizationDismissed(context: Context, dismissed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_OPTIMIZATION_DISMISSED, dismissed).apply()
    }
}
