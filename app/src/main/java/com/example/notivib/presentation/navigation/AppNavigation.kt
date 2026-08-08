package com.example.notivib.presentation.navigation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notivib.framework.service.InterceptorService
import com.example.notivib.framework.utils.BatteryOptimizationHelper
import com.example.notivib.framework.utils.EngineState
import com.example.notivib.presentation.rules_list.NotificationLogScreen
import com.example.notivib.presentation.rules_list.RulesListScreen
import com.example.notivib.presentation.theme.SourceSerif4
import com.example.notivib.presentation.theme.icons.campaign
import com.example.notivib.presentation.theme.icons.energy_savings_leaf
import com.example.notivib.presentation.theme.icons.mobile_sound
import com.example.notivib.presentation.theme.icons.face
import com.example.notivib.presentation.theme.icons.arrow_forward_ios

enum class Destination {
    RulesList,
    Logs,
    Settings,
    EditRule
}

fun checkNotificationAccess(context: Context): Boolean {
    return try {
        val componentName = ComponentName(context, InterceptorService::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        enabledListeners != null && enabledListeners.contains(componentName.flattenToString())
    } catch (e: Exception) {
        false
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    var hasNotificationAccess by remember { mutableStateOf(checkNotificationAccess(context)) }
    var hasPostNotificationPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = checkNotificationAccess(context)
                isIgnoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasPostNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasNotificationAccess && hasPostNotificationPermission) {
        var currentDestination by remember { mutableStateOf(Destination.RulesList) }
        var editingRule: com.example.notivib.domain.model.AlarmRule? by remember { mutableStateOf(null) }
        when (currentDestination) {
            Destination.RulesList -> RulesListScreen(
                onNavigateToLogs = { currentDestination = Destination.Logs },
                onNavigateToSettings = { currentDestination = Destination.Settings },
                onNavigateToEditRule = { rule ->
                    editingRule = rule
                    currentDestination = Destination.EditRule
                }
            )
            Destination.Logs -> NotificationLogScreen(onNavigateBack = { currentDestination = Destination.RulesList })
            Destination.Settings -> com.example.notivib.presentation.settings.SettingsScreen(
                onNavigateBack = { currentDestination = Destination.RulesList },
                onNavigateToLogs = { currentDestination = Destination.Logs }
            )
            Destination.EditRule -> com.example.notivib.presentation.rules_list.EditRuleScreen(
                rule = editingRule,
                onNavigateBack = { currentDestination = Destination.RulesList }
            )
        }
    } else {
        PermissionsScreen(
            hasNotificationAccess = hasNotificationAccess,
            hasPostNotificationPermission = hasPostNotificationPermission,
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
            onPostNotificationResult = { hasPostNotificationPermission = it }
        )
    }
}

@Composable
fun PermissionsScreen(
    hasNotificationAccess: Boolean,
    hasPostNotificationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onPostNotificationResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var localIsIgnoringBatteryOptimizations by remember(isIgnoringBatteryOptimizations) {
        mutableStateOf(isIgnoringBatteryOptimizations)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPostNotificationResult(isGranted)
    }
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        
        // Dynamic Layout Math based on screen height
        val isCompact = screenHeight < 720.dp
        val isUltraCompact = screenHeight < 640.dp
        
        val logoSize = when {
            isUltraCompact -> 64.dp
            isCompact -> 72.dp
            else -> 84.dp
        }
        
        val screenPadding = when {
            isUltraCompact -> 14.dp
            isCompact -> 18.dp
            else -> 24.dp
        }
        
        val sectionSpacing = when {
            isUltraCompact -> 10.dp
            isCompact -> 14.dp
            else -> 20.dp
        }
        
        val cardInnerPadding = when {
            isUltraCompact -> 14.dp
            isCompact -> 16.dp
            else -> 20.dp
        }
        
        val cardSpacing = when {
            isUltraCompact -> 8.dp
            isCompact -> 12.dp
            else -> 16.dp
        }

        // Subtitle dynamic sizing & line height
        val subtitleFontSize = when {
            isUltraCompact -> 12.5.sp
            isCompact -> 14.sp
            else -> 15.5.sp
        }
        val subtitleLineHeight = when {
            isUltraCompact -> 16.sp
            isCompact -> 18.sp
            else -> 21.sp
        }

        // Privacy Card dynamic typography & icon sizing
        val privacyHeaderIconSize = when {
            isUltraCompact -> 18.dp
            isCompact -> 20.dp
            else -> 22.dp
        }
        val privacyHeaderFontSize = when {
            isUltraCompact -> 14.sp
            isCompact -> 15.5.sp
            else -> 17.sp
        }
        val privacyBulletFontSize = when {
            isUltraCompact -> 11.sp
            isCompact -> 12.sp
            else -> 13.5.sp
        }
        val privacyBulletLineHeight = when {
            isUltraCompact -> 15.sp
            isCompact -> 17.sp
            else -> 19.sp
        }
        val privacyInnerSpacing = when {
            isUltraCompact -> 6.dp
            isCompact -> 8.dp
            else -> 10.dp
        }

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = screenPadding, vertical = screenPadding / 2),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(sectionSpacing / 2))
                
                // NotiVib Logo with Dynamic Soft Rounded Corners
                Image(
                    painter = painterResource(id = com.example.notivib.R.drawable.notivib_new_logo),
                    contentDescription = "NotiVib Logo",
                    modifier = Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(logoSize * 0.25f))
                )
                
                Spacer(Modifier.height(sectionSpacing))
                Text(
                    "Welcome to NotiVib",
                    fontFamily = SourceSerif4,
                    style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(if (isCompact) 6.dp else 10.dp))
                Text(
                    "To provide powerful notification interception, NotiVib needs access to the following core systems.", 
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = subtitleFontSize,
                    lineHeight = subtitleLineHeight,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(sectionSpacing))

                // Friendly & Professional Privacy Assurance Card (Dynamic typography & spacing)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(cardInnerPadding)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                face,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(privacyHeaderIconSize)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Your Privacy Comes First",
                                fontFamily = SourceSerif4,
                                fontWeight = FontWeight.Bold,
                                fontSize = privacyHeaderFontSize,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(privacyInnerSpacing))
                        Text(
                            "• 100% Private: We never collect, track, or share your notifications or messages.\n" +
                            "• Works Offline: NotiVib has no internet access, so your data stays on your phone.\n" +
                            "• Stored On-Device: Your rules, notification logs, and preferences stay private on your device.",
                            fontSize = privacyBulletFontSize,
                            lineHeight = privacyBulletLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }

                Spacer(Modifier.height(sectionSpacing))

                // 1. System Notifications Card
                if (!hasPostNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionCard(
                        title = "Allow NotiVib to Send Notifications",
                        description = "Required to display active background monitoring status and trigger loud ringtone or vibration alerts when rules match.",
                        icon = campaign,
                        cardPadding = cardInnerPadding,
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                    Spacer(Modifier.height(cardSpacing))
                }

                // 2. Allow Background Activity Card
                if (!localIsIgnoringBatteryOptimizations) {
                    PermissionCard(
                        title = "Allow Background Activity",
                        description = "Recommended: Keeps NotiVib running reliably without being closed by your phone's battery saver. NotiVib uses virtually zero background power.",
                        icon = energy_savings_leaf,
                        cardPadding = cardInnerPadding,
                        secondaryText = "Already Enabled / Skip",
                        onSecondaryClick = {
                            EngineState.setBatteryOptimizationDismissed(context, true)
                            localIsIgnoringBatteryOptimizations = true
                        },
                        onClick = {
                            try {
                                context.startActivity(BatteryOptimizationHelper.getIgnoreBatteryOptimizationIntent(context))
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(BatteryOptimizationHelper.getAppInfoIntent(context))
                                } catch (e2: Exception) {}
                            }
                        }
                    )
                    Spacer(Modifier.height(cardSpacing))
                }

                // 3. Notification Interception Card
                if (!hasNotificationAccess) {
                    PermissionCard(
                        title = "Notification Interception",
                        description = "Required to read incoming notifications from your target apps in real-time so your customized alarm rules can evaluate instantly.",
                        icon = mobile_sound,
                        cardPadding = cardInnerPadding,
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (e: Exception) {}
                        }
                    )
                }
                Spacer(Modifier.height(sectionSpacing / 2))
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cardPadding: Dp = 20.dp,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(10.dp))
                Icon(arrow_forward_ios, contentDescription = "Grant", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            if (secondaryText != null && onSecondaryClick != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onSecondaryClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            secondaryText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    MaterialTheme {
        PermissionsScreen(
            hasNotificationAccess = false,
            hasPostNotificationPermission = false,
            isIgnoringBatteryOptimizations = false,
            onPostNotificationResult = {}
        )
    }
}
