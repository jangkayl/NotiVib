package com.example.notivib.presentation.rules_list

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.presentation.theme.ElectricBlue
import com.example.notivib.framework.service.InterceptorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun checkNotificationAccess(context: Context): Boolean {
    return try {
        val componentName = ComponentName(context, InterceptorService::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        enabledListeners != null && enabledListeners.contains(componentName.flattenToString())
    } catch (e: Exception) {
        false
    }
}

data class AppInfo(val name: String, val packageName: String)

suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    try {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        
        resolveInfoList.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            if (!appName.startsWith("com.") && !appName.startsWith("android.") && !appName.startsWith("org.")) {
                AppInfo(appName, packageName)
            } else null
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    } catch (e: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesListScreen(
    viewModel: RulesListViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AlarmRule?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showSystemLogsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var hasNotificationAccess by remember { mutableStateOf(checkNotificationAccess(context)) }

    var isServiceEnabled by remember {
        mutableStateOf(
            try {
                val state = context.packageManager.getComponentEnabledSetting(ComponentName(context, InterceptorService::class.java))
                state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } catch (e: Exception) {
                true
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = checkNotificationAccess(context)
                try {
                    val state = context.packageManager.getComponentEnabledSetting(ComponentName(context, InterceptorService::class.java))
                    isServiceEnabled = state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } catch (e: Exception) { }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                title = { Text("NotiVib Radar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { showSystemLogsDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "System Status Logs")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showLogDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Intercept Log")
                }
                FloatingActionButton(
                    onClick = { 
                        editingRule = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            RadarStatusIndicator(isActive = hasNotificationAccess && isServiceEnabled)

            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isServiceEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isServiceEnabled) "App Background Service is Enabled" else "App is Forcefully Stopped",
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Toggle to permanently kill/revive the background listener process.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isServiceEnabled) Color.Gray else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Switch(
                        checked = isServiceEnabled,
                        onCheckedChange = { enable ->
                            isServiceEnabled = enable
                            try {
                                val state = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                context.packageManager.setComponentEnabledSetting(
                                    ComponentName(context, InterceptorService::class.java),
                                    state,
                                    PackageManager.DONT_KILL_APP
                                )
                                if (!enable) {
                                    Toast.makeText(context, "App forcefully stopped. It will not run in the background.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }

            if (!permissionGrantedState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    AnimatedVisibility(visible = true) {
                        RuleCard(
                            rule = rule, 
                            onDelete = { viewModel.deleteRule(it.id) },
                            onEdit = { 
                                editingRule = it
                                showAddDialog = true 
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRuleDialog(
                editingRule = editingRule,
                onDismiss = { showAddDialog = false },
                onSave = { id, pkg, kw, st, end, vibOnly ->
                    viewModel.saveRule(id, pkg, kw, st, end, vibOnly)
                    showAddDialog = false
                }
            )
        }

        if (showSystemLogsDialog) {
            AlertDialog(
                onDismissRequest = { showSystemLogsDialog = false },
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("System Logs")
                        TextButton(onClick = { viewModel.clearSystemLogs() }) {
                            Text("Clear All", color = Color.Red)
                        }
                    }
                },
                text = {
                    if (systemLogs.isEmpty()) {
                        Text("No system logs found.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                            items(systemLogs) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.deleteSystemLog(log) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                                HorizontalDivider(color = Color.DarkGray)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSystemLogsDialog = false }) { Text("Close") } }
            )
        }

        if (showLogDialog) {
            var expandedLogId by remember { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { showLogDialog = false },
                title = { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Interceptions")
                        TextButton(onClick = { viewModel.clearInterceptLogs() }) {
                            Text("Clear All", color = Color.Red)
                        }
                    }
                },
                text = {
                    if (logs.isEmpty()) {
                        Text("No notifications intercepted yet.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { log ->
                                val logId = log.time + log.packageName
                                val isExpanded = expandedLogId == logId
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        expandedLogId = if (isExpanded) null else logId
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (log.matchedRule != null) Color(0xFF1E3A1E) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.padding(8.dp).weight(1f).animateContentSize()) {
                                            Text("${log.time} - ${log.appName}", fontWeight = FontWeight.Bold)
                                            Text("Package: ${log.packageName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text("Title: ${log.title}", style = MaterialTheme.typography.bodySmall)
                                            
                                            if (isExpanded) {
                                                Text("Content: ${log.text}", style = MaterialTheme.typography.bodySmall)
                                            } else {
                                                Text("Content: ${log.text}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                            }
                                            
                                            if (log.matchedRule != null) {
                                                Text("ALARM TRIGGERED: ${log.matchedRule}", color = Color.Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteInterceptLog(log) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLogDialog = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun RadarStatusIndicator(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.5f else 1f,
        animationSpec = if (isActive) InfiniteRepeatableSpec(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ) else InfiniteRepeatableSpec(animation = tween(0)),
        label = "radar_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0f else 1f,
        animationSpec = if (isActive) InfiniteRepeatableSpec(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ) else InfiniteRepeatableSpec(animation = tween(0)),
        label = "radar_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(if (isActive) ElectricBlue.copy(alpha = alpha) else Color.Gray.copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Listening",
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun RuleCard(rule: AlarmRule, onDelete: (AlarmRule) -> Unit, onEdit: (AlarmRule) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(rule) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("App: ${rule.targetPackage.ifEmpty { "Any" }}", fontWeight = FontWeight.Bold)
                Text("Keywords: ${rule.keyword.ifEmpty { "Any" }}", color = Color.Gray)
                
                Text(
                    "Mode: ${if (rule.vibrationOnly) "Vibrate Only" else "Alarm + Vibrate"}",
                    color = if (rule.vibrationOnly) Color.Cyan else Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (rule.startTimeMinute == 0 && rule.endTimeMinute == 1440) {
                    Text("Time: 24/7 Active", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(
                        "Time: ${String.format("%02d:%02d", rule.startTimeMinute/60, rule.startTimeMinute%60)} - " +
                        "${String.format("%02d:%02d", rule.endTimeMinute/60, rule.endTimeMinute%60)}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { onDelete(rule) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    editingRule: AlarmRule?, 
    onDismiss: () -> Unit, 
    onSave: (String?, String, String, Int, Int, Boolean) -> Unit
) {
    var pkg by remember { mutableStateOf(editingRule?.targetPackage ?: "") }
    var kw by remember { mutableStateOf(editingRule?.keyword ?: "") }
    
    val initial247 = editingRule == null || (editingRule.startTimeMinute == 0 && editingRule.endTimeMinute == 1440)
    var is247 by remember { mutableStateOf(initial247) }
    
    var startHour by remember { mutableStateOf(if (editingRule != null) String.format("%02d", editingRule.startTimeMinute / 60) else "08") }
    var startMin by remember { mutableStateOf(if (editingRule != null) String.format("%02d", editingRule.startTimeMinute % 60) else "00") }
    var endHour by remember { mutableStateOf(if (editingRule != null) String.format("%02d", editingRule.endTimeMinute / 60) else "20") }
    var endMin by remember { mutableStateOf(if (editingRule != null) String.format("%02d", editingRule.endTimeMinute % 60) else "00") }
    
    var vibrationOnly by remember { mutableStateOf(editingRule?.vibrationOnly ?: false) }
    
    var showAppPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Select Installed App") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    items(installedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pkg = app.name
                                    showAppPicker = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(app.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppPicker = false }) { Text("Cancel") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingRule != null) "Edit Rule" else "Add New Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pkg, 
                        onValueChange = { pkg = it }, 
                        label = { Text("App Name (e.g. Messenger)") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showAppPicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick App", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                OutlinedTextField(
                    value = kw, 
                    onValueChange = { kw = it }, 
                    label = { Text("Keywords (comma separated)") }
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vibrationOnly, onCheckedChange = { vibrationOnly = it })
                    Text("Vibrate Only (Silent)")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = is247, onCheckedChange = { is247 = it })
                    Text("24/7 Active")
                }
                
                if (!is247) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startHour, onValueChange = { startHour = it }, label = { Text("Start HH") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = startMin, onValueChange = { startMin = it }, label = { Text("Start MM") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = endHour, onValueChange = { endHour = it }, label = { Text("End HH") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endMin, onValueChange = { endMin = it }, label = { Text("End MM") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val st = if (is247) 0 else (startHour.toIntOrNull() ?: 8) * 60 + (startMin.toIntOrNull() ?: 0)
                val et = if (is247) 1440 else (endHour.toIntOrNull() ?: 20) * 60 + (endMin.toIntOrNull() ?: 0)
                onSave(editingRule?.id, pkg.trim(), kw.trim(), st, et, vibrationOnly) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
