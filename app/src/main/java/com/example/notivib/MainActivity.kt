package com.example.notivib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.notivib.presentation.navigation.AppNavigation
import com.example.notivib.presentation.theme.NotiVibTheme
import dagger.hilt.android.AndroidEntryPoint

import com.example.notivib.framework.utils.XiaomiDeviceHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotiVibTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        XiaomiDeviceHelper.requestRebindListener(this)
    }
}
