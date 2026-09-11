package com.example.notivib.domain.usecase

import com.example.notivib.domain.model.RuleSerialization
import com.example.notivib.domain.repository.RuleRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Serializes all currently-saved rules to a JSON string, using the same format
 * [com.example.notivib.data.local.RulesDataStore] persists to disk (see [RuleSerialization]).
 * An empty rule list serializes to "[]".
 */
class ExportRulesUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    suspend operator fun invoke(): String {
        val rules = repository.getRules().firstOrNull() ?: emptyList()
        return RuleSerialization.serializeRules(rules)
    }
}
