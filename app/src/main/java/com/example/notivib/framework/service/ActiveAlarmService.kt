package com.example.notivib.framework.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class ActiveAlarmService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_ALARM_MODE = "ALARM_MODE"
        const val MODE_INTERCEPT = 0
        const val MODE_SCHEDULE_START = 1
        const val MODE_SCHEDULE_END = 2
        const val MODE_SCHEDULE_START_FOLLOWUP = 3
        const val MODE_SCHEDULE_END_FOLLOWUP = 4
        
        var isAlarmRunning = false
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isVibrationOnly: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isVibrationOnly = intent?.getBooleanExtra("VIBRATION_ONLY", false) ?: false
        when (intent?.action) {
            ACTION_START -> {
                if (!isAlarmRunning) {
                    isAlarmRunning = true
                    val notification = buildNotification(intent)
                    startForeground(NOTIFICATION_ID, notification)
                    startAlarm()
                    try {
                        val appName = intent?.getStringExtra("APP_NAME") ?: "An App"
                        val keyword = intent?.getStringExtra("KEYWORD") ?: "a keyword"
                        val ruleId = intent?.getStringExtra("RULE_ID")
                        val mode = intent?.getIntExtra(EXTRA_ALARM_MODE, MODE_INTERCEPT) ?: MODE_INTERCEPT

                        val fullScreenIntent = Intent(this, com.example.notivib.presentation.alarm.AlarmActivity::class.java).apply {
                            putExtra("APP_NAME", appName)
                            putExtra("KEYWORD", keyword)
                            putExtra("RULE_ID", ruleId)
                            putExtra(EXTRA_ALARM_MODE, mode)
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(fullScreenIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            ACTION_STOP -> {
                isAlarmRunning = false
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

                val ruleId = intent?.getStringExtra("RULE_ID")
                val mode = intent?.getIntExtra(EXTRA_ALARM_MODE, MODE_INTERCEPT) ?: MODE_INTERCEPT
                val appName = intent?.getStringExtra("APP_NAME") ?: "An App"

                if (mode == MODE_SCHEDULE_START || mode == MODE_SCHEDULE_END) {
                    if (!ruleId.isNullOrEmpty()) {
                        com.example.notivib.domain.manager.ScheduleReminderManager.scheduleFollowUp(
                            context = this,
                            appName = appName,
                            ruleId = ruleId,
                            isStart = (mode == MODE_SCHEDULE_START)
                        )
                    }
                }

                val closeIntent = Intent("com.example.notivib.ACTION_CLOSE_ALARM_SCREEN")
                closeIntent.setPackage(packageName)
                sendBroadcast(closeIntent)
            }
        }
        return START_STICKY
    }

    private fun buildNotification(intent: Intent?): Notification {
        val appName = intent?.getStringExtra("APP_NAME") ?: "An App"
        val keyword = intent?.getStringExtra("KEYWORD") ?: "a keyword"
        val ruleId = intent?.getStringExtra("RULE_ID")
        val mode = intent?.getIntExtra(EXTRA_ALARM_MODE, MODE_INTERCEPT) ?: MODE_INTERCEPT

        val stopIntent = Intent(this, ActiveAlarmService::class.java).apply {
            action = ACTION_STOP
            putExtra("APP_NAME", appName)
            putExtra("RULE_ID", ruleId)
            putExtra(EXTRA_ALARM_MODE, mode)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, System.currentTimeMillis().toInt(), stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val fullScreenIntent = Intent(this, com.example.notivib.presentation.alarm.AlarmActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("KEYWORD", keyword)
            putExtra("RULE_ID", ruleId)
            putExtra(EXTRA_ALARM_MODE, mode)
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = System.currentTimeMillis().toInt()
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, requestCode, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (mode) {
            MODE_SCHEDULE_START -> "Schedule Started!"
            MODE_SCHEDULE_END -> "Schedule Ended!"
            MODE_SCHEDULE_START_FOLLOWUP -> "Follow-up: Schedule Started"
            MODE_SCHEDULE_END_FOLLOWUP -> "Follow-up: Schedule Ended"
            else -> "NotiVib Alarm Active!"
        }
        val text = when (mode) {
            MODE_SCHEDULE_START, MODE_SCHEDULE_START_FOLLOWUP -> "$appName interception is now active."
            MODE_SCHEDULE_END, MODE_SCHEDULE_END_FOLLOWUP -> "$appName interception has ended."
            else -> "Matched: $appName - $keyword"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            
        if (mode != MODE_SCHEDULE_START_FOLLOWUP && mode != MODE_SCHEDULE_END_FOLLOWUP) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "ACKNOWLEDGE", stopPendingIntent)
        }
            
        return builder.build()
    }

    private fun startAlarm() {
        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        ringtone = RingtoneManager.getRingtone(this, defaultAlarmUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ringtone?.audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }
        if (!isVibrationOnly) {
            ringtone?.play()
        }


        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                    .build()
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used for continuous alarm sounds"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
