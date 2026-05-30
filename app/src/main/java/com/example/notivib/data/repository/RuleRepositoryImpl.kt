package com.example.notivib.data.repository

import com.example.notivib.data.local.RulesDataStore
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val dataStore: RulesDataStore
) : RuleRepository {
    override fun getRules(): Flow<List<AlarmRule>> = dataStore.rulesFlow
    override suspend fun saveRule(rule: AlarmRule) = dataStore.saveRule(rule)
    override suspend fun deleteRule(ruleId: String) = dataStore.deleteRule(ruleId)
}
