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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notivib.framework.service.InterceptorService
import com.example.notivib.presentation.rules_list.NotificationLogScreen
import com.example.notivib.presentation.rules_list.RulesListScreen

enum class Destination {
    RulesList,
    Logs
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = checkNotificationAccess(context)
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
        
        when (currentDestination) {
            Destination.RulesList -> RulesListScreen(onNavigateToLogs = { currentDestination = Destination.Logs })
            Destination.Logs -> NotificationLogScreen(onNavigateBack = { currentDestination = Destination.RulesList })
        }
    } else {
        PermissionsScreen(
            hasNotificationAccess = hasNotificationAccess,
            hasPostNotificationPermission = hasPostNotificationPermission,
            onPostNotificationResult = { hasPostNotificationPermission = it }
        )
    }
}

@Composable
fun PermissionsScreen(
    hasNotificationAccess: Boolean,
    hasPostNotificationPermission: Boolean,
    onPostNotificationResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPostNotificationResult(isGranted)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(24.dp))
            Text("Welcome to NotiVib", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Text(
                "To provide powerful notification interception, NotiVib needs access to the following core systems.", 
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(48.dp))

            if (!hasPostNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    title = "System Notifications",
                    description = "Required to run in the background and display alarm states.",
                    icon = Icons.Default.NotificationsActive,
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
                Spacer(Modifier.height(16.dp))
            }

            if (!hasNotificationAccess) {
                PermissionCard(
                    title = "Notification Interception",
                    description = "Required to read incoming notifications and trigger your rules.",
                    icon = Icons.Default.Hearing,
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (e: Exception) {}
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Grant", tint = MaterialTheme.colorScheme.primary)
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
            onPostNotificationResult = {}
        )
    }
}
