package com.example.notivib.domain.repository

import com.example.notivib.domain.model.AlarmRule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getRules(): Flow<List<AlarmRule>>
    suspend fun saveRule(rule: AlarmRule)
    suspend fun deleteRule(ruleId: String)
}
