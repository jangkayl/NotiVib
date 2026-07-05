package com.example.notivib.presentation.rules_list

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.NotificationLog
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.usecase.DeleteRuleUseCase
import com.example.notivib.domain.usecase.GetRulesUseCase
import com.example.notivib.domain.usecase.SaveRuleUseCase
import com.example.notivib.framework.receiver.ScheduleReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        id: String?, targetPackage: String, keyword: String, start: Int, end: Int, 
        vibrationOnly: Boolean, isActive: Boolean, activeDays: Set<Int>,
        hasCustomWindows: Boolean, customWindows: Map<Int, com.example.notivib.domain.model.TimeWindow>
    ) {
        viewModelScope.launch {
            saveRuleUseCase(
                AlarmRule(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    targetPackage = targetPackage,
                    keyword = keyword,
                    startTimeMinute = start,
                    endTimeMinute = end,
                    vibrationOnly = vibrationOnly,
                    isActive = isActive,
                    activeDays = activeDays,
                    hasCustomTimeWindows = hasCustomWindows,
                    customTimeWindows = customWindows
                )
            )
            triggerEvaluation()
        }
    }

    fun toggleRuleActive(rule: AlarmRule, isActive: Boolean) {
        viewModelScope.launch {
            saveRuleUseCase(rule.copy(isActive = isActive))
            triggerEvaluation()
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            deleteRuleUseCase(ruleId)
            triggerEvaluation()
        }
    }

    fun clearSystemLogs() = notificationLogRepository.clearSystemLogs()
    fun deleteSystemLog(log: String) = notificationLogRepository.deleteSystemLog(log)
    fun clearInterceptLogs() = notificationLogRepository.clearInterceptLogs()
    fun deleteInterceptLog(log: NotificationLog) = notificationLogRepository.deleteInterceptLog(log)
}
