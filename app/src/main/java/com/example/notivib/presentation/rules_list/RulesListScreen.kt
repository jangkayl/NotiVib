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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notivib.domain.model.AlarmRule
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

fun formatTime(minutes: Int): String {
    return String.format("%02d:%02d", minutes / 60, minutes % 60)
}

data class AppInfo(val name: String, val packageName: String)

object AppListCache {
    var cachedApps: List<AppInfo>? = null
}

suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    if (AppListCache.cachedApps != null) {
        return@withContext AppListCache.cachedApps!!
    }

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
        
        val apps = resolveInfoList.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            if (!appName.startsWith("com.") && !appName.startsWith("android.") && !appName.startsWith("org.")) {
                AppInfo(appName, packageName)
            } else null
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
        
        AppListCache.cachedApps = apps
        apps
    } catch (e: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesListScreen(
    viewModel: RulesListViewModel = hiltViewModel(),
    onNavigateToLogs: () -> Unit = {}
) {
    val rules by viewModel.rules.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AlarmRule?>(null) }
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
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("NotiVib", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { showSystemLogsDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "System Status Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onNavigateToLogs,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Intercept Log")
                }
                ExtendedFloatingActionButton(
                    onClick = { 
                        editingRule = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Rule") },
                    text = { Text("New Rule", fontWeight = FontWeight.Bold) }
                )
            }
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
                    try {
                        val state = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        context.packageManager.setComponentEnabledSetting(
                            ComponentName(context, InterceptorService::class.java),
                            state,
                            PackageManager.DONT_KILL_APP
                        )
                        if (!enable) {
                            Toast.makeText(context, "Engine suspended.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )

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
            Dialog(onDismissRequest = { showSystemLogsDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(24.dp).heightIn(max = 600.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Engine Diagnostics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            TextButton(onClick = { viewModel.clearSystemLogs() }) {
                                Text("Clear All", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        if (systemLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("All systems nominal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                items(systemLogs) { log ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(log, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.deleteSystemLog(log) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { showSystemLogsDialog = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun RuleCard(rule: AlarmRule, onDelete: (AlarmRule) -> Unit, onEdit: (AlarmRule) -> Unit) {
    val context = LocalContext.current
    var friendlyAppName by remember(rule.targetPackage) { mutableStateOf(if (rule.targetPackage == "ANY") "All Applications" else rule.targetPackage) }

    LaunchedEffect(rule.targetPackage) {
        if (rule.targetPackage != "ANY") {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(rule.targetPackage, 0)
                friendlyAppName = pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                // Keep the package name as fallback
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (rule.targetPackage == "ANY") {
                        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        AppIconImage(packageName = rule.targetPackage, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = friendlyAppName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (rule.targetPackage != "ANY") {
                            Text(
                                text = rule.targetPackage,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = { onEdit(rule) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { onDelete(rule) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Text("MATCHING KEYWORDS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = rule.keyword.replace(",", " • "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${formatTime(rule.startTimeMinute)} - ${formatTime(rule.endTimeMinute)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (rule.vibrationOnly) Icons.Default.Vibration else Icons.Default.NotificationsActive, 
                        contentDescription = null, 
                        tint = if (rule.vibrationOnly) Color.Cyan.copy(alpha = 0.8f) else Color.Magenta.copy(alpha = 0.8f), 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (rule.vibrationOnly) "Vibrate Only" else "Alarm & Vibrate",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rule.vibrationOnly) Color.Cyan.copy(alpha = 0.8f) else Color.Magenta.copy(alpha = 0.8f)
                    )
                }
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
    val context = LocalContext.current
    var keyword by remember { mutableStateOf(editingRule?.keyword ?: "") }
    var targetPackage by remember { mutableStateOf(editingRule?.targetPackage ?: "ANY") }
    
    var appName by remember { 
        mutableStateOf(
            if (editingRule == null || editingRule.targetPackage == "ANY") "ANY APP"
            else {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(editingRule.targetPackage, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    editingRule.targetPackage
                }
            }
        )
    }
    
    var startTimeMinute by remember { mutableStateOf(editingRule?.startTimeMinute ?: 0) }
    var endTimeMinute by remember { mutableStateOf(editingRule?.endTimeMinute ?: 1439) }
    var vibrationOnly by remember { mutableStateOf(editingRule?.vibrationOnly ?: false) }

    var expanded by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (editingRule == null) "Create New Rule" else "Edit Rule",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Configure how the app intercepts and alerts you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(Modifier.height(28.dp))
                
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Trigger Keywords (Comma separated)") },
                    placeholder = { Text("e.g. URGENT, Boss, Emergency") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
                
                Spacer(Modifier.height(16.dp))

                Text("Target Application", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(4.dp))
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (targetPackage == "ANY") {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ALL APPLICATIONS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            AppIconImage(packageName = targetPackage, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(appName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(targetPackage, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                    }
                }
                
                if (expanded) {
                    Dialog(onDismissRequest = { expanded = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Select Target Application", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                targetPackage = "ANY"
                                                appName = "ANY APP"
                                                expanded = false
                                            }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.width(16.dp))
                                            Text("ALL APPLICATIONS", fontWeight = FontWeight.ExtraBold)
                                        }
                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                    }
                                    items(installedApps) { app ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                targetPackage = app.packageName
                                                appName = app.name
                                                expanded = false
                                            }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AppIconImage(packageName = app.packageName, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.width(16.dp))
                                            Column {
                                                Text(app.name, fontWeight = FontWeight.SemiBold)
                                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Time Window", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Slider(
                            value = startTimeMinute.toFloat(),
                            onValueChange = { startTimeMinute = it.toInt() },
                            valueRange = 0f..1439f,
                            steps = 95
                        )
                        Text(formatTime(startTimeMinute), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Slider(
                            value = endTimeMinute.toFloat(),
                            onValueChange = { endTimeMinute = it.toInt() },
                            valueRange = 0f..1439f,
                            steps = 95
                        )
                        Text(formatTime(endTimeMinute), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vibrationOnly = !vibrationOnly }.padding(vertical = 8.dp)) {
                    Switch(checked = vibrationOnly, onCheckedChange = { vibrationOnly = it })
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Vibration Only Mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("If enabled, audio alarms will not play.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(editingRule?.id, targetPackage, keyword, startTimeMinute, endTimeMinute, vibrationOnly) },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) { 
                        Text("Save Rule", fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EngineStatusCardPreview() {
    MaterialTheme {
        EngineStatusCard(isActive = true, onToggle = {})
    }
}

@Preview(showBackground = true)
@Composable
fun RuleCardPreview() {
    MaterialTheme {
        RuleCard(
            rule = AlarmRule(
                id = "1",
                targetPackage = "Messenger",
                keyword = "Emergency",
                startTimeMinute = 480,
                endTimeMinute = 1200,
                vibrationOnly = false
            ),
            onDelete = {},
            onEdit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddRuleDialogPreview() {
    MaterialTheme {
        AddRuleDialog(
            editingRule = null,
            onDismiss = {},
            onSave = { _, _, _, _, _, _ -> }
        )
    }
}
