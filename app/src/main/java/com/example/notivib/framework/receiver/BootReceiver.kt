package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notivib.domain.manager.ProtectedNoticeManager
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.repository.ProtectedNoticeRepository
import com.example.notivib.framework.utils.EngineState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationLogRepository: NotificationLogRepository

    @Inject
    lateinit var protectedNoticeRepository: ProtectedNoticeRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, "android.intent.action.LOCKED_BOOT_COMPLETED" -> "boot"
            Intent.ACTION_MY_PACKAGE_REPLACED -> "app update"
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> "time change"
            else -> return
        }

        // Reminders must be re-armed even if the engine is off — ScheduleReceiver already stops
        // the foreground service in that case, so re-arming here doesn't turn the engine back on.
        val scheduleIntent = Intent(context, ScheduleReceiver::class.java)
        context.sendBroadcast(scheduleIntent)

        if (reason == "boot" || reason == "app update") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val notices = protectedNoticeRepository.getAll()
                    ProtectedNoticeManager.restoreAll(context, notices)
                } catch (t: Throwable) {
                    notificationLogRepository.addSystemLog("[Protect] FAILED restore on $reason: ${t.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }

        if (reason == "boot" && EngineState.isGloballyEnabled(context)) {
            notificationLogRepository.addSystemLog("[Engine Diagnostic] Device rebooted — engine restored")
        }
        notificationLogRepository.addSystemLog("[Engine Diagnostic] Reminders re-armed after $reason")
    }
}
