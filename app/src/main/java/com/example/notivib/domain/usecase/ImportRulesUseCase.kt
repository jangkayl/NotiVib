package com.example.notivib.domain.usecase

import com.example.notivib.domain.model.RuleSerialization
import com.example.notivib.domain.repository.RuleRepository
import javax.inject.Inject

/**
 * Parses a rules backup JSON string and saves each rule through [RuleRepository], the same
 * path used when editing a rule in the UI, so scheduling/reminders stay consistent.
 *
 * Merge semantics: rules are merged by [com.example.notivib.domain.model.AlarmRule.id] —
 * an imported rule whose id matches an existing rule REPLACES it, an imported rule with a new
 * id is ADDED. Existing rules not present in the import file are left untouched (no
 * wipe-and-replace). If the same id appears more than once in the import file, the last
 * occurrence wins, since rules are saved in file order.
 *
 * Parsing is all-or-nothing: if [json] is malformed, nothing is saved and the failure is
 * returned to the caller to surface.
 */
class ImportRulesUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    suspend operator fun invoke(json: String): Result<Int> {
        return try {
            val rules = RuleSerialization.parseRules(json)
            rules.forEach { rule -> repository.saveRule(rule) }
            Result.success(rules.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
