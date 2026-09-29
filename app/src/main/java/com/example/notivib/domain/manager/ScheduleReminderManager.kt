package com.example.notivib.domain.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.framework.receiver.ScheduleReminderReceiver
import com.example.notivib.framework.utils.ReminderDiagnostics
import com.example.notivib.framework.utils.ReminderStateStore
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ScheduleReminderManager {

    private val LOG_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun scheduleForRule(context: Context, rule: AlarmRule, isRescheduling: Boolean = false, forceLog: Boolean = false) {
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
                // Built via atStartOfDay().plusMinutes(...) rather than withHour/withMinute so
                // a full-day rule's end minute of 1440 doesn't throw DateTimeException
                // (withHour(24) is invalid).
                val start = targetDay.toLocalDate().atStartOfDay().plusMinutes(startMin.toLong())

                val endMin = getEndMinute(rule, dayOfWeek)
                var end = targetDay.toLocalDate().atStartOfDay().plusMinutes(endMin.toLong())

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

        if (nextStart != null) {
            arm(context, rule, isStart = true, triggerAt = nextStart, forceLog = forceLog)
        }
        if (nextEnd != null) {
            arm(context, rule, isStart = false, triggerAt = nextEnd, forceLog = forceLog)
        }
    }

    private fun arm(context: Context, rule: AlarmRule, isStart: Boolean, triggerAt: LocalDateTime, forceLog: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val label = if (isStart) "START" else "END"
        val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
            putExtra("APP_NAME", rule.targetPackage.ifEmpty { "Any App" })
            putExtra("RULE_ID", rule.id)
            putExtra("RULE_NAME", rule.ruleName)
            putExtra("IS_START", isStart)
        }
        val requestCode = (rule.id.hashCode() * 31) + (if (isStart) 1 else 2)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (!canScheduleExact(alarmManager)) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                ReminderDiagnostics.log(
                    context,
                    "[Reminder] FAILED $label \"${rule.ruleName}\": exact alarm not permitted — using inexact alarm"
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d("ScheduleReminder", "Scheduled $label reminder for ${rule.id} at $triggerAt")

            // Only log "Armed" when the target trigger time actually changed, so the frequent
            // rescheduleAll sweeps (every schedule evaluation + every fired reminder) don't
            // flood the diagnostics log with an identical line every time. [forceLog] overrides
            // that for user-initiated sweeps (Restart Engine) so they visibly confirm the re-arm.
            val previous = ReminderStateStore.getExpected(context, rule.id, isStart)
            ReminderStateStore.setExpected(context, rule.id, isStart, triggerAtMillis, rule.ruleName)
            if (forceLog || previous == null || previous.millis != triggerAtMillis) {
                ReminderDiagnostics.log(
                    context,
                    "[Reminder] Armed $label \"${rule.ruleName}\" for ${triggerAt.format(LOG_TIME_FORMAT)}"
                )
            }
        } catch (e: SecurityException) {
            ReminderDiagnostics.log(context, "[Reminder] FAILED $label \"${rule.ruleName}\": ${e.message}")
        }
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
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

        ReminderStateStore.clearAllForRule(context, ruleId)
    }

    fun scheduleFollowUp(context: Context, appName: String, ruleId: String, isStart: Boolean, ruleName: String = "") {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val label = if (isStart) "START" else "END"
        val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("RULE_ID", ruleId)
            putExtra("RULE_NAME", ruleName)
            putExtra("IS_START", isStart)
            putExtra("IS_FOLLOWUP", true)
        }
        val requestCode = (ruleId.hashCode() * 31) + (if (isStart) 3 else 4)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val triggerAtMillis = System.currentTimeMillis() + 60_000L // 1 minute delay
        try {
            if (!canScheduleExact(alarmManager)) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                ReminderDiagnostics.log(
                    context,
                    "[Reminder] FAILED $label \"$ruleName\": exact alarm not permitted — using inexact alarm"
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d("ScheduleReminder", "Scheduled follow-up reminder for $ruleId in 1 minute")
        } catch (e: SecurityException) {
            ReminderDiagnostics.log(context, "[Reminder] FAILED $label \"$ruleName\": ${e.message}")
        }
    }

    /**
     * Re-arms reminders for every rule, called on every schedule evaluation (boot, app update,
     * timezone/time change, MainActivity resume, and after every ScheduleReceiver sweep) so the
     * reminder chain can never permanently die from a single missed link.
     *
     * When [logMissed] is true, first checks for reminders whose expected trigger time has
     * already passed (with a grace period) without having been cleared by a fire — meaning the
     * alarm silently failed to deliver — and logs them as MISSED before re-arming.
     */
    fun rescheduleAll(context: Context, rules: List<AlarmRule>, logMissed: Boolean = true, forceLog: Boolean = false) {
        if (logMissed) {
            val graceMillis = 2 * 60_000L
            val expired = ReminderStateStore.allExpired(context, System.currentTimeMillis(), graceMillis)
            for (entry in expired) {
                val rule = rules.find { it.id == entry.ruleId }
                if (rule != null && rule.isActive && rule.remindSchedule) {
                    val label = if (entry.isStart) "START" else "END"
                    val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.millis), ZoneId.systemDefault())
                    ReminderDiagnostics.log(
                        context,
                        "[Reminder] MISSED $label \"${entry.ruleName}\" (expected ${time.format(LOG_TIME_FORMAT)})"
                    )
                }
                // Entries for unknown/inactive rules are just cleared silently.
                ReminderStateStore.clearExpected(context, entry.ruleId, entry.isStart)
            }
        }

        for (rule in rules) {
            scheduleForRule(context, rule, isRescheduling = true, forceLog = forceLog)
        }

        if (forceLog) {
            val count = rules.count { it.remindSchedule && it.isActive && it.activeDays.isNotEmpty() }
            ReminderDiagnostics.log(context, "[Reminder] Re-arm sweep complete: $count rule(s) with reminders")
        }
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
