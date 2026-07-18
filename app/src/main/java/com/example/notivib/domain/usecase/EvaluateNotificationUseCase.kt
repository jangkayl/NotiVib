package com.example.notivib.domain.usecase

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.RuleRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalTime
import javax.inject.Inject

sealed class EvaluationResult {
    data class TriggerAlarm(val rule: AlarmRule) : EvaluationResult()
    data class Mute(val rule: AlarmRule) : EvaluationResult()
    object Ignore : EvaluationResult()
}

class EvaluateNotificationUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    suspend fun evaluate(packageName: String, appName: String, title: String, text: String): EvaluationResult {
        val rules = repository.getRules().firstOrNull() ?: return EvaluationResult.Ignore
        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute

        var pendingMute: EvaluationResult.Mute? = null

        for (rule in rules) {
            if (!rule.isActive) continue
            val currentDay = java.time.LocalDate.now().dayOfWeek.value
            
            val matchApp = rule.targetPackage.isNotEmpty() && (
                           packageName.contains(rule.targetPackage, ignoreCase = true) ||
                           appName.contains(rule.targetPackage, ignoreCase = true))

            if (!rule.activeDays.contains(currentDay)) {
                if (rule.muteOutsideSchedule && matchApp) {
                    pendingMute = EvaluationResult.Mute(rule)
                }
                continue
            }

            val keywords = rule.keyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matchKeyword = keywords.isEmpty() || keywords.any { kw ->
                title.contains(kw, ignoreCase = true) || text.contains(kw, ignoreCase = true)
            }

            if (matchApp || (rule.targetPackage.isEmpty() && matchKeyword && rule.keyword.isNotEmpty())) {
                val startMinute = if (rule.hasCustomTimeWindows && rule.customTimeWindows.containsKey(currentDay)) {
                    rule.customTimeWindows[currentDay]!!.startTimeMinute
                } else {
                    rule.startTimeMinute
                }

                val endMinute = if (rule.hasCustomTimeWindows && rule.customTimeWindows.containsKey(currentDay)) {
                    rule.customTimeWindows[currentDay]!!.endTimeMinute
                } else {
                    rule.endTimeMinute
                }

                val isWithinTime = if (startMinute == 0 && endMinute == 1440) {
                    true
                } else if (startMinute < endMinute) {
                    currentMinutes in startMinute..endMinute
                } else if (startMinute > endMinute) {
                    currentMinutes >= startMinute || currentMinutes <= endMinute
                } else {
                    true
                }

                if (isWithinTime) {
                    if (matchKeyword) {
                        return EvaluationResult.TriggerAlarm(rule)
                    }
                } else if (rule.muteOutsideSchedule && matchApp) {
                    pendingMute = EvaluationResult.Mute(rule)
                }
            }
        }
        
        return pendingMute ?: EvaluationResult.Ignore
    }
}
