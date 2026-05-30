package com.example.notivib.framework.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.usecase.EvaluateNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InterceptorService : NotificationListenerService() {

    @Inject
    lateinit var evaluateNotificationUseCase: EvaluateNotificationUseCase

    @Inject
    lateinit var notificationLogRepository: NotificationLogRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationLogRepository.addSystemLog("Service Process Created by Android OS")
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationLogRepository.addSystemLog("Service Process Destroyed by Android OS")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        notificationLogRepository.addSystemLog("Interceptor Successfully Connected to Notification Stream")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        notificationLogRepository.addSystemLog("Interceptor Disconnected from Notification Stream")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            val fullText = buildString {
                if (text.isNotEmpty()) append(text)
                if (subText.isNotEmpty()) append("\nSub: $subText")
                if (bigText.isNotEmpty() && bigText != text) append("\nExtended: $bigText")
            }

            val pm = packageManager
            val appName = try {
                val ai = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                ""
            }

            scope.launch {
                val matchedRule = evaluateNotificationUseCase.evaluate(packageName, appName, title, fullText)
                
                notificationLogRepository.addLog(
                    appName = appName.ifEmpty { "Unknown" },
                    packageName = packageName,
                    title = title.ifEmpty { "No Title" },
                    text = fullText.ifEmpty { "No Content" },
                    matchedRule = matchedRule?.let { "Rule: ${it.targetPackage.ifEmpty{"Any App"}} / ${it.keyword.ifEmpty{"Any Keyword"}}" }
                )

                if (matchedRule != null) {
                    if (!ActiveAlarmService.isAlarmRunning) {
                        triggerAlarm(appName.ifEmpty { packageName }, matchedRule.keyword.ifEmpty { "Any" }, matchedRule.vibrationOnly)
                    }
                }
            }
        }
    }

    private fun triggerAlarm(appName: String, keyword: String, vibrationOnly: Boolean) {
        val intent = Intent(this, ActiveAlarmService::class.java).apply {
            action = ActiveAlarmService.ACTION_START
            putExtra("APP_NAME", appName)
            putExtra("KEYWORD", keyword)
            putExtra("VIBRATION_ONLY", vibrationOnly)
        }
        startForegroundService(intent)
    }
}
