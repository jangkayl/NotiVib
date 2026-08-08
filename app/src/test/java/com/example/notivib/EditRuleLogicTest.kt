package com.example.notivib

import com.example.notivib.domain.model.TimeWindow
import com.example.notivib.presentation.rules_list.calculateOutsideScheduleMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditRuleLogicTest {

    @Test
    fun test247Schedule_returnsZeroOutsideMinutes() {
        val activeDays = setOf(1, 2, 3, 4, 5, 6, 7)
        val minutes = calculateOutsideScheduleMinutes(
            activeDays = activeDays,
            hasCustomTimeWindows = false,
            startTimeMinute = 0,
            endTimeMinute = 1439,
            customTimeWindows = emptyMap()
        )
        assertEquals(0, minutes)
        val hasOutsideSchedule = minutes >= 30
        assertFalse(hasOutsideSchedule)
    }

    @Test
    fun testPartialDaySchedule_outsideMinutesThreshold() {
        val activeDays = setOf(1, 2, 3, 4, 5, 6, 7)
        // 00:00 to 23:30 (30 mins outside per day * 7 days = 210 mins outside)
        val minutes = calculateOutsideScheduleMinutes(
            activeDays = activeDays,
            hasCustomTimeWindows = false,
            startTimeMinute = 0,
            endTimeMinute = 1410,
            customTimeWindows = emptyMap()
        )
        assertTrue(minutes >= 30)
    }

    @Test
    fun test5ActiveDays_returnsOutsideMinutesForInactiveDays() {
        val activeDays = setOf(1, 2, 3, 4, 5) // Mon-Fri
        val minutes = calculateOutsideScheduleMinutes(
            activeDays = activeDays,
            hasCustomTimeWindows = false,
            startTimeMinute = 0,
            endTimeMinute = 1439,
            customTimeWindows = emptyMap()
        )
        // 2 inactive days * 1440 mins = 2880 mins
        assertEquals(2880, minutes)
        assertTrue(minutes >= 30)
    }

    @Test
    fun testKeywordMutualExclusionFiltering() {
        val triggerKeywords = listOf("urgent", "alarm")
        val ignoredKeywordsInput = listOf("urgent", "spam", "ALARM", "promo")

        val triggerLower = triggerKeywords.map { it.lowercase() }.toSet()
        val filteredIgnored = ignoredKeywordsInput.filter { !triggerLower.contains(it.lowercase()) }

        assertEquals(listOf("spam", "promo"), filteredIgnored)
    }
}
