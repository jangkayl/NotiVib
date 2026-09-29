package com.example.notivib

import android.app.Application
import android.os.PowerManager
import com.example.notivib.data.local.RulesDataStore
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.framework.utils.BatteryOptimizationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NotiVibApp : Application() {

    @Inject
    lateinit var rulesDataStore: RulesDataStore

    @Inject
    lateinit var notificationLogRepository: NotificationLogRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            rulesDataStore.migrateIfNeeded()
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        notificationLogRepository.addSystemLog(
            "[Engine Diagnostic] App started — battery optimization exempt=" +
                "${BatteryOptimizationHelper.isReallyIgnoring(this)}, powerSaveMode=${powerManager.isPowerSaveMode}"
        )
    }
}
