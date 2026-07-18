package com.example.notivib.domain.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.domain.repository.RuleRepository
import com.example.notivib.framework.receiver.ScheduleReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

object ScheduleReminderManager {
    fun scheduleForRule(context: Context, rule: AlarmRule, isRescheduling: Boolean = false) {
        if (!rule.remindSchedule || !rule.isActive || rule.activeDays.isEmpty()) {
            cancelAlarm(context, rule.id)
            return
        }

        val now = LocalDateTime.now()
        var nextStart: LocalDateTime? = null
        var nextEnd: LocalDateTime? = null

        // Check the next 8 days to find the next start and end times
        for (offset in 0..7) {
            val targetDay = now.plusDays(offset.toLong())
            val dayOfWeek = targetDay.dayOfWeek.value
            
            if (rule.activeDays.contains(dayOfWeek)) {
                val startMin = getStartMinute(rule, dayOfWeek)
                val start = targetDay.withHour(startMin / 60).withMinute(startMin % 60).withSecond(0).withNano(0)
                
                val endMin = getEndMinute(rule, dayOfWeek)
                var end = targetDay.withHour(endMin / 60).withMinute(endMin % 60).withSecond(0).withNano(0)
                
                if (startMin > endMin) {
                    end = end.plusDays(1)
                }

                var nowMinute = now.withSecond(0).withNano(0)
                if (isRescheduling) {
                    nowMinute = nowMinute.plusMinutes(1)
                }
                
                if (!start.isBefore(nowMinute) && (nextStart == null || start.isBefore(nextStart))) {
                    nextStart = start
                }
                if (!end.isBefore(nowMinute) && (nextEnd == null || end.isBefore(nextEnd))) {
                    nextEnd = end
                }
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Next Start Alarm
        if (nextStart != null) {
            val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
                putExtra("APP_NAME", rule.targetPackage.ifEmpty { "Any App" })
                putExtra("RULE_ID", rule.id)
                putExtra("IS_START", true)
            }
            // Use positive hash code for request code to avoid collisions, but keep start and end separate
            val requestCode = (rule.id.hashCode() * 31) + 1
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val triggerAtMillis = nextStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.d("ScheduleReminder", "Scheduled start reminder for ${rule.id} at $nextStart")
            } catch (e: SecurityException) { e.printStackTrace() }
        }

        // Next End Alarm
        if (nextEnd != null) {
            val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
                putExtra("APP_NAME", rule.targetPackage.ifEmpty { "Any App" })
                putExtra("RULE_ID", rule.id)
                putExtra("IS_START", false)
            }
            val requestCode = (rule.id.hashCode() * 31) + 2
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val triggerAtMillis = nextEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.d("ScheduleReminder", "Scheduled end reminder for ${rule.id} at $nextEnd")
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    fun cancelAlarm(context: Context, ruleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel start
        val startIntent = Intent(context, ScheduleReminderReceiver::class.java)
        val startRequestCode = (ruleId.hashCode() * 31) + 1
        val startPendingIntent = PendingIntent.getBroadcast(
            context, startRequestCode, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.cancel(startPendingIntent)

        // Cancel end
        val endIntent = Intent(context, ScheduleReminderReceiver::class.java)
        val endRequestCode = (ruleId.hashCode() * 31) + 2
        val endPendingIntent = PendingIntent.getBroadcast(
            context, endRequestCode, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.cancel(endPendingIntent)
    }

    fun scheduleFollowUp(context: Context, appName: String, ruleId: String, isStart: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("RULE_ID", ruleId)
            putExtra("IS_START", isStart)
            putExtra("IS_FOLLOWUP", true)
        }
        val requestCode = (ruleId.hashCode() * 31) + (if (isStart) 3 else 4)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val triggerAtMillis = System.currentTimeMillis() + 60_000L // 1 minute delay
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.d("ScheduleReminder", "Scheduled follow-up reminder for $ruleId in 1 minute")
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun getStartMinute(rule: AlarmRule, day: Int): Int {
        return if (rule.hasCustomTimeWindows && rule.customTimeWindows.containsKey(day)) {
            rule.customTimeWindows[day]!!.startTimeMinute
        } else {
            rule.startTimeMinute
        }
    }

    private fun getEndMinute(rule: AlarmRule, day: Int): Int {
        return if (rule.hasCustomTimeWindows && rule.customTimeWindows.containsKey(day)) {
            rule.customTimeWindows[day]!!.endTimeMinute
        } else {
            rule.endTimeMinute
        }
    }
}
