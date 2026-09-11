package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.framework.utils.EngineState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationLogRepository: NotificationLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            return
        }

        if (!EngineState.isGloballyEnabled(context)) return

        // Re-establish schedules + foreground service the same way ScheduleReceiver does.
        val scheduleIntent = Intent(context, ScheduleReceiver::class.java)
        context.sendBroadcast(scheduleIntent)

        notificationLogRepository.addSystemLog("[Engine Diagnostic] Device rebooted — engine restored")
    }
}
