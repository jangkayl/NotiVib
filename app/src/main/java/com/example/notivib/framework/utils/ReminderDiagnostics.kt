package com.example.notivib.framework.utils

import android.content.Context
import com.example.notivib.domain.repository.NotificationLogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Lets plain (non-Hilt-injected) call sites — e.g. the ScheduleReminderManager/ScheduleManager
 * singleton objects and ActiveAlarmService — write diagnostics to the same
 * NotificationLogRepository singleton used elsewhere, without needing constructor injection.
 */
object ReminderDiagnostics {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationLogRepositoryEntryPoint {
        fun notificationLogRepository(): NotificationLogRepository
    }

    fun log(context: Context, message: String) {
        try {
            val repository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                NotificationLogRepositoryEntryPoint::class.java
            ).notificationLogRepository()
            repository.addSystemLog(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
