package com.example.notivib.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notivib.domain.manager.ProtectedNoticeManager
import com.example.notivib.domain.repository.ProtectedNoticeRepository
import com.example.notivib.framework.utils.ReminderDiagnostics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProtectedNoticeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ProtectedNoticeRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val key = intent.getStringExtra(EXTRA_KEY) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_ACK -> {
                        ProtectedNoticeManager.acknowledge(context, key)
                    }
                    ACTION_REPOST -> {
                        val notice = repository.getAll().find { it.key == key }
                        if (notice != null) {
                            ProtectedNoticeManager.post(context, notice)
                            ReminderDiagnostics.log(
                                context,
                                "[Protect] Re-posted after swipe: ${notice.appName}: ${notice.title} (${notice.ruleName})"
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                ReminderDiagnostics.log(context, "[Protect] FAILED receiver ($action): ${t.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ACK = "com.example.notivib.action.PROTECTED_NOTICE_ACK"
        const val ACTION_REPOST = "com.example.notivib.action.PROTECTED_NOTICE_REPOST"
        const val EXTRA_KEY = "extra_notice_key"
    }
}
