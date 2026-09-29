package com.example.notivib.framework.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import com.example.notivib.framework.receiver.ScheduleReceiver
import com.example.notivib.framework.service.InterceptorService

/**
 * Single code path for "restart the engine", used by both the Restart Engine button in
 * RulesListScreen and EngineForegroundService's notification action. Previously each caller
 * duplicated (and drifted from) this logic and only requested a listener rebind, which does
 * not re-arm the schedule/reminder alarms.
 */
object EngineRestarter {

    fun restart(context: Context) {
        val appContext = context.applicationContext

        try {
            val component = ComponentName(appContext, InterceptorService::class.java)
            val packageManager = appContext.packageManager
            // Toggling the listener component forces Android to tear down and recreate the
            // binding, which is more reliable than requestRebind alone on some OEM skins.
            packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            NotificationListenerService.requestRebind(component)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            // Re-arm the schedule + reminder alarms as part of the restart.
            appContext.sendBroadcast(Intent(appContext, ScheduleReceiver::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ReminderDiagnostics.log(appContext, "[Engine Diagnostic] Engine restarted by user")

        Handler(Looper.getMainLooper()).postDelayed({
            ReminderDiagnostics.log(
                appContext,
                "[Engine Diagnostic] After restart: listener connected=${InterceptorService.isConnected}"
            )
        }, 3000L)
    }
}
