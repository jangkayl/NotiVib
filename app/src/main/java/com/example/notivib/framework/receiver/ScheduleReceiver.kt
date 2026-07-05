package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.notivib.domain.manager.ScheduleManager
import com.example.notivib.domain.repository.RuleRepository
import com.example.notivib.framework.service.EngineForegroundService
import com.example.notivib.framework.utils.EngineState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: RuleRepository

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val rules = repository.getRules().firstOrNull() ?: emptyList()
            val isActive = ScheduleManager.evaluateAndSchedule(context, rules)

            EngineState.setScheduleActive(context, isActive)

            if (EngineState.shouldIntercept(context) && EngineState.isShowForegroundNotification(context)) {
                // Start Foreground Service
                val serviceIntent = Intent(context, EngineForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            } else {
                // Stop Foreground Service
                val serviceIntent = Intent(context, EngineForegroundService::class.java)
                context.stopService(serviceIntent)
            }
        }
    }
}
