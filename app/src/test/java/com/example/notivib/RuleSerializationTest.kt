package com.example.notivib

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.RuleSerialization
import com.example.notivib.domain.model.TimeWindow
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSerializationTest {

    @Test
    fun emptyList_roundTrips() {
        val json = RuleSerialization.serializeRules(emptyList())
        assertEquals("[]", json)
        assertEquals(emptyList<AlarmRule>(), RuleSerialization.parseRules(json))
    }

    @Test
    fun ruleList_survivesSerializeThenParse_unchanged() {
        val rules = listOf(
            AlarmRule(
                id = "rule-1",
                ruleName = "Work Alerts",
                targetPackage = "com.slack",
                keyword = "urgent|||deadline",
                startTimeMinute = 480,
                endTimeMinute = 1020,
                vibrationOnly = true,
                isActive = true,
                activeDays = setOf(1, 2, 3, 4, 5),
                hasCustomTimeWindows = true,
                customTimeWindows = mapOf(
                    6 to TimeWindow(startTimeMinute = 600, endTimeMinute = 720),
                    7 to TimeWindow(startTimeMinute = 0, endTimeMinute = 0)
                ),
                muteOutsideSchedule = true,
                remindSchedule = true,
                ignoredKeywords = "spam,promo"
            ),
            AlarmRule(
                id = "rule-2",
                ruleName = "",
                targetPackage = "",
                keyword = "",
                startTimeMinute = 0,
                endTimeMinute = 1439,
                vibrationOnly = false,
                isActive = false,
                activeDays = setOf(1, 2, 3, 4, 5, 6, 7),
                hasCustomTimeWindows = false,
                customTimeWindows = emptyMap(),
                muteOutsideSchedule = false,
                remindSchedule = false,
                ignoredKeywords = ""
            )
        )

        val json = RuleSerialization.serializeRules(rules)
        val parsed = RuleSerialization.parseRules(json)

        assertEquals(rules, parsed)
    }

    @Test
    fun malformedJson_throwsRatherThanSwallowingError() {
        assertThrows(JSONException::class.java) {
            RuleSerialization.parseRules("not valid json")
        }
    }

    @Test
    fun missingRequiredField_throws() {
        // "targetPackage" is required (obj.getString), so a rule object missing it must fail
        // parsing rather than silently defaulting.
        val malformed = """[{"id":"rule-1","keyword":"x","startTimeMinute":0,"endTimeMinute":1}]"""
        assertThrows(JSONException::class.java) {
            RuleSerialization.parseRules(malformed)
        }
    }

    @Test
    fun duplicateIdsInImport_lastOccurrenceWins() {
        // Mirrors ImportRulesUseCase's merge semantics: when the same id appears twice in a
        // backup file, sequential saves mean the later entry's fields are what's kept.
        val json = """
            [
              {"id":"dup","ruleName":"First","targetPackage":"a","keyword":"k","startTimeMinute":0,"endTimeMinute":1},
              {"id":"dup","ruleName":"Second","targetPackage":"b","keyword":"k","startTimeMinute":0,"endTimeMinute":1}
            ]
        """.trimIndent()
        val parsed = RuleSerialization.parseRules(json)
        assertEquals(2, parsed.size)
        assertTrue(parsed.all { it.id == "dup" })
        // The use-case applies these via sequential repository.saveRule() calls keyed by id,
        // so the last list entry ("Second") is what ends up persisted.
        assertEquals("Second", parsed.last().ruleName)
    }
}
