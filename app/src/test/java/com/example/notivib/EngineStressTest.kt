package com.example.notivib

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.TimeWindow
import com.example.notivib.domain.usecase.EvaluationResult
import com.example.notivib.domain.usecase.EvaluateNotificationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class EngineStressTest {

    @Test
    fun stressTestNotificationEvaluationEngine_10kNotifications() = runBlocking {
        // Construct 50 complex rules with multiple keywords and custom time windows
        val rules = (1..50).map { i ->
            AlarmRule(
                id = "rule_$i",
                ruleName = "Stress Rule $i",
                targetPackage = if (i % 2 == 0) "com.whatsapp" else "ANY",
                keyword = (1..15).joinToString("|||") { "keyword_$it" },
                startTimeMinute = 0,
                endTimeMinute = 1439,
                ignoredKeywords = (1..15).joinToString("|||") { "ignore_$it" },
                activeDays = setOf(1, 2, 3, 4, 5, 6, 7),
                hasCustomTimeWindows = true,
                customTimeWindows = (1..7).associateWith { TimeWindow(0, 1439) },
                vibrationOnly = i % 2 == 0,
                muteOutsideSchedule = true
            )
        }

        val repo = FakeRuleRepository(rules)
        val useCase = EvaluateNotificationUseCase(repo)

        val totalNotifications = 10_000
        val concurrencyLevel = 10

        println("Starting Stress Test: Processing $totalNotifications notifications across $concurrencyLevel concurrent coroutines...")

        val executionTimeMs = measureTimeMillis {
            val chunkCount = totalNotifications / concurrencyLevel
            val jobs = (1..concurrencyLevel).map { workerId ->
                async(Dispatchers.Default) {
                    var triggerCount = 0
                    var ignoreCount = 0

                    for (j in 1..chunkCount) {
                        val isTrigger = j % 3 == 0
                        val title = if (isTrigger) "Important update containing keyword_5" else "Regular message $j"
                        val text = if (j % 5 == 0) "Also contains ignore_3" else "Normal content body"

                        val result = useCase.evaluate(
                            packageName = if (j % 2 == 0) "com.whatsapp" else "com.slack",
                            appName = "App_$j",
                            title = title,
                            text = text
                        )

                        if (result is EvaluationResult.TriggerAlarm) {
                            triggerCount++
                        } else {
                            ignoreCount++
                        }
                    }
                    triggerCount to ignoreCount
                }
            }

            val results = jobs.awaitAll()
            val totalProcessed = results.sumOf { it.first + it.second }
            assertEquals(totalNotifications, totalProcessed)
        }

        val avgTimePerNotificationMs = executionTimeMs.toDouble() / totalNotifications
        println("Stress Test Completed Successfully!")
        println("Total Time: ${executionTimeMs} ms for $totalNotifications notifications")
        println("Average Evaluation Latency: ${String.format("%.4f", avgTimePerNotificationMs)} ms per notification")
        println("Throughput: ${String.format("%.0f", (totalNotifications.toDouble() / executionTimeMs) * 1000)} evaluations/sec")

        // Assert average evaluation latency is well under 1ms per notification
        assertTrue("Evaluation latency must be < 1.0ms per notification", avgTimePerNotificationMs < 1.0)
    }
}
