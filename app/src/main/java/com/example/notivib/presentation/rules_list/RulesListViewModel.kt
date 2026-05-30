package com.example.notivib.presentation.rules_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.NotificationLog
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.usecase.DeleteRuleUseCase
import com.example.notivib.domain.usecase.GetRulesUseCase
import com.example.notivib.domain.usecase.SaveRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val notificationLogRepository: NotificationLogRepository
) : ViewModel() {

    val rules: StateFlow<List<AlarmRule>> = getRulesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<NotificationLog>> = notificationLogRepository.logs
    val systemLogs: StateFlow<List<String>> = notificationLogRepository.systemLogs

    fun saveRule(id: String?, targetPackage: String, keyword: String, start: Int, end: Int, vibrationOnly: Boolean) {
        viewModelScope.launch {
            saveRuleUseCase(
                AlarmRule(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    targetPackage = targetPackage,
                    keyword = keyword,
                    startTimeMinute = start,
                    endTimeMinute = end,
                    vibrationOnly = vibrationOnly
                )
            )
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            deleteRuleUseCase(ruleId)
        }
    }

    fun clearSystemLogs() = notificationLogRepository.clearSystemLogs()
    fun deleteSystemLog(log: String) = notificationLogRepository.deleteSystemLog(log)
    fun clearInterceptLogs() = notificationLogRepository.clearInterceptLogs()
    fun deleteInterceptLog(log: NotificationLog) = notificationLogRepository.deleteInterceptLog(log)
}
