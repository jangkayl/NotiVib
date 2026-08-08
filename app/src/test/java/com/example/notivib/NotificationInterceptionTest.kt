package com.example.notivib

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.TimeWindow
import com.example.notivib.domain.repository.RuleRepository
import com.example.notivib.domain.usecase.EvaluationResult
import com.example.notivib.domain.usecase.EvaluateNotificationUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeRuleRepository(private val rulesList: List<AlarmRule>) : RuleRepository {
    override fun getRules() = flowOf(rulesList)
    override suspend fun saveRule(rule: AlarmRule) {}
    override suspend fun deleteRule(ruleId: String) {}
}

class NotificationInterceptionTest {

    private val currentDay = LocalDate.now().dayOfWeek.value

    @Test
    fun testTriggerAlarm_whenTriggerKeywordMatches() = runBlocking {
        val rule = AlarmRule(
            id = "1",
            ruleName = "Work Alert",
            targetPackage = "com.whatsapp",
            keyword = "urgent|||emergency",
            ignoredKeywords = "spam",
            activeDays = setOf(currentDay),
            startTimeMinute = 0,
            endTimeMinute = 1439
        )
        val repo = FakeRuleRepository(listOf(rule))
        val useCase = EvaluateNotificationUseCase(repo)

        val result = useCase.evaluate(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Urgent message from Boss",
            text = "Please check this now"
        )

        assertTrue(result is EvaluationResult.TriggerAlarm)
        val triggerResult = result as EvaluationResult.TriggerAlarm
        assertEquals(listOf("urgent"), triggerResult.matchedKeywords)
    }

    @Test
    fun testIgnoredKeyword_preventsAlarmTrigger() = runBlocking {
        val rule = AlarmRule(
            id = "2",
            ruleName = "Work Alert",
            targetPackage = "com.whatsapp",
            keyword = "urgent",
            ignoredKeywords = "spam|||promo",
            activeDays = setOf(currentDay),
            startTimeMinute = 0,
            endTimeMinute = 1439
        )
        val repo = FakeRuleRepository(listOf(rule))
        val useCase = EvaluateNotificationUseCase(repo)

        val result = useCase.evaluate(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Urgent PROMO offer!",
            text = "Get 50% off today (spam)"
        )

        // Ignored keyword "promo" or "spam" should prevent triggering alarm
        assertTrue(result is EvaluationResult.Ignore)
    }

    @Test
    fun testMuteOutsideSchedule_whenTimeIsOutsideActiveSchedule() = runBlocking {
        val rule = AlarmRule(
            id = "3",
            ruleName = "Night Shift",
            targetPackage = "com.slack",
            keyword = "meeting",
            activeDays = setOf(currentDay),
            startTimeMinute = 1,  // Not minute 0
            endTimeMinute = 2,    // Window 00:01..00:02
            muteOutsideSchedule = true
        )
        val repo = FakeRuleRepository(listOf(rule))
        val useCase = EvaluateNotificationUseCase(repo)

        // Evaluate at current time (assuming current time is outside 00:01..00:02 unless testing at exactly 00:01)
        val currentMin = java.time.LocalTime.now().hour * 60 + java.time.LocalTime.now().minute
        if (currentMin !in 1..2) {
            val result = useCase.evaluate(
                packageName = "com.slack",
                appName = "Slack",
                title = "Meeting at 3pm",
                text = "See you there"
            )
            assertTrue("Expected Mute outside active window", result is EvaluationResult.Mute)
        }
    }

    @Test
    fun testNonTargetApp_isIgnored() = runBlocking {
        val rule = AlarmRule(
            id = "4",
            ruleName = "WhatsApp Only",
            targetPackage = "com.whatsapp",
            keyword = "urgent",
            activeDays = setOf(currentDay),
            startTimeMinute = 0,
            endTimeMinute = 1439
        )
        val repo = FakeRuleRepository(listOf(rule))
        val useCase = EvaluateNotificationUseCase(repo)

        val result = useCase.evaluate(
            packageName = "com.telegram",
            appName = "Telegram",
            title = "Urgent message",
            text = "Hello"
        )

        assertTrue(result is EvaluationResult.Ignore)
    }
}
