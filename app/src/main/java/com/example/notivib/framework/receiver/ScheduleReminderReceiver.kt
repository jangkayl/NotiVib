package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notivib.framework.service.ActiveAlarmService
import com.example.notivib.domain.manager.ScheduleReminderManager
import com.example.notivib.domain.repository.RuleRepository
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
        val isStart = intent.getBooleanExtra("IS_START", true)
        val isFollowUp = intent.getBooleanExtra("IS_FOLLOWUP", false)

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
            putExtra(ActiveAlarmService.EXTRA_ALARM_MODE, mode)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        // Reschedule the next alarm for this rule
        if (ruleId != null && !isFollowUp) {
            CoroutineScope(Dispatchers.IO).launch {
                val rules = repository.getRules().firstOrNull() ?: emptyList()
                val rule = rules.find { it.id == ruleId }
                if (rule != null) {
                    ScheduleReminderManager.scheduleForRule(context, rule, isRescheduling = true)
                }
            }
        }
    }
}
