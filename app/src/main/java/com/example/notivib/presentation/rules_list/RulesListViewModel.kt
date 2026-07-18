package com.example.notivib.presentation.rules_list

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.model.TimeWindow
import com.example.notivib.domain.repository.NotificationLog
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.usecase.DeleteRuleUseCase
import com.example.notivib.domain.usecase.GetRulesUseCase
import com.example.notivib.domain.usecase.SaveRuleUseCase
import com.example.notivib.domain.manager.ScheduleReminderManager
import com.example.notivib.framework.receiver.ScheduleReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesListViewModel @Inject constructor(
    private val getRulesUseCase: GetRulesUseCase,
    private val saveRuleUseCase: SaveRuleUseCase,
    private val deleteRuleUseCase: DeleteRuleUseCase,
    private val notificationLogRepository: NotificationLogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    init {
        triggerEvaluation()
    }

    private fun triggerEvaluation() {
        val intent = Intent(context, ScheduleReceiver::class.java)
        context.sendBroadcast(intent)
    }

    val rules: StateFlow<List<AlarmRule>> = getRulesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<NotificationLog>> = notificationLogRepository.logs
    val systemLogs: StateFlow<List<String>> = notificationLogRepository.systemLogs

    fun saveRule(
        id: String?, 
        targetPackage: String, 
        keyword: String, 
        startTimeMinute: Int, 
        endTimeMinute: Int, 
        vibrationOnly: Boolean,
        isActive: Boolean = true,
        activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
        hasCustomTimeWindows: Boolean = false,
        customTimeWindows: Map<Int, TimeWindow> = emptyMap(),
        muteOutsideSchedule: Boolean = false,
        remindSchedule: Boolean = false
    ) {
        val ruleId = id ?: java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            saveRuleUseCase(
                AlarmRule(
                    id = ruleId,
                    targetPackage = targetPackage,
                    keyword = keyword,
                    startTimeMinute = startTimeMinute,
                    endTimeMinute = endTimeMinute,
                    vibrationOnly = vibrationOnly,
                    isActive = isActive,
                    activeDays = activeDays,
                    hasCustomTimeWindows = hasCustomTimeWindows,
                    customTimeWindows = customTimeWindows,
                    muteOutsideSchedule = muteOutsideSchedule,
                    remindSchedule = remindSchedule
                )
            )
            triggerEvaluation()
            // Wait for save to complete, but rule ID is deterministic or provided
            val updatedRules = getRulesUseCase().firstOrNull() ?: emptyList()
            val savedRule = updatedRules.find { it.id == ruleId }
            if (savedRule != null) {
                ScheduleReminderManager.scheduleForRule(context, savedRule)
            }
        }
    }

    fun toggleRuleActive(rule: AlarmRule, isActive: Boolean) {
        viewModelScope.launch {
            saveRuleUseCase(rule.copy(isActive = isActive))
            triggerEvaluation()
            ScheduleReminderManager.scheduleForRule(context, rule.copy(isActive = isActive))
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            deleteRuleUseCase(ruleId)
            triggerEvaluation()
            ScheduleReminderManager.cancelAlarm(context, ruleId)
        }
    }

    fun clearSystemLogs() = notificationLogRepository.clearSystemLogs()
    fun deleteSystemLog(log: String) = notificationLogRepository.deleteSystemLog(log)
    fun clearInterceptLogs() = notificationLogRepository.clearInterceptLogs()
    fun deleteInterceptLog(log: NotificationLog) = notificationLogRepository.deleteInterceptLog(log)
}
