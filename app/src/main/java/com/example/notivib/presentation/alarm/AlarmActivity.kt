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

            AlarmScreen(

                appName = appName,

                keyword = keyword,

                onAcknowledge = {

                    val stopIntent = Intent(this@AlarmActivity, ActiveAlarmService::class.java).apply {

                        action = ActiveAlarmService.ACTION_STOP

                    }

                    startService(stopIntent)

                    finish()

                }

            )

        }

    }

}

@Composable

fun AlarmScreen(appName: String, keyword: String, onAcknowledge: () -> Unit) {

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

    MaterialTheme {

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            Box(

                modifier = Modifier

                    .fillMaxSize()

                    .background(

                        Brush.radialGradient(

                            colors = listOf(Color.Red.copy(alpha = pulseAlpha), Color.Transparent),

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

                    Icons.Filled.Warning,

                    contentDescription = null,

                    tint = Color.Red,

                    modifier = Modifier.size(80.dp)

                )

                Spacer(Modifier.height(24.dp))

                Text(

                    "INTERCEPTION ALERT", 

                    color = Color.White, 

                    style = MaterialTheme.typography.headlineLarge, 

                    fontWeight = FontWeight.ExtraBold,

                    letterSpacing = 2.sp,

                    textAlign = TextAlign.Center

                )

                Spacer(Modifier.height(8.dp))

                Text(

                    "A critical notification has matched your rules.", 

                    color = Color.Gray, 

                    style = MaterialTheme.typography.bodyLarge,

                    textAlign = TextAlign.Center

                )

                Spacer(Modifier.height(48.dp))

                Card(

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(24.dp),

                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),

                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))

                ) {

                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                        Text("TARGET APPLICATION", style = MaterialTheme.typography.labelMedium, color = Color.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        Spacer(Modifier.height(8.dp))

                        Text(appName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(24.dp))

                        HorizontalDivider(color = Color.Red.copy(alpha = 0.1f))

                        Spacer(Modifier.height(24.dp))

                        Text("MATCHED KEYWORD", style = MaterialTheme.typography.labelMedium, color = Color.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        Spacer(Modifier.height(8.dp))

                        Text(keyword, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                    }

                }

                Spacer(Modifier.height(64.dp))

                Button(

                    onClick = onAcknowledge,

                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),

                    shape = RoundedCornerShape(50),

                    modifier = Modifier.fillMaxWidth().height(64.dp),

                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)

                ) {

                    Text("ACKNOWLEDGE & DISMISS", fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)

                }

            }

        }

    }

}

@Preview(showBackground = true)

@Composable

fun AlarmScreenPreview() {

    AlarmScreen(appName = "WhatsApp", keyword = "Emergency", onAcknowledge = {})

}