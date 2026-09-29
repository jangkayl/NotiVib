package com.example.notivib.framework.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks the expected trigger time of each armed schedule reminder alarm so that
 * ScheduleReminderManager.rescheduleAll can detect reminders whose alarm silently failed to
 * fire (process killed, OEM alarm throttling, missed link in the reschedule chain, etc.) and
 * log the miss instead of the chain simply dying forever.
 */
object ReminderStateStore {
    private const val PREFS_NAME = "notivib_reminder_prefs"

    data class Expected(val ruleId: String, val isStart: Boolean, val millis: Long, val ruleName: String)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun key(ruleId: String, isStart: Boolean): String {
        return "$ruleId|${if (isStart) "START" else "END"}"
    }

    fun setExpected(context: Context, ruleId: String, isStart: Boolean, millis: Long, ruleName: String) {
        getPrefs(context).edit().putString(key(ruleId, isStart), "$millis|$ruleName").apply()
    }

    fun getExpected(context: Context, ruleId: String, isStart: Boolean): Expected? {
        val raw = getPrefs(context).getString(key(ruleId, isStart), null) ?: return null
        return parseEntry(ruleId, isStart, raw)
    }

    fun clearExpected(context: Context, ruleId: String, isStart: Boolean) {
        getPrefs(context).edit().remove(key(ruleId, isStart)).apply()
    }

    fun clearAllForRule(context: Context, ruleId: String) {
        clearExpected(context, ruleId, true)
        clearExpected(context, ruleId, false)
    }

    fun allExpired(context: Context, nowMillis: Long, graceMillis: Long): List<Expected> {
        val result = mutableListOf<Expected>()
        for ((rawKey, rawValue) in getPrefs(context).all) {
            // Split on the LAST '|' so an imported rule id that itself contains '|' still parses.
            val separator = rawKey.lastIndexOf('|')
            if (separator < 0) continue
            val ruleId = rawKey.substring(0, separator)
            val isStart = rawKey.substring(separator + 1) == "START"
            val value = rawValue as? String ?: continue
            val entry = parseEntry(ruleId, isStart, value) ?: continue
            if (entry.millis < nowMillis - graceMillis) {
                result.add(entry)
            }
        }
        return result
    }

    private fun parseEntry(ruleId: String, isStart: Boolean, raw: String): Expected? {
        val parts = raw.split("|", limit = 2)
        val millis = parts.getOrNull(0)?.toLongOrNull() ?: return null
        val ruleName = parts.getOrNull(1) ?: ""
        return Expected(ruleId, isStart, millis, ruleName)
    }
}
