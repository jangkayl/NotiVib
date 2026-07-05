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
    
    fun shouldIntercept(context: Context): Boolean {
        return isGloballyEnabled(context) && isScheduleActive(context)
    }
}
