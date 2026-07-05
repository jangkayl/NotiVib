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
            val todayStart = getStartMinute(rule, currentDay)
            val todayEnd = getEndMinute(rule, currentDay)
            val isFullDay = todayStart == 0 && todayEnd == 1440 || todayStart == todayEnd

            var ruleIsActiveRightNow = false
            
            if (ruleStartsToday) {
                if (isFullDay) {
                    ruleIsActiveRightNow = true
                } else if (todayStart < todayEnd) {
                    ruleIsActiveRightNow = currentMinutes in todayStart..todayEnd
                } else if (todayStart > todayEnd) {
                    ruleIsActiveRightNow = currentMinutes >= todayStart || currentMinutes <= todayEnd
                }
            } else {
                // Check if it started yesterday and spans into today
                val yesterday = now.minusDays(1).dayOfWeek.value
                val yesterdayStart = getStartMinute(rule, yesterday)
                val yesterdayEnd = getEndMinute(rule, yesterday)
                if (rule.activeDays.contains(yesterday) && yesterdayStart > yesterdayEnd && currentMinutes <= yesterdayEnd) {
                    ruleIsActiveRightNow = true
                }
            }

            if (ruleIsActiveRightNow) {
                isAnyRuleActive = true
            }

            // Calculate next start time and next end time to find the next event
            // We must check upcoming days because each day can have a different time window
            for (offset in 0..7) {
                val targetDay = now.plusDays(offset.toLong())
                val dayOfWeek = targetDay.dayOfWeek.value
                
                if (rule.activeDays.contains(dayOfWeek)) {
                    val startMin = getStartMinute(rule, dayOfWeek)
                    val nextStart = targetDay.withHour(startMin / 60).withMinute(startMin % 60).withSecond(0).withNano(0)
                    
                    val endMin = getEndMinute(rule, dayOfWeek)
                    var nextEnd = targetDay.withHour(endMin / 60).withMinute(endMin % 60).withSecond(0).withNano(0)
                    if (startMin > endMin) {
                        nextEnd = nextEnd.plusDays(1)
                    }

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
