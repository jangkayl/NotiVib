package com.example.notivib.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for converting [AlarmRule]s to/from their JSON representation.
 *
 * This is pure org.json with no Android framework dependency, so it can be exercised by
 * plain JVM unit tests. [com.example.notivib.data.local.RulesDataStore] uses this for its
 * on-disk persistence format, and rule export/import (see `domain/usecase`) reuses the same
 * functions so the backup file format never drifts from what's stored on-device.
 *
 * [parseRules] throws on malformed input rather than silently swallowing errors, so callers
 * that need "all or nothing" semantics (e.g. import) can catch and abort cleanly. Callers that
 * want a best-effort read (e.g. the on-device DataStore) should catch and fall back themselves.
 */
object RuleSerialization {

    fun serializeRules(rules: List<AlarmRule>): String {
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject()
            obj.put("id", rule.id)
            obj.put("ruleName", rule.ruleName)
            obj.put("targetPackage", rule.targetPackage)
            obj.put("keyword", rule.keyword)
            obj.put("startTimeMinute", rule.startTimeMinute)
            obj.put("endTimeMinute", rule.endTimeMinute)
            obj.put("vibrationOnly", rule.vibrationOnly)
            obj.put("isActive", rule.isActive)
            obj.put("muteOutsideSchedule", rule.muteOutsideSchedule)
            obj.put("remindSchedule", rule.remindSchedule)
            obj.put("ignoredKeywords", rule.ignoredKeywords)
            obj.put("activeDays", JSONArray(rule.activeDays))
            obj.put("hasCustomTimeWindows", rule.hasCustomTimeWindows)
            val customWindowsObj = JSONObject()
            rule.customTimeWindows.forEach { (day, window) ->
                val windowObj = JSONObject()
                windowObj.put("startTimeMinute", window.startTimeMinute)
                windowObj.put("endTimeMinute", window.endTimeMinute)
                customWindowsObj.put(day.toString(), windowObj)
            }
            obj.put("customTimeWindows", customWindowsObj)
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * @throws org.json.JSONException if [jsonString] is not a valid rules array.
     */
    fun parseRules(jsonString: String): List<AlarmRule> {
        val list = mutableListOf<AlarmRule>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                AlarmRule(
                    id = obj.getString("id"),
                    ruleName = obj.optString("ruleName", ""),
                    targetPackage = obj.getString("targetPackage"),
                    keyword = obj.getString("keyword"),
                    startTimeMinute = obj.getInt("startTimeMinute"),
                    endTimeMinute = obj.getInt("endTimeMinute"),
                    vibrationOnly = obj.optBoolean("vibrationOnly", false),
                    isActive = obj.optBoolean("isActive", true),
                    activeDays = obj.optJSONArray("activeDays")?.let { arr ->
                        val days = mutableSetOf<Int>()
                        for (j in 0 until arr.length()) days.add(arr.getInt(j))
                        days
                    } ?: setOf(1, 2, 3, 4, 5, 6, 7),
                    muteOutsideSchedule = obj.optBoolean("muteOutsideSchedule", false),
                    remindSchedule = obj.optBoolean("remindSchedule", false),
                    ignoredKeywords = obj.optString("ignoredKeywords", ""),
                    hasCustomTimeWindows = obj.optBoolean("hasCustomTimeWindows", false),
                    customTimeWindows = obj.optJSONObject("customTimeWindows")?.let { customWindowsObj ->
                        val map = mutableMapOf<Int, com.example.notivib.domain.model.TimeWindow>()
                        val keys = customWindowsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val windowObj = customWindowsObj.getJSONObject(key)
                            map[key.toInt()] = TimeWindow(
                                startTimeMinute = windowObj.getInt("startTimeMinute"),
                                endTimeMinute = windowObj.getInt("endTimeMinute")
                            )
                        }
                        map
                    } ?: emptyMap()
                )
            )
        }
        return list
    }
}
