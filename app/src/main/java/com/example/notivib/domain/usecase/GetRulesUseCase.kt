package com.example.notivib.domain.usecase

import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRulesUseCase @Inject constructor(
    private val repository: RuleRepository
) {
    operator fun invoke(): Flow<List<AlarmRule>> = repository.getRules()
}
