package com.example.notivib.framework.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notivib.domain.repository.NotificationLogRepository
import com.example.notivib.domain.usecase.EvaluateNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InterceptorService : NotificationListenerService() {

    companion object {
        var isConnected = false
    }

    @Inject
    lateinit var evaluateNotificationUseCase: EvaluateNotificationUseCase

    @Inject
    lateinit var notificationLogRepository: NotificationLogRepository

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        notificationLogRepository.addSystemLog("[Engine Error] Unhandled: ${throwable.message}")
        EngineForegroundService.showInterruptedBanner(
            this,
            "Notification monitoring hit an unexpected error. Tap restart."
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        notificationLogRepository.addConnectionLog("[Engine Diagnostic] Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        notificationLogRepository.addConnectionLog("[Engine Diagnostic] Listener disconnected — attempting automatic rebind")
        try {
            requestRebind(ComponentName(this, InterceptorService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName ?: return
            if (packageName == this.packageName) return // Ignore self

            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val fullText = "$text $bigText $subText".trim()

            var appName = ""
            try {
                val pm = packageManager
                val ai = pm.getApplicationInfo(packageName, 0)
                appName = pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                appName = packageName
            }

            scope.launch {
                try {
                    val evaluationResult = evaluateNotificationUseCase.evaluate(packageName, appName, title, fullText)

                    val matchedRule = when (evaluationResult) {
                        is com.example.notivib.domain.usecase.EvaluationResult.TriggerAlarm -> evaluationResult.rule
                        is com.example.notivib.domain.usecase.EvaluationResult.Mute -> evaluationResult.rule
                        is com.example.notivib.domain.usecase.EvaluationResult.Ignore -> null
                    }

                    val trackedApps = com.example.notivib.framework.utils.EngineState.getTrackedApps(this@InterceptorService)

                    if (matchedRule != null || trackedApps.contains("ALL_APPS") || trackedApps.contains(packageName)) {
                        notificationLogRepository.addLog(
                            appName = appName.ifEmpty { "Unknown" },
                            packageName = packageName,
                            title = title.ifEmpty { "No Title" },
                            text = fullText.ifEmpty { "No Content" },
                            matchedRule = matchedRule?.let { rule ->
                                val kwDisplay = com.example.notivib.domain.model.parseKeywords(rule.keyword).joinToString(", ")
                                "Rule: ${rule.targetPackage.ifEmpty{"Any App"}} / ${kwDisplay.ifEmpty{"Any Keyword"}}"
                            }
                        )
                    }

                    when (evaluationResult) {
                        is com.example.notivib.domain.usecase.EvaluationResult.TriggerAlarm -> {
                            if (!ActiveAlarmService.isAlarmRunning) {
                                val matchedKwDisplay = if (evaluationResult.matchedKeywords.isNotEmpty()) {
                                    evaluationResult.matchedKeywords.joinToString(", ")
                                } else {
                                    val parsed = com.example.notivib.domain.model.parseKeywords(evaluationResult.rule.keyword)
                                    if (parsed.isNotEmpty()) parsed.joinToString(", ") else "Any"
                                }
                                triggerAlarm(
                                    appName = appName.ifEmpty { packageName },
                                    keyword = matchedKwDisplay,
                                    vibrationOnly = evaluationResult.rule.vibrationOnly,
                                    ruleName = evaluationResult.rule.ruleName
                                )
                            }
                        }
                        is com.example.notivib.domain.usecase.EvaluationResult.Mute -> {
                            try {
                                cancelNotification(it.key)
                                notificationLogRepository.addSystemLog("Silently dismissed notification from $packageName (Focus Mode)")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        is com.example.notivib.domain.usecase.EvaluationResult.Ignore -> {}
                    }
                } catch (e: Exception) {
                    notificationLogRepository.addSystemLog("[Engine Error] Unhandled: ${e.message}")
                    EngineForegroundService.showInterruptedBanner(
                        this@InterceptorService,
                        "Notification monitoring hit an unexpected error. Tap restart."
                    )
                }
            }
        }
    }

    private fun triggerAlarm(appName: String, keyword: String, vibrationOnly: Boolean, ruleName: String) {
        val intent = Intent(this, ActiveAlarmService::class.java).apply {
            action = ActiveAlarmService.ACTION_START
            putExtra("APP_NAME", appName)
            putExtra("KEYWORD", keyword)
            putExtra("VIBRATION_ONLY", vibrationOnly)
            putExtra("RULE_NAME", ruleName)
        }
        try {
            startForegroundService(intent)
        } catch (e: Exception) {
            notificationLogRepository.addSystemLog("[Engine Error] Failed to start alarm: ${e.message}")
            EngineForegroundService.showInterruptedBanner(
                this,
                "Couldn't start the alarm for a matched rule. Tap restart."
            )
        }
    }
}
