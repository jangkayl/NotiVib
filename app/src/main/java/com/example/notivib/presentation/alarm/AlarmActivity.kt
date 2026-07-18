package com.example.notivib.presentation.alarm

import android.content.Intent

import android.os.Bundle

import androidx.activity.ComponentActivity

import androidx.activity.compose.setContent

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.animation.core.*

import androidx.compose.ui.graphics.Brush

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.outlined.*

import androidx.compose.material.icons.automirrored.outlined.*

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.BorderStroke

import androidx.compose.ui.text.style.TextAlign

import com.example.notivib.framework.service.ActiveAlarmService

class AlarmActivity : ComponentActivity() {

    private val closeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == "com.example.notivib.ACTION_CLOSE_ALARM_SCREEN") {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            closeReceiver,
            android.content.IntentFilter("com.example.notivib.ACTION_CLOSE_ALARM_SCREEN"),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

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
        val ruleId = intent.getStringExtra("RULE_ID") ?: ""
        val mode = intent.getIntExtra(ActiveAlarmService.EXTRA_ALARM_MODE, ActiveAlarmService.MODE_INTERCEPT)

        setContent {
            androidx.activity.compose.BackHandler {
                // Ignore back presses to prevent dismissing the alarm
            }
            AlarmScreen(
                appName = appName,
                keyword = keyword,
                mode = mode,
                onAcknowledge = {
                    val stopIntent = Intent(this@AlarmActivity, ActiveAlarmService::class.java).apply {
                        action = ActiveAlarmService.ACTION_STOP
                        putExtra("APP_NAME", appName)
                        putExtra("RULE_ID", ruleId)
                        putExtra(ActiveAlarmService.EXTRA_ALARM_MODE, mode)
                    }
                    startService(stopIntent)
                    finish()
                }
            )
        }

    }

}

data class Captcha(val prompt: String, val answer: String)

fun generateCaptcha(): Captcha {
    val isMath = kotlin.random.Random.nextBoolean()
    return if (isMath) {
        val a = kotlin.random.Random.nextInt(10, 50)
        val b = kotlin.random.Random.nextInt(10, 50)
        Captcha("What is $a + $b?", (a + b).toString())
    } else {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val str = (1..5).map { chars.random() }.joinToString("")
        Captcha("Type '$str'", str)
    }
}

@Composable
fun AlarmScreen(appName: String, keyword: String, mode: Int, onAcknowledge: () -> Unit) {
    var captchaAnswer by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val isFollowUp = mode == ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP || mode == ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP
    val captcha = androidx.compose.runtime.remember { generateCaptcha() }
    val isAcknowledgeEnabled = if (isFollowUp) captchaAnswer.equals(captcha.answer, ignoreCase = true) else true

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val themeColor = when (mode) {
        ActiveAlarmService.MODE_SCHEDULE_START, ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP -> Color(0xFF00FF00) // Green
        ActiveAlarmService.MODE_SCHEDULE_END, ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP -> Color(0xFFFFD700) // Yellow
        else -> Color.Red
    }
    
    val title = when (mode) {
        ActiveAlarmService.MODE_SCHEDULE_START -> "SCHEDULE STARTED"
        ActiveAlarmService.MODE_SCHEDULE_END -> "SCHEDULE ENDED"
        ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP -> "FOLLOW-UP REMINDER"
        ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP -> "FOLLOW-UP REMINDER"
        else -> "INTERCEPTION ALERT"
    }

    val subtitle = when (mode) {
        ActiveAlarmService.MODE_SCHEDULE_START -> "The interception window has begun."
        ActiveAlarmService.MODE_SCHEDULE_END -> "The interception window has ended."
        ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP -> "Solve the captcha to confirm you are awake and ready for interception."
        ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP -> "Solve the captcha to confirm you are awake."
        else -> "A critical notification has matched your rules."
    }

    val icon = when (mode) {
        ActiveAlarmService.MODE_SCHEDULE_START, ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP -> Icons.Outlined.CircleNotifications
        ActiveAlarmService.MODE_SCHEDULE_END, ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP -> Icons.Outlined.CircleNotifications
        else -> Icons.Filled.Warning
    }

    MaterialTheme {

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            Box(

                modifier = Modifier

                    .fillMaxSize()

                    .background(
                        Brush.radialGradient(
                            colors = listOf(themeColor.copy(alpha = pulseAlpha), Color.Transparent),
                            radius = 1200f
                        )
                    )

            )

            Column(

                modifier = Modifier

                    .fillMaxSize()

                    .padding(32.dp),

                verticalArrangement = Arrangement.Center,

                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Icon(
                    icon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    title, 
                    color = Color.White, 
                    style = MaterialTheme.typography.headlineLarge, 
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    subtitle, 
                    color = Color.Gray, 
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(48.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TARGET APPLICATION", style = MaterialTheme.typography.labelMedium, color = themeColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        Spacer(Modifier.height(8.dp))

                        Text(appName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(24.dp))

                        HorizontalDivider(color = themeColor.copy(alpha = 0.1f))
                        Spacer(Modifier.height(24.dp))
                        if (mode == ActiveAlarmService.MODE_INTERCEPT) {
                            Text("MATCHED KEYWORD", style = MaterialTheme.typography.labelMedium, color = themeColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(keyword, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("SCHEDULE", style = MaterialTheme.typography.labelMedium, color = themeColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Active Interception Phase", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                    }

                }

                if (isFollowUp) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = captcha.prompt,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = captchaAnswer,
                        onValueChange = { captchaAnswer = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = themeColor.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }

                Spacer(Modifier.height(64.dp))

                Button(
                    onClick = onAcknowledge,
                    enabled = isAcknowledgeEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor, disabledContainerColor = Color.DarkGray),

                    shape = RoundedCornerShape(50),

                    modifier = Modifier.fillMaxWidth().height(64.dp),

                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)

                ) {

                    Text("ACKNOWLEDGE & DISMISS", fontWeight = FontWeight.ExtraBold, color = if (mode == ActiveAlarmService.MODE_SCHEDULE_END) Color.Black else Color.White, letterSpacing = 1.sp)

                }

            }

        }

    }

}

@Preview(showBackground = true)

@Composable

fun AlarmScreenPreview() {

    AlarmScreen(appName = "WhatsApp", keyword = "Emergency", mode = ActiveAlarmService.MODE_INTERCEPT, onAcknowledge = {})

}