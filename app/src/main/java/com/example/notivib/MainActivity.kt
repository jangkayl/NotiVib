package com.example.notivib

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.example.notivib.framework.receiver.ScheduleReceiver
import com.example.notivib.presentation.navigation.AppNavigation
import com.example.notivib.presentation.theme.NotiVibTheme
import dagger.hilt.android.AndroidEntryPoint

import com.example.notivib.framework.utils.XiaomiDeviceHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val navTarget = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navTarget.value = intent?.getStringExtra(EXTRA_DESTINATION)
        setContent {
            NotiVibTheme {
                AppNavigation(
                    navTarget = navTarget.value,
                    onClearNavTarget = { navTarget.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_DESTINATION)?.let {
            navTarget.value = it
        }
    }

    override fun onResume() {
        super.onResume()
        XiaomiDeviceHelper.requestRebindListener(this)
        // Re-arm schedule + reminder alarms whenever the app is opened. Alarms use
        // FLAG_UPDATE_CURRENT with fixed request codes, so this broadcast is idempotent.
        sendBroadcast(Intent(this, ScheduleReceiver::class.java))
    }

    companion object {
        const val EXTRA_DESTINATION = "extra_destination"
        const val DESTINATION_PROTECTED_NOTICES = "protected_notices"
    }
}
