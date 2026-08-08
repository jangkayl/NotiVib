package com.example.notivib.presentation.alarm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.notivib.framework.service.ActiveAlarmService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val ruleName = intent.getStringExtra("RULE_NAME") ?: ""
        val mode = intent.getIntExtra(ActiveAlarmService.EXTRA_ALARM_MODE, ActiveAlarmService.MODE_INTERCEPT)

        setContent {
            androidx.activity.compose.BackHandler {
                // Ignore back presses to prevent dismissing the alarm
            }
            AlarmScreen(
                appName = appName,
                keyword = keyword,
                mode = mode,
                ruleName = ruleName,
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

private data class InterceptionThemeSpec(
    val backgroundColor: Color,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
fun RuleAppPill(
    ruleName: String,
    appName: String,
    darkColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayText = ruleName.ifEmpty { appName }

    var appIconBitmap by remember(appName, ruleName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(appName, ruleName) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            var iconDrawable: Drawable? = try {
                if (appName.isNotEmpty()) pm.getApplicationIcon(appName) else null
            } catch (e: Exception) {
                null
            }

            if (iconDrawable == null && ruleName.isNotEmpty()) {
                iconDrawable = try {
                    pm.getApplicationIcon(ruleName)
                } catch (e: Exception) {
                    null
                }
            }

            if (iconDrawable == null && appName.isNotEmpty()) {
                try {
                    val packages = pm.getInstalledApplications(0)
                    for (appInfo in packages) {
                        val label = pm.getApplicationLabel(appInfo).toString()
                        if (label.equals(appName, ignoreCase = true)) {
                            iconDrawable = pm.getApplicationIcon(appInfo)
                            break
                        }
                    }
                } catch (e: Exception) {}
            }

            if (iconDrawable != null) {
                try {
                    val w = if (iconDrawable.intrinsicWidth > 0) iconDrawable.intrinsicWidth else 96
                    val h = if (iconDrawable.intrinsicHeight > 0) iconDrawable.intrinsicHeight else 96
                    val b = iconDrawable.toBitmap(w, h, Bitmap.Config.ARGB_8888)
                    appIconBitmap = b.asImageBitmap()
                } catch (e: Exception) {}
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, darkColor),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = displayText,
                color = darkColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AlarmScreen(
    appName: String,
    keyword: String,
    mode: Int,
    ruleName: String,
    onAcknowledge: () -> Unit
) {
    var captchaAnswer by remember { mutableStateOf("") }
    val isFollowUp = mode == ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP || mode == ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP
    val captcha = remember { generateCaptcha() }
    val isAcknowledgeEnabled = if (isFollowUp) captchaAnswer.equals(captcha.answer, ignoreCase = true) else true

    val darkColor = Color(0xFF1C1B1F)

    val themeSpec = when (mode) {
        ActiveAlarmService.MODE_SCHEDULE_START -> InterceptionThemeSpec(
            backgroundColor = Color(0xFFD9FF0B), // Lime Green
            icon = rocket_launch,
            title = "Schedule Started",
            subtitle = "Interception window has begun."
        )
        ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP -> InterceptionThemeSpec(
            backgroundColor = Color(0xFFD9FF0B), // Lime Green
            icon = extension,
            title = "Follow-up Reminder",
            subtitle = "Solve the captcha to confirm"
        )
        ActiveAlarmService.MODE_SCHEDULE_END -> InterceptionThemeSpec(
            backgroundColor = Color(0xFFFFCE0B), // Amber / Yellow
            icon = hourglass_bottom,
            title = "Schedule Ended",
            subtitle = "Interception window has ended."
        )
        ActiveAlarmService.MODE_SCHEDULE_END_FOLLOWUP -> InterceptionThemeSpec(
            backgroundColor = Color(0xFFFFCE0B), // Amber / Yellow
            icon = extension,
            title = "Follow-up Reminder",
            subtitle = "Solve the captcha to confirm"
        )
        else -> InterceptionThemeSpec(
            backgroundColor = Color(0xFFFF0B0B), // Vibrant Red #FF0B0B
            icon = bolt,
            title = "Interception Alert",
            subtitle = "A critical notification has matched your rules"
        )
    }

    // Prominent, highly noticeable color pulse animation for background and icon glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeSpec.backgroundColor)
        ) {
            // Dynamic Ambient Color Shifting Overlay (highly noticeable fade pulse across all themes)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = pulseAlpha * 0.70f),
                                themeSpec.backgroundColor.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            radius = 1600f
                        )
                    )
            )

            // Main Screen Layout Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Stacked Hero Column following exact mockup layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Large Prominent Icon with Ambient Pulse Halo
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .graphicsLayer {
                                    alpha = pulseAlpha * 0.60f
                                    scaleX = pulseScale * 1.15f
                                    scaleY = pulseScale * 1.15f
                                }
                                .background(Color.White.copy(alpha = 0.45f), shape = CircleShape)
                        )
                        Icon(
                            imageVector = themeSpec.icon,
                            contentDescription = null,
                            tint = darkColor,
                            modifier = Modifier
                                .size(135.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    Text(
                        text = themeSpec.title,
                        color = darkColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = themeSpec.subtitle,
                        color = darkColor.copy(alpha = 0.95f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Reduced spacing between subtitle and Interception Title label
                    Spacer(modifier = Modifier.height(14.dp))

                    // Section Label 1: Interception Title
                    Text(
                        text = "Interception Title",
                        color = darkColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pill 1: Rule / App Pill
                    RuleAppPill(
                        ruleName = ruleName,
                        appName = appName,
                        darkColor = darkColor
                    )

                    // Section 2: Matched Keywords or Captcha
                    if (isFollowUp) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = captcha.prompt,
                            color = darkColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = captchaAnswer,
                            onValueChange = { captchaAnswer = it },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                focusedBorderColor = darkColor,
                                unfocusedBorderColor = darkColor,
                                focusedTextColor = darkColor,
                                unfocusedTextColor = darkColor
                            ),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = darkColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(56.dp)
                        )
                    } else if (mode == ActiveAlarmService.MODE_INTERCEPT) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Matched Keywords",
                            color = darkColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, darkColor),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(56.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = keyword.replace("|||", ", ").ifEmpty { "Boss, Urgent, @Name" },
                                    color = darkColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Button placed directly below pills
                    Button(
                        onClick = onAcknowledge,
                        enabled = isAcknowledgeEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = darkColor,
                            disabledContainerColor = darkColor.copy(alpha = 0.4f),
                            contentColor = themeSpec.backgroundColor,
                            disabledContentColor = themeSpec.backgroundColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Acknowledge & Dismiss",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeSpec.backgroundColor
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreviewScheduleStarted() {
    AlarmScreen(
        appName = "com.facebook.orca",
        keyword = "Emergency",
        mode = ActiveAlarmService.MODE_SCHEDULE_START,
        ruleName = "",
        onAcknowledge = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreviewScheduleStartedFollowup() {
    AlarmScreen(
        appName = "WhatsApp",
        keyword = "Emergency",
        mode = ActiveAlarmService.MODE_SCHEDULE_START_FOLLOWUP,
        ruleName = "Rule Name",
        onAcknowledge = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreviewScheduleEnded() {
    AlarmScreen(
        appName = "WhatsApp",
        keyword = "Emergency",
        mode = ActiveAlarmService.MODE_SCHEDULE_END,
        ruleName = "Rule Name",
        onAcknowledge = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreviewInterceptAlert() {
    AlarmScreen(
        appName = "com.facebook.orca",
        keyword = "Boss, Urgent, @Name",
        mode = ActiveAlarmService.MODE_INTERCEPT,
        ruleName = "Rule Name",
        onAcknowledge = {}
    )
}