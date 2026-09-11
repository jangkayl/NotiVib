package com.example.notivib.framework.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat
import com.example.notivib.MainActivity
import com.example.notivib.domain.repository.NotificationLogRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EngineForegroundService : Service() {

    @Inject
    lateinit var notificationLogRepository: NotificationLogRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_INTERRUPTED -> {
                val reason = intent.getStringExtra(EXTRA_REASON)
                    ?: "Notification monitoring was interrupted. Tap restart."
                showNotification(buildInterruptedNotification(reason))
            }
            ACTION_RESTART -> {
                handleRestart()
            }
            else -> {
                showNotification(buildHealthyNotification())
            }
        }

        return START_STICKY
    }

    private fun handleRestart() {
        try {
            NotificationListenerService.requestRebind(
                ComponentName(this, InterceptorService::class.java)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        showNotification(buildHealthyNotification())
        notificationLogRepository.addSystemLog("[Engine Diagnostic] Engine restarted by user")
    }

    private fun showNotification(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildHealthyNotification(): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NotiVib Active")
            .setContentText("Monitoring notifications based on your rules.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun buildInterruptedNotification(reason: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val restartIntent = Intent(this, EngineForegroundService::class.java).apply {
            action = ACTION_RESTART
        }
        val restartPendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Engine Interrupted")
            .setContentText(reason)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Fallback icon
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Restart Engine", restartPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Engine Status Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "EngineStatusChannel"
        const val NOTIFICATION_ID = 2 // 1 is probably used for ActiveAlarmService

        const val ACTION_SHOW_INTERRUPTED = "com.example.notivib.action.SHOW_INTERRUPTED"
        const val ACTION_RESTART = "com.example.notivib.action.RESTART_ENGINE"
        const val EXTRA_REASON = "extra_reason"

        /**
         * Shows the "Engine Interrupted" banner with a Restart action, but only if the user
         * has foreground notifications enabled. Safe to call from any context.
         */
        fun showInterruptedBanner(context: android.content.Context, reason: String? = null) {
            if (!com.example.notivib.framework.utils.EngineState.isShowForegroundNotification(context)) return
            try {
                val intent = Intent(context, EngineForegroundService::class.java).apply {
                    action = ACTION_SHOW_INTERRUPTED
                    reason?.let { putExtra(EXTRA_REASON, it) }
                }
                context.startForegroundService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
