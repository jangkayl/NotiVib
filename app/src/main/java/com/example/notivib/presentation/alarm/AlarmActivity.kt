package com.example.notivib.presentation.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notivib.framework.service.ActiveAlarmService

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        val appName = intent.getStringExtra("APP_NAME") ?: "An App"
        val keyword = intent.getStringExtra("KEYWORD") ?: "a keyword"

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1E0A0A)).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ALARM TRIGGERED!", color = Color.Red, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(32.dp))
                    Text("Matched App:", color = Color.Gray)
                    Text(appName, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("Matched Rule:", color = Color.Gray)
                    Text(keyword, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(64.dp))
                    Button(
                        onClick = {
                            val stopIntent = Intent(this@AlarmActivity, ActiveAlarmService::class.java).apply {
                                action = ActiveAlarmService.ACTION_STOP
                            }
                            startService(stopIntent)
                            finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) {
                        Text("ACKNOWLEDGE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
