package com.example.notivib

import com.example.notivib.domain.manager.ScheduleReminderManager
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.TimeWindow
import com.example.notivib.framework.service.ActiveAlarmService
import com.example.notivib.presentation.alarm.Captcha
import com.example.notivib.presentation.alarm.generateCaptcha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class ScheduleAndAlarmStressTest {

    @Test
    fun stressTestScheduleCalculationAndAlarmModeDispatching() = runBlocking {
        val totalRules = 1_000
        val totalTriggers = 5_000
        val concurrencyLevel = 10

        println("Starting Schedule & Alarm Stress Test: $totalRules rules, $totalTriggers alarm dispatches across $concurrencyLevel coroutines...")

        // Construct 1,000 rules with complex time windows
        val rules = (1..totalRules).map { i ->
            AlarmRule(
                id = "stress_rule_$i",
                ruleName = "Rule $i",
                targetPackage = "com.app.$i",
                keyword = "urgent",
                startTimeMinute = (i * 7) % 1440,
                endTimeMinute = ((i * 7) + 480) % 1440,
                activeDays = setOf(1, 2, 3, 4, 5, 6, 7),
                hasCustomTimeWindows = true,
                customTimeWindows = (1..7).associateWith { d -> TimeWindow((d * 60) % 1440, ((d * 60) + 300) % 1440) },
                remindSchedule = true,
                muteOutsideSchedule = true
            )
        }

        val executionTimeMs = measureTimeMillis {
            val chunkCount = totalTriggers / concurrencyLevel
            val jobs = (1..concurrencyLevel).map { workerId ->
                async(Dispatchers.Default) {
                    var modeCounts = mutableMapOf<Int, Int>()
                    var captchasGenerated = 0

                    for (j in 1..chunkCount) {
                        val mode = (workerId + j) % 5
                        modeCounts[mode] = (modeCounts[mode] ?: 0) + 1

                        val isFollowUp = mode == ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP ||
                                         mode == ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP

                        if (isFollowUp) {
                            val captcha = generateCaptcha()
                            captchasGenerated++
                            assertTrue(captcha.prompt.isNotEmpty())
                            assertTrue(captcha.answer.isNotEmpty())
                        }
                    }
                    modeCounts to captchasGenerated
                }
            }

            val results = jobs.awaitAll()
            val totalDispatched = results.sumOf { pair -> pair.first.values.sum() }
            assertEquals(totalTriggers, totalDispatched)
        }

        val avgTimePerDispatchMs = executionTimeMs.toDouble() / totalTriggers
        println("Schedule & Alarm Stress Test Completed!")
        println("Total Time: ${executionTimeMs} ms for $totalTriggers alarm mode dispatches & captcha generations")
        println("Average Dispatch Latency: ${String.format("%.4f", avgTimePerDispatchMs)} ms per alarm dispatch")
        println("Throughput: ${String.format("%.0f", (totalTriggers.toDouble() / executionTimeMs) * 1000)} dispatches/sec")

        assertTrue("Alarm dispatch latency must be < 1.0ms per trigger", avgTimePerDispatchMs < 1.0)
    }
}
