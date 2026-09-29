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

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_ACK_ALL -> {
                        ProtectedNoticeManager.acknowledgeAll(context)
                    }
                    ACTION_REPOST -> {
                        val notices = repository.getAll()
                        ProtectedNoticeManager.refresh(context, notices)
                        if (notices.isNotEmpty()) {
                            ReminderDiagnostics.log(context, "[Protect] Re-posted after swipe")
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
        const val ACTION_ACK_ALL = "com.example.notivib.action.PROTECTED_NOTICE_ACK_ALL"
        const val ACTION_REPOST = "com.example.notivib.action.PROTECTED_NOTICE_REPOST"
    }
}
