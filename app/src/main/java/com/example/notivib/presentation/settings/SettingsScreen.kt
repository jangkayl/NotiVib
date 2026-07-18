package com.example.notivib.presentation.settings
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notivib.framework.service.InterceptorService
import com.example.notivib.framework.utils.BatteryOptimizationHelper
import androidx.compose.foundation.BorderStroke

fun checkNotificationAccess(context: android.content.Context): Boolean {
    return try {
        val componentName = ComponentName(context, InterceptorService::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        enabledListeners != null && enabledListeners.contains(componentName.flattenToString())
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var hasNotificationAccess by remember { mutableStateOf(checkNotificationAccess(context)) }
    var isServiceEnabled by remember {
        mutableStateOf(com.example.notivib.framework.utils.EngineState.isGloballyEnabled(context))
    }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
    var canDrawOverlays by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = checkNotificationAccess(context)
                isIgnoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                isServiceEnabled = com.example.notivib.framework.utils.EngineState.isGloballyEnabled(context)
                canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isIgnoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }
    
    var permissionGrantedState by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGrantedState = isGranted
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            EngineStatusCard(
                isActive = hasNotificationAccess && isServiceEnabled,
                onToggle = { enable ->
                    isServiceEnabled = enable
                    com.example.notivib.framework.utils.EngineState.setGloballyEnabled(context, enable)
                    if (!enable) {
                        Toast.makeText(context, "Engine suspended.", Toast.LENGTH_SHORT).show()
                    }
                    context.sendBroadcast(Intent(context, com.example.notivib.framework.receiver.ScheduleReceiver::class.java))
                }
            )
            
            if (!canDrawOverlays && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Display Over Other Apps Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("This allows the interception screen to forcibly pop up even when you are using the phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open settings. Please open manually.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            if (!isIgnoringBatteryOptimizations) {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Allow Background Usage Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("This app needs background usage allowed to run reliably in the background.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                try {
                                    batteryLauncher.launch(BatteryOptimizationHelper.getIgnoreBatteryOptimizationIntent(context))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Background Usage")
                        }
                    }
                }
            }
            
            var showForegroundNotification by remember { mutableStateOf(com.example.notivib.framework.utils.EngineState.isShowForegroundNotification(context)) }
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Persistent Notification", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Keep NotiVib alive in the background. Disabling this hides the notification but may cause Android to kill the background engine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = showForegroundNotification,
                        onCheckedChange = { 
                            showForegroundNotification = it
                            com.example.notivib.framework.utils.EngineState.setShowForegroundNotification(context, it)
                            context.sendBroadcast(Intent(context, com.example.notivib.framework.receiver.ScheduleReceiver::class.java))
                        }
                    )
                }
            }
            
            if (!permissionGrantedState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Post Notifications Permission Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
            
            if (!hasNotificationAccess) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Notification Access Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open settings. Please open manually.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EngineStatusCard(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(12.dp).background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (isActive) "Engine Active" else "Engine Suspended",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isActive) "Intercepting notifications" else "All rules are paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}