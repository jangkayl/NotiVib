package com.example.notivib.domain.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
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

    // Fixed id for the single grouped summary notification. Kept clear of
    // ActiveAlarmService (1001) and EngineForegroundService (2).
    private const val SUMMARY_ID = 3

    // XOR mask applied to a notice's notifId when building its "acknowledge" PendingIntent so it
    // can never collide with the request code used for the row's own click PendingIntent.
    private const val ACK_REQUEST_CODE_MASK = -0x5f3759df

    private const val ACK_ALL_REQUEST_CODE = -1
    private const val REPOST_REQUEST_CODE = -2

    private const val MAX_ROWS = 5

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
     * Builds and posts (or cancels) the single grouped protected-notifications summary from the
     * current list of pending [notices]. Also cancels any legacy per-conversation ongoing
     * notifications (keyed by [ProtectedNotice.notifId]) left over from the previous per-notice
     * version, so upgrading users don't end up with duplicates.
     */
    fun refresh(context: Context, notices: List<ProtectedNotice>) {
        try {
            // Cleanup: clear any old per-conversation notifications from the previous version.
            notices.forEach { notice ->
                NotificationManagerCompat.from(context).cancel(notice.notifId)
            }

            if (notices.isEmpty()) {
                NotificationManagerCompat.from(context).cancel(SUMMARY_ID)
                return
            }

            ensureChannelCreated(context)

            val collapsedView = RemoteViews(context.packageName, R.layout.notification_protected_collapsed)
            val expandedView = RemoteViews(context.packageName, R.layout.notification_protected_expanded)

            val headerText = "Protected Notifications (${notices.size})"
            collapsedView.setTextViewText(R.id.header_text, headerText)
            expandedView.setTextViewText(R.id.header_text, headerText)

            bindRow(context, collapsedView, notices[0], rowContainerId = R.id.row1, iconId = R.id.row1_icon, textId = R.id.row1_text, ackId = R.id.row1_ack)

            val rowIds = listOf(
                RowIds(R.id.row1, R.id.row1_icon, R.id.row1_text, R.id.row1_ack),
                RowIds(R.id.row2, R.id.row2_icon, R.id.row2_text, R.id.row2_ack),
                RowIds(R.id.row3, R.id.row3_icon, R.id.row3_text, R.id.row3_ack),
                RowIds(R.id.row4, R.id.row4_icon, R.id.row4_text, R.id.row4_ack),
                RowIds(R.id.row5, R.id.row5_icon, R.id.row5_text, R.id.row5_ack)
            )
            rowIds.forEachIndexed { index, ids ->
                if (index < notices.size && index < MAX_ROWS) {
                    expandedView.setViewVisibility(ids.container, android.view.View.VISIBLE)
                    bindRow(context, expandedView, notices[index], ids.container, ids.icon, ids.text, ids.ack)
                } else {
                    expandedView.setViewVisibility(ids.container, android.view.View.GONE)
                }
            }

            val remaining = notices.size - MAX_ROWS
            if (remaining > 0) {
                expandedView.setTextViewText(R.id.more_text, "+ $remaining more")
                expandedView.setViewVisibility(R.id.more_text, android.view.View.VISIBLE)
            } else {
                expandedView.setViewVisibility(R.id.more_text, android.view.View.GONE)
            }

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
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon; a coloured PNG renders as a white block
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setWhen(notices[0].timeMillis)
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

    private data class RowIds(val container: Int, val icon: Int, val text: Int, val ack: Int)

    private fun bindRow(
        context: Context,
        views: RemoteViews,
        notice: ProtectedNotice,
        rowContainerId: Int,
        iconId: Int,
        textId: Int,
        ackId: Int
    ) {
        views.setTextViewText(textId, "${notice.title}: ${notice.text}")

        val icon: Drawable? = try {
            context.packageManager.getApplicationIcon(notice.packageName)
        } catch (e: Exception) {
            null
        }
        try {
            val bitmap = (icon ?: context.getDrawable(R.drawable.notivib_new_logo))?.toBitmap(width = 96, height = 96)
            if (bitmap != null) {
                views.setImageViewBitmap(iconId, bitmap)
            }
        } catch (e: Exception) {
            // Leave the icon view as-is if even the fallback drawable can't be rendered.
        }

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
        views.setOnClickPendingIntent(rowContainerId, contentPendingIntent)

        val ackIntent = Intent(context, ProtectedNoticeReceiver::class.java).apply {
            action = ProtectedNoticeReceiver.ACTION_ACK
            putExtra(ProtectedNoticeReceiver.EXTRA_KEY, notice.key)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            notice.notifId xor ACK_REQUEST_CODE_MASK,
            ackIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(ackId, ackPendingIntent)
    }

    suspend fun acknowledge(context: Context, key: String) {
        try {
            val repository = getRepository(context)
            val updated = repository.remove(key)
            refresh(context, updated)
            ReminderDiagnostics.log(context, "[Protect] Acknowledged: $key")
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
