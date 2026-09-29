package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notivib.framework.service.ActiveAlarmService
import com.example.notivib.domain.manager.ScheduleReminderManager
import com.example.notivib.domain.repository.RuleRepository
import com.example.notivib.framework.utils.ReminderDiagnostics
import com.example.notivib.framework.utils.ReminderStateStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: RuleRepository
    override fun onReceive(context: Context, intent: Intent) {
        val appName = intent.getStringExtra("APP_NAME") ?: "An App"
        val ruleId = intent.getStringExtra("RULE_ID")
        val ruleName = intent.getStringExtra("RULE_NAME") ?: ""
        val isStart = intent.getBooleanExtra("IS_START", true)
        val isFollowUp = intent.getBooleanExtra("IS_FOLLOWUP", false)
        val label = if (isStart) "START" else "END"

        val mode = if (isFollowUp) {
            if (isStart) ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP else ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP
        } else {
            if (isStart) ActiveAlarmService.MODE_SCHEDULE_START else ActiveAlarmService.MODE_SCHEDULE_END
        }

        val serviceIntent = Intent(context, ActiveAlarmService::class.java).apply {
            action = ActiveAlarmService.ACTION_START
            putExtra("APP_NAME", appName)
            putExtra("KEYWORD", "Schedule")
            putExtra("RULE_ID", ruleId)
            putExtra("RULE_NAME", ruleName)
            putExtra(ActiveAlarmService.EXTRA_ALARM_MODE, mode)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            val verb = if (isFollowUp) "Follow-up fired" else "Fired"
            ReminderDiagnostics.log(context, "[Reminder] $verb $label \"$ruleName\" -> alarm started")
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Reminder] FAILED $label \"$ruleName\": ${e.message}")
        }

        // goAsync() + a finally-finish keeps the process alive long enough for the reschedule
        // coroutine below to complete even if the system is about to kill it; any Throwable is
        // caught so it can never crash the process.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (ruleId != null && !isFollowUp) {
                    // Clear the expected-fire marker for the key that just fired, before
                    // re-arming the next occurrence.
                    ReminderStateStore.clearExpected(context, ruleId, isStart)

                    val rules = repository.getRules().firstOrNull() ?: emptyList()
                    val rule = rules.find { it.id == ruleId }
                    if (rule != null) {
                        try {
                            ScheduleReminderManager.scheduleForRule(context, rule, isRescheduling = true)
                        } catch (e: Exception) {
                            ReminderDiagnostics.log(context, "[Reminder] FAILED $label \"$ruleName\": ${e.message}")
                        }
                    }
                    // If the rule was deleted/inactive, simply don't re-arm.
                }
            } catch (t: Throwable) {
                ReminderDiagnostics.log(context, "[Reminder] FAILED $label \"$ruleName\": ${t.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
