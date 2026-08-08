package com.example.notivib.data.repository

import com.example.notivib.data.local.RulesDataStore
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val dataStore: RulesDataStore,
    private val logRepository: NotificationLogRepository
) : RuleRepository {

    override fun getRules(): Flow<List<AlarmRule>> = dataStore.rulesFlow

    override suspend fun saveRule(rule: AlarmRule) {
        val existingRules = dataStore.rulesFlow.firstOrNull() ?: emptyList()
        val existingRule = existingRules.find { it.id == rule.id }
        
        dataStore.saveRule(rule)
        
        val appInfo = rule.targetPackage.ifEmpty { "All Apps" }
        val ruleNameDisplay = rule.ruleName.ifEmpty { "Unnamed Rule" }
        val keywordDisplay = rule.keyword.ifEmpty { "Any Keyword" }
        
        if (existingRule == null) {
            logRepository.addSystemLog(
                "[Engine Diagnostic] Rule Created: '$ruleNameDisplay' (App: $appInfo, Keywords: '$keywordDisplay')"
            )
        } else if (existingRule.isActive != rule.isActive) {
            val statusStr = if (rule.isActive) "ENABLED" else "DISABLED"
            logRepository.addSystemLog(
                "[Engine Diagnostic] Rule Status Changed ($statusStr): '$ruleNameDisplay'"
            )
        } else {
            logRepository.addSystemLog(
                "[Engine Diagnostic] Rule Updated: '$ruleNameDisplay' (App: $appInfo, Keywords: '$keywordDisplay')"
            )
        }
    }

    override suspend fun deleteRule(ruleId: String) {
        val existingRules = dataStore.rulesFlow.firstOrNull() ?: emptyList()
        val deletedRule = existingRules.find { it.id == ruleId }
        val ruleNameDisplay = deletedRule?.ruleName?.ifEmpty { "Unnamed Rule" } ?: "Rule ID $ruleId"
        val appInfo = deletedRule?.targetPackage?.ifEmpty { "All Apps" } ?: "Unknown"

        dataStore.deleteRule(ruleId)
        
        logRepository.addSystemLog(
            "[Engine Diagnostic] Rule Deleted: '$ruleNameDisplay' (App: $appInfo)"
        )
    }
}
