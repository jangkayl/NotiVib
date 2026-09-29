package com.example.notivib.domain.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notivib.MainActivity
import com.example.notivib.R
import com.example.notivib.domain.repository.ProtectedNotice
import com.example.notivib.domain.repository.ProtectedNoticeRepository
import com.example.notivib.framework.receiver.ProtectedNoticeReceiver
import com.example.notivib.framework.utils.ReminderDiagnostics
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object ProtectedNoticeManager {

    private const val CHANNEL_ID = "protected_notice_channel"

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProtectedNoticeRepositoryEntryPoint {
        fun protectedNoticeRepository(): ProtectedNoticeRepository
    }

    private fun getRepository(context: Context): ProtectedNoticeRepository {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            ProtectedNoticeRepositoryEntryPoint::class.java
        ).protectedNoticeRepository()
    }

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Protected Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    vibrationPattern = longArrayOf(0)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun post(context: Context, notice: ProtectedNotice) {
        ensureChannelCreated(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(notice.packageName)
        val contentIntent = launchIntent ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notice.notifId,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ackIntent = Intent(context, ProtectedNoticeReceiver::class.java).apply {
            action = ProtectedNoticeReceiver.ACTION_ACK
            putExtra(ProtectedNoticeReceiver.EXTRA_KEY, notice.key)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            notice.notifId,
            ackIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val repostIntent = Intent(context, ProtectedNoticeReceiver::class.java).apply {
            action = ProtectedNoticeReceiver.ACTION_REPOST
            putExtra(ProtectedNoticeReceiver.EXTRA_KEY, notice.key)
        }
        val repostPendingIntent = PendingIntent.getBroadcast(
            context,
            notice.notifId,
            repostIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notivib_new_logo)
            .setContentTitle("${notice.appName}: ${notice.title}")
            .setContentText(notice.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.text))
            .setWhen(notice.timeMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(repostPendingIntent)
            .addAction(0, "Acknowledge", ackPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notice.notifId, notification)
        } catch (e: SecurityException) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }

    fun acknowledge(context: Context, key: String) {
        try {
            val notifId = key.hashCode()
            NotificationManagerCompat.from(context).cancel(notifId)
            val repository = getRepository(context)
            repository.remove(key)
            ReminderDiagnostics.log(context, "[Protect] Acknowledged: $key")
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }

    fun restoreAll(context: Context, notices: List<ProtectedNotice>) {
        try {
            notices.forEach { notice ->
                post(context, notice)
            }
            ReminderDiagnostics.log(context, "[Protect] Restored ${notices.size} after reboot")
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }
}
