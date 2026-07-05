package com.example.notivib.domain.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.framework.receiver.ScheduleReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ScheduleManager {
    fun evaluateAndSchedule(context: Context, rules: List<AlarmRule>): Boolean {
        val now = LocalDateTime.now()
        val currentMinutes = now.hour * 60 + now.minute
        val currentDay = now.dayOfWeek.value

        var isAnyRuleActive = false
        var nextEventTime: LocalDateTime? = null

        val activeRules = rules.filter { it.isActive && it.activeDays.isNotEmpty() }

        if (activeRules.isEmpty()) {
            cancelAlarm(context)
            return false
        }

        for (rule in activeRules) {
            val ruleStartsToday = rule.activeDays.contains(currentDay)
            val isFullDay = rule.startTimeMinute == 0 && rule.endTimeMinute == 1440 || rule.startTimeMinute == rule.endTimeMinute

            var ruleIsActiveRightNow = false
            
            if (ruleStartsToday) {
                if (isFullDay) {
                    ruleIsActiveRightNow = true
                } else if (rule.startTimeMinute < rule.endTimeMinute) {
                    ruleIsActiveRightNow = currentMinutes in rule.startTimeMinute..rule.endTimeMinute
                } else if (rule.startTimeMinute > rule.endTimeMinute) {
                    ruleIsActiveRightNow = currentMinutes >= rule.startTimeMinute || currentMinutes <= rule.endTimeMinute
                }
            } else if (rule.startTimeMinute > rule.endTimeMinute) {
                // Check if it started yesterday and spans into today
                val yesterday = now.minusDays(1).dayOfWeek.value
                if (rule.activeDays.contains(yesterday) && currentMinutes <= rule.endTimeMinute) {
                    ruleIsActiveRightNow = true
                }
            }

            if (ruleIsActiveRightNow) {
                isAnyRuleActive = true
            }

            // Calculate next start time and next end time to find the next event
            val nextStart = getNextTime(now, rule.startTimeMinute, rule.activeDays)
            val nextEnd = getNextTime(
                now, 
                rule.endTimeMinute, 
                if (rule.startTimeMinute > rule.endTimeMinute) 
                    rule.activeDays.map { if (it == 7) 1 else it + 1 }.toSet() // ends on the next day
                else rule.activeDays
            )

            // We only care about events in the future
            if (nextStart.isAfter(now)) {
                if (nextEventTime == null || nextStart.isBefore(nextEventTime)) {
                    nextEventTime = nextStart
                }
            }
            if (nextEnd.isAfter(now)) {
                if (nextEventTime == null || nextEnd.isBefore(nextEventTime)) {
                    nextEventTime = nextEnd
                }
            }
        }

        nextEventTime?.let {
            scheduleAlarm(context, it)
        } ?: cancelAlarm(context)

        return isAnyRuleActive
    }

    private fun getNextTime(now: LocalDateTime, minuteOfDay: Int, activeDays: Set<Int>): LocalDateTime {
        var targetTime = now.withHour(minuteOfDay / 60).withMinute(minuteOfDay % 60).withSecond(0).withNano(0)
        
        // If the time has already passed today, or today is not an active day, move to the next valid day
        if (!activeDays.contains(targetTime.dayOfWeek.value) || targetTime.isBefore(now) || targetTime.isEqual(now)) {
            var addedDays = 0
            do {
                addedDays++
                targetTime = targetTime.plusDays(1)
            } while (!activeDays.contains(targetTime.dayOfWeek.value) && addedDays < 8)
        }
        return targetTime
    }

    private fun scheduleAlarm(context: Context, time: LocalDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.d("ScheduleManager", "Scheduled next evaluation at $time")
        } catch (e: SecurityException) {
            // Missing SCHEDULE_EXACT_ALARM permission
            e.printStackTrace()
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
