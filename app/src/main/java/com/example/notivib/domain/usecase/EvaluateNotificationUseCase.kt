package com.example.notivib.domain.usecase

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.RuleRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalTime
import javax.inject.Inject

class EvaluateNotificationUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    suspend fun evaluate(packageName: String, appName: String, title: String, text: String): AlarmRule? {
        val rules = repository.getRules().firstOrNull() ?: return null
        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute

        for (rule in rules) {
            val isWithinTime = if (rule.startTimeMinute == 0 && rule.endTimeMinute == 1440) {
                true
            } else if (rule.startTimeMinute < rule.endTimeMinute) {
                currentMinutes in rule.startTimeMinute..rule.endTimeMinute
            } else if (rule.startTimeMinute > rule.endTimeMinute) {
                currentMinutes >= rule.startTimeMinute || currentMinutes <= rule.endTimeMinute
            } else {
                true // Full 24-hour period when start and end time are exactly the same
            }

            if (!isWithinTime) continue
            if (!rule.isActive) continue
            val currentDay = java.time.LocalDate.now().dayOfWeek.value
            if (!rule.activeDays.contains(currentDay)) continue

            val matchApp = rule.targetPackage.isEmpty() || 
                           packageName.contains(rule.targetPackage, ignoreCase = true) ||
                           appName.contains(rule.targetPackage, ignoreCase = true)

            val keywords = rule.keyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matchKeyword = keywords.isEmpty() || keywords.any { kw ->
                title.contains(kw, ignoreCase = true) || text.contains(kw, ignoreCase = true)
            }

            if (matchApp && matchKeyword) {
                if (rule.targetPackage.isNotEmpty() || rule.keyword.isNotEmpty()) {
                    return rule
                }
            }
        }
        return null
    }
}
