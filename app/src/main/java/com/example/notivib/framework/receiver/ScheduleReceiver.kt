package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notivib.domain.manager.ScheduleManager
import com.example.notivib.domain.manager.ScheduleReminderManager
import com.example.notivib.domain.repository.RuleRepository
import com.example.notivib.framework.service.EngineForegroundService
import com.example.notivib.framework.utils.EngineState
import com.example.notivib.framework.utils.ReminderDiagnostics
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
        // goAsync() + a finally-finish keeps the process alive long enough for this coroutine to
        // complete even if the system is about to kill it; any Throwable is caught so it can
        // never crash the process.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rules = repository.getRules().firstOrNull() ?: emptyList()
                val isActive = ScheduleManager.evaluateAndSchedule(context, rules)

                EngineState.setScheduleActive(context, isActive)

                try {
                    if (EngineState.shouldIntercept(context) && EngineState.isShowForegroundNotification(context)) {
                        // Start Foreground Service
                        val serviceIntent = Intent(context, EngineForegroundService::class.java)
                        context.startForegroundService(serviceIntent)
                    } else {
                        // Stop Foreground Service
                        val serviceIntent = Intent(context, EngineForegroundService::class.java)
                        context.stopService(serviceIntent)
                    }
                } catch (e: Exception) {
                    // e.g. ForegroundServiceStartNotAllowedException from a non-exempt
                    // background start (MY_PACKAGE_REPLACED, timezone change, etc.)
                    e.printStackTrace()
                }

                ScheduleReminderManager.rescheduleAll(context, rules, forceLog = intent.getBooleanExtra(EXTRA_VERBOSE, false))
            } catch (t: Throwable) {
                ReminderDiagnostics.log(context, "[Reminder] FAILED to re-arm reminders: ${t.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /** Set by user-initiated re-arms (Restart Engine) so every reminder's re-arm is logged. */
        const val EXTRA_VERBOSE = "extra_verbose"
    }
}
