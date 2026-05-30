package com.example.notivib.domain.usecase

import com.example.notivib.domain.repository.RuleRepository
import javax.inject.Inject

class DeleteRuleUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    suspend operator fun invoke(ruleId: String) = repository.deleteRule(ruleId)
}
