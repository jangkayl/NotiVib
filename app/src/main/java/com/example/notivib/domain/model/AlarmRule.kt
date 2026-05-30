package com.example.notivib.domain.model

import java.util.UUID

data class AlarmRule(
    val id: String = UUID.randomUUID().toString(),
    val targetPackage: String,
    val keyword: String,
    val startTimeMinute: Int,
    val endTimeMinute: Int,
    val vibrationOnly: Boolean = false
)
