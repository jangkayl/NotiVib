package com.example.notivib.domain.model

import java.util.UUID

data class TimeWindow(val startTimeMinute: Int, val endTimeMinute: Int)

data class AlarmRule(
    val id: String = UUID.randomUUID().toString(),
    val targetPackage: String,
    val keyword: String,
    val startTimeMinute: Int,
    val endTimeMinute: Int,
    val vibrationOnly: Boolean = false,
    val isActive: Boolean = true,
    val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val hasCustomTimeWindows: Boolean = false,
    val customTimeWindows: Map<Int, TimeWindow> = emptyMap(),
    val muteOutsideSchedule: Boolean = false
)
