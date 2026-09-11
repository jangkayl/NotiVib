package com.example.notivib.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.RuleSerialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rules_prefs")

class RulesDataStore(private val context: Context) {

    private val RULES_KEY = stringPreferencesKey("rules")
    private val IS_ISO_MIGRATED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_iso_days_migrated_v1")

    val rulesFlow: Flow<List<AlarmRule>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[RULES_KEY] ?: "[]"
        parseRules(jsonString)
    }

    /**
     * One-time migration: converts stored activeDays and customTimeWindows keys
     * from legacy Calendar convention (1=Sunday, 2=Monday, ..., 7=Saturday)
     * to ISO-8601 convention (1=Monday, 2=Tuesday, ..., 7=Sunday).
     * Call this once on app startup. Safe to call multiple times — it's a no-op
     * after the first successful migration.
     */
    suspend fun migrateIfNeeded() {
        context.dataStore.edit { preferences ->
            val isMigrated = preferences[IS_ISO_MIGRATED_KEY] ?: false
            if (isMigrated) return@edit
            val currentRules = parseRules(preferences[RULES_KEY] ?: "[]")
            if (currentRules.isNotEmpty()) {
                val migratedRules = currentRules.map { rule ->
                    val newActiveDays = rule.activeDays.map { if (it == 1) 7 else it - 1 }.toSet()
                    val newCustomWindows = rule.customTimeWindows.mapKeys { (day, _) ->
                        if (day == 1) 7 else day - 1
                    }
                    rule.copy(activeDays = newActiveDays, customTimeWindows = newCustomWindows)
                }
                preferences[RULES_KEY] = serializeRules(migratedRules)
            }
            preferences[IS_ISO_MIGRATED_KEY] = true
        }
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

    // Delegates to RuleSerialization (the single source of truth for the on-disk/export JSON
    // format). parseRules there throws on malformed JSON; here we fall back to an empty list
    // to preserve this class's prior best-effort behavior for on-device reads.
    private fun parseRules(jsonString: String): List<AlarmRule> {
        return try {
            RuleSerialization.parseRules(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun serializeRules(rules: List<AlarmRule>): String = RuleSerialization.serializeRules(rules)
}
