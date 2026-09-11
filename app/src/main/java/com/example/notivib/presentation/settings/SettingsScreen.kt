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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notivib.framework.service.InterceptorService
import com.example.notivib.framework.utils.BatteryOptimizationHelper
import com.example.notivib.framework.utils.EngineState
import com.example.notivib.framework.utils.XiaomiDeviceHelper
import com.example.notivib.presentation.rules_list.RulesListViewModel
import androidx.compose.foundation.BorderStroke
import com.example.notivib.presentation.theme.SourceSerif4
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    rulesViewModel: RulesListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val logRepository = remember(context) { com.example.notivib.domain.repository.NotificationLogRepository(context.applicationContext) }
    val systemLogs by logRepository.systemLogs.collectAsState()
    val interceptLogs by logRepository.logs.collectAsState()
    var isStorageClearedManually by remember { mutableStateOf(false) }

    fun checkHasCacheFiles(ctx: android.content.Context): Boolean {
        return try {
            val f1 = ctx.cacheDir?.listFiles()
            val f2 = ctx.codeCacheDir?.listFiles()
            (f1 != null && f1.isNotEmpty()) || (f2 != null && f2.isNotEmpty())
        } catch (e: Exception) {
            false
        }
    }

    val hasDataToClear = !isStorageClearedManually && (systemLogs.isNotEmpty() || interceptLogs.isNotEmpty() || checkHasCacheFiles(context))
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

    val exportRulesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        rulesViewModel.exportRules { json ->
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { it.write(json) }
                }
                Toast.makeText(context, "Rules exported", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export rules", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importRulesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (e: Exception) {
            null
        }
        if (json == null) {
            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        rulesViewModel.importRules(json) { result ->
            result.onSuccess { count ->
                Toast.makeText(context, "Imported $count rule(s)", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Failed to import rules: invalid file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(com.example.notivib.presentation.theme.icons.arrow_back_ios_new, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.White)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            
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
            
            var showForegroundNotification by remember { mutableStateOf(com.example.notivib.framework.utils.EngineState.isShowForegroundNotification(context)) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Persistent Notification", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Keep NotiVib alive in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = showForegroundNotification,
                        onCheckedChange = { 
                            showForegroundNotification = it
                            com.example.notivib.framework.utils.EngineState.setShowForegroundNotification(context, it)
                            context.sendBroadcast(Intent(context, com.example.notivib.framework.receiver.ScheduleReceiver::class.java))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.background,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (!canDrawOverlays && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Display Over Other Apps Required", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("Required to display full screen interception alerts.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open settings.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Grant Permission", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isIgnoringBatteryOptimizations) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Allow Background Usage Required", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("Prevents OS from stopping engine during deep sleep.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    try {
                                        batteryLauncher.launch(BatteryOptimizationHelper.getIgnoreBatteryOptimizationIntent(context))
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Allow Usage", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { 
                                    EngineState.setBatteryOptimizationDismissed(context, true)
                                    isIgnoringBatteryOptimizations = true
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Already Enabled", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            
            if (XiaomiDeviceHelper.isXiaomiDevice()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Xiaomi / HyperOS Setup Required", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "HyperOS & MIUI forcefully disconnect notification listeners when apps are closed. Enable Autostart and background permissions to keep NotiVib running.",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    try {
                                        context.startActivity(XiaomiDeviceHelper.getAutostartIntent(context))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Open Settings > Apps > Autostart", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Autostart", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { 
                                    try {
                                        context.startActivity(XiaomiDeviceHelper.getMiuiPermissionsIntent(context))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Open App Info > Permissions", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Pop-Up Perms", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Backup & Restore Rules", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Save all your rules to a file, or restore them on this or another device. Importing merges with your existing rules by matching id.",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportRulesLauncher.launch("notivib_rules_backup.json") },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text("Export Rules", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { importRulesLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text("Import Rules", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("App Storage & Maintenance", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Clears temporary app cache, system diagnostic logs, and saved notification history in one tap to keep NotiVib lightweight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (hasDataToClear) {
                                try {
                                    context.cacheDir?.deleteRecursively()
                                    context.codeCacheDir?.deleteRecursively()
                                } catch (e: Exception) {}
                                logRepository.clearSystemLogs()
                                logRepository.clearInterceptLogs()
                                isStorageClearedManually = true
                                Toast.makeText(context, "App cache, diagnostic logs & history cleared", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Storage and logs are already cleared", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasDataToClear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (hasDataToClear) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(
                            if (hasDataToClear) "Clear Cache & All Logs" else "Already Cleared",
                            fontFamily = SourceSerif4,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Notification History Link Button (As per Design Mockup)
            Button(
                onClick = onNavigateToLogs,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Notification History", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun EngineStatusCard(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
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
                        fontFamily = SourceSerif4,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White
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
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}