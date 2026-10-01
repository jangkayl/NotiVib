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

    // Fixed id for the single summary notification. Kept clear of
    // ActiveAlarmService (1001) and EngineForegroundService (2).
    private const val SUMMARY_ID = 3

    private const val ACK_ALL_REQUEST_CODE = -1
    private const val REPOST_REQUEST_CODE = -2

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
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Builds and posts (or cancels) the single protected-notifications status notification from the
     * current list of pending [notices]. Tapping it opens the in-app Protected Messages screen.
     * Also cancels any legacy per-conversation ongoing notifications left over from earlier versions.
     */
    fun refresh(context: Context, notices: List<ProtectedNotice>) {
        try {
            // Cleanup: clear any old per-conversation notifications from earlier versions.
            notices.forEach { notice ->
                NotificationManagerCompat.from(context).cancel(notice.notifId)
            }

            if (notices.isEmpty()) {
                NotificationManagerCompat.from(context).cancel(SUMMARY_ID)
                return
            }

            ensureChannelCreated(context)

            val count = notices.size
            val latest = notices[0]
            val title = "Protected Notifications ($count)"
            val latestSnippet = if (latest.title.isNotEmpty() && latest.title != "No Title") {
                latest.title
            } else {
                latest.text
            }
            val contentText = if (count == 1) {
                "${latest.appName}: $latestSnippet"
            } else {
                "$count pending • Latest from ${latest.appName}: $latestSnippet"
            }

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)

            val maxLines = 5
            val displayNotices = notices.take(maxLines)
            displayNotices.forEach { notice ->
                val appPrefix = notice.appName
                val rawTitle = if (notice.title.isNotEmpty() && notice.title != "No Title") {
                    notice.title.replace("\n", " ").trim()
                } else ""
                val rawBody = notice.text.replace("\n", " ").trim()

                val cleanBody = if (rawTitle.isNotEmpty() && rawBody.startsWith(rawTitle, ignoreCase = true)) {
                    rawBody.substring(rawTitle.length).trim().removePrefix(":").trim().removePrefix("-").trim()
                } else {
                    rawBody
                }

                val lineContent = when {
                    rawTitle.isNotEmpty() && cleanBody.isNotEmpty() && !rawTitle.equals(cleanBody, ignoreCase = true) -> {
                        "$rawTitle · $cleanBody"
                    }
                    rawTitle.isNotEmpty() -> rawTitle
                    else -> cleanBody
                }

                val html = "<b>$appPrefix:</b> $lineContent"
                val styledLine = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
                } else {
                    @Suppress("DEPRECATION")
                    android.text.Html.fromHtml(html)
                }
                inboxStyle.addLine(styledLine)
            }

            val remaining = count - displayNotices.size
            if (remaining > 0) {
                inboxStyle.setSummaryText("+$remaining more • Tap to view all")
            } else {
                inboxStyle.setSummaryText("$count protected")
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_PROTECTED_NOTICES)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val ackAllIntent = Intent(context, ProtectedNoticeReceiver::class.java).apply {
                action = ProtectedNoticeReceiver.ACTION_ACK_ALL
            }
            val ackAllPendingIntent = PendingIntent.getBroadcast(
                context,
                ACK_ALL_REQUEST_CODE,
                ackAllIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val repostIntent = Intent(context, ProtectedNoticeReceiver::class.java).apply {
                action = ProtectedNoticeReceiver.ACTION_REPOST
            }
            val repostPendingIntent = PendingIntent.getBroadcast(
                context,
                REPOST_REQUEST_CODE,
                repostIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(inboxStyle)
                .setContentIntent(contentPendingIntent)
                .setWhen(latest.timeMillis)
                .setShowWhen(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDeleteIntent(repostPendingIntent)
                .addAction(0, "Acknowledge all", ackAllPendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(SUMMARY_ID, notification)
        } catch (e: Exception) {
            // Includes SecurityException when POST_NOTIFICATIONS is denied.
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }

    suspend fun acknowledgeSingle(context: Context, key: String) {
        try {
            val repository = getRepository(context)
            val updated = repository.remove(key)
            refresh(context, updated)
            ReminderDiagnostics.log(context, "[Protect] Acknowledged notice")
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }

    suspend fun acknowledgeAll(context: Context) {
        try {
            val repository = getRepository(context)
            repository.clear()
            refresh(context, emptyList())
            ReminderDiagnostics.log(context, "[Protect] Acknowledged all")
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }

    fun restoreAll(context: Context, notices: List<ProtectedNotice>) {
        try {
            refresh(context, notices)
            if (notices.isNotEmpty()) {
                ReminderDiagnostics.log(context, "[Protect] Restored ${notices.size} after reboot")
            }
        } catch (e: Exception) {
            ReminderDiagnostics.log(context, "[Protect] FAILED: ${e.message}")
        }
    }
}
