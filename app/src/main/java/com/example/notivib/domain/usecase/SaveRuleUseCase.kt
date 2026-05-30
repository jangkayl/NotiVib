package com.example.notivib.domain.usecase

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.RuleRepository
import javax.inject.Inject

class SaveRuleUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    suspend operator fun invoke(rule: AlarmRule) = repository.saveRule(rule)
}
