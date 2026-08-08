package com.example.notivib

import com.example.notivib.framework.service.ActiveAlarmService
import com.example.notivib.presentation.alarm.Captcha
import com.example.notivib.presentation.alarm.generateCaptcha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmAndInterceptionFlowTest {

    @Test
    fun testAlarmModesConstants() {
        assertEquals(0, ActiveAlarmService.MODE_INTERCEPT)
        assertEquals(1, ActiveAlarmService.MODE_SCHEDULE_START)
        assertEquals(2, ActiveAlarmService.MODE_SCHEDULE_END)
        assertEquals(3, ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP)
        assertEquals(4, ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP)
    }

    @Test
    fun testCaptchaGeneration() {
        val captcha = generateCaptcha()
        assertNotNull(captcha.prompt)
        assertNotNull(captcha.answer)
        assertTrue(captcha.prompt.isNotEmpty())
        assertTrue(captcha.answer.isNotEmpty())

        // Test matching captcha input (case-insensitive)
        assertTrue(captcha.answer.equals(captcha.answer.lowercase(), ignoreCase = true))
        assertFalse(captcha.answer.equals("wrong_answer_xyz", ignoreCase = true))
    }

    @Test
    fun testFollowUpModeDetection() {
        fun isFollowUpMode(mode: Int): Boolean {
            return mode == ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP ||
                   mode == ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP
        }

        assertTrue(isFollowUpMode(ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP))
        assertTrue(isFollowUpMode(ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP))
        assertFalse(isFollowUpMode(ActiveAlarmService.MODE_SCHEDULE_START))
        assertFalse(isFollowUpMode(ActiveAlarmService.MODE_SCHEDULE_END))
        assertFalse(isFollowUpMode(ActiveAlarmService.MODE_INTERCEPT))
    }

    @Test
    fun testAcknowledgeRequirement_forStandardVsFollowUp() {
        fun isAcknowledgeEnabled(mode: Int, userInput: String, captcha: Captcha): Boolean {
            val isFollowUp = mode == ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP ||
                             mode == ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP
            return if (isFollowUp) userInput.equals(captcha.answer, ignoreCase = true) else true
        }

        val testCaptcha = Captcha("What is 10 + 20?", "30")

        // Standard Interception Alert: Acknowledge enabled immediately
        assertTrue(isAcknowledgeEnabled(ActiveAlarmService.MODE_INTERCEPT, "", testCaptcha))

        // Rule Started: Acknowledge enabled immediately
        assertTrue(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_START, "", testCaptcha))

        // Rule Ended: Acknowledge enabled immediately
        assertTrue(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_END, "", testCaptcha))

        // Rule Started Follow-up: Acknowledge disabled until correct captcha answer
        assertFalse(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP, "", testCaptcha))
        assertFalse(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP, "wrong", testCaptcha))
        assertTrue(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP, "30", testCaptcha))

        // Rule Ended Follow-up: Acknowledge disabled until correct captcha answer
        assertFalse(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP, "", testCaptcha))
        assertTrue(isAcknowledgeEnabled(ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP, "30", testCaptcha))
    }
}
