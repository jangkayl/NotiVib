package com.example.notivib.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.TimeWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "rules_prefs")

class RulesDataStore(private val context: Context) {

    private val RULES_KEY = stringPreferencesKey("rules")

    val rulesFlow: Flow<List<AlarmRule>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[RULES_KEY] ?: "[]"
        parseRules(jsonString)
    }

    suspend fun saveRule(rule: AlarmRule) {
        context.dataStore.edit { preferences ->
            val currentRules = parseRules(preferences[RULES_KEY] ?: "[]").toMutableList()
            val existingIndex = currentRules.indexOfFirst { it.id == rule.id }
            if (existingIndex >= 0) {
                currentRules[existingIndex] = rule
            } else {
                currentRules.add(rule)
            }
            preferences[RULES_KEY] = serializeRules(currentRules)
        }
    }

    suspend fun deleteRule(ruleId: String) {
        context.dataStore.edit { preferences ->
            val currentRules = parseRules(preferences[RULES_KEY] ?: "[]").toMutableList()
            currentRules.removeAll { it.id == ruleId }
            preferences[RULES_KEY] = serializeRules(currentRules)
        }
    }

    private fun parseRules(jsonString: String): List<AlarmRule> {
        val list = mutableListOf<AlarmRule>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AlarmRule(
                        id = obj.getString("id"),
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
                        hasCustomTimeWindows = obj.optBoolean("hasCustomTimeWindows", false),
                        customTimeWindows = obj.optJSONObject("customTimeWindows")?.let { customWindowsObj ->
                            val map = mutableMapOf<Int, TimeWindow>()
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
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private fun serializeRules(rules: List<AlarmRule>): String {
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject()
            obj.put("id", rule.id)
            obj.put("targetPackage", rule.targetPackage)
            obj.put("keyword", rule.keyword)
            obj.put("startTimeMinute", rule.startTimeMinute)
            obj.put("endTimeMinute", rule.endTimeMinute)
            obj.put("vibrationOnly", rule.vibrationOnly)
            obj.put("isActive", rule.isActive)
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
}
