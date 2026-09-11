package com.example.notivib.presentation.rules_list

import android.Manifest

import android.content.ComponentName

import android.content.Context

import android.content.Intent

import android.content.pm.PackageManager

import android.os.Build

import android.provider.Settings

import android.service.notification.NotificationListenerService

import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.animateContentSize

import androidx.compose.animation.core.*

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.outlined.*

import androidx.compose.material.icons.automirrored.outlined.*

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.material3.TabRow

import androidx.compose.material3.Tab

import androidx.compose.material3.TabRowDefaults

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.ui.unit.sp

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate

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

import com.example.notivib.framework.utils.BatteryOptimizationHelper

import com.example.notivib.presentation.theme.SourceSerif4
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
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditRule: (AlarmRule?) -> Unit = {}
) {
    val rules by viewModel.rules.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    val connectionLogs by viewModel.connectionLogs.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val activeRules = rules.filter { it.isActive }
    val inactiveRules = rules.filter { !it.isActive }
    val currentRulesList = if (selectedTabIndex == 0) activeRules else inactiveRules

    var showSystemLogsDialog by remember { mutableStateOf(false) }

    var showStatusCards by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var hasNotificationAccess by remember { mutableStateOf(checkNotificationAccess(context)) }

    var isServiceEnabled by remember {

        mutableStateOf(com.example.notivib.framework.utils.EngineState.isGloballyEnabled(context))

    }

    var isIgnoringBatteryOptimizations by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                hasNotificationAccess = checkNotificationAccess(context)

                isIgnoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

                isServiceEnabled = com.example.notivib.framework.utils.EngineState.isGloballyEnabled(context)

                if (hasNotificationAccess && isServiceEnabled && !InterceptorService.isConnected) {

                    try {

                        val componentName = ComponentName(context, InterceptorService::class.java)

                        NotificationListenerService.requestRebind(componentName)

                    } catch (e: Exception) {}

                }

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
                        val isEngineActive = hasNotificationAccess && isServiceEnabled
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = if (isEngineActive) com.example.notivib.R.drawable.engine_active_indicator 
                                else com.example.notivib.R.drawable.engine_inactive_indicator
                            ),
                            contentDescription = "Engine Status",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("NotiVib", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { showSystemLogsDialog = true }) {
                        Icon(com.example.notivib.presentation.theme.icons.ecg_heart, contentDescription = "Engine Diagnostics", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            var showFabMenu by remember { mutableStateOf(false) }
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            
            Box(contentAlignment = Alignment.BottomEnd) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showFabMenu,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 })
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 80.dp)) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onNavigateToEditRule(null)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            icon = { Icon(Icons.Outlined.Edit, contentDescription = "Manual") },
                            text = { Text("Create Manually", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                val clipboardText = clipboardManager.getText()?.text
                                if (!clipboardText.isNullOrBlank()) {
                                    try {
                                        val obj = org.json.JSONObject(clipboardText)
                                        val newRule = com.example.notivib.domain.model.AlarmRule(
                                            id = java.util.UUID.randomUUID().toString(),
                                            ruleName = obj.optString("ruleName", ""),
                                            targetPackage = obj.getString("targetPackage"),
                                            keyword = obj.getString("keyword"),
                                            startTimeMinute = obj.getInt("startTimeMinute"),
                                            endTimeMinute = obj.getInt("endTimeMinute"),
                                            vibrationOnly = obj.optBoolean("vibrationOnly", false),
                                            isActive = obj.optBoolean("isActive", true),
                                            activeDays = obj.optJSONArray("activeDays")?.let { arr ->
                                                val days = mutableSetOf<Int>()
                                                for (j in 0 until arr.length()) days.add(arr.getInt(j))
                                                days
                                            } ?: setOf(1, 2, 3, 4, 5, 6, 7),
                                            muteOutsideSchedule = obj.optBoolean("muteOutsideSchedule", false),
                                            remindSchedule = obj.optBoolean("remindSchedule", false),
                                            ignoredKeywords = obj.optString("ignoredKeywords", ""),
                                            hasCustomTimeWindows = obj.optBoolean("hasCustomTimeWindows", false),
                                            customTimeWindows = obj.optJSONObject("customTimeWindows")?.let { customWindowsObj ->
                                                val map = mutableMapOf<Int, com.example.notivib.domain.model.TimeWindow>()
                                                val keys = customWindowsObj.keys()
                                                while (keys.hasNext()) {
                                                    val key = keys.next()
                                                    val windowObj = customWindowsObj.getJSONObject(key)
                                                    map[key.toInt()] = com.example.notivib.domain.model.TimeWindow(
                                                        startTimeMinute = windowObj.getInt("startTimeMinute"),
                                                        endTimeMinute = windowObj.getInt("endTimeMinute")
                                                    )
                                                }
                                                map
                                            } ?: emptyMap()
                                        )
                                        onNavigateToEditRule(newRule)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Invalid rule JSON on clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste") },
                            text = { Text("Paste Copied Rule", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    val rotation by androidx.compose.animation.core.animateFloatAsState(targetValue = if (showFabMenu) 45f else 0f)
                    Icon(
                        Icons.Filled.Add, 
                        contentDescription = "Add Rule", 
                        modifier = Modifier.size(36.dp).rotate(rotation)
                    )
                }
            }
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Custom Segmented Control matching UI/UX design mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF2B2B28), RoundedCornerShape(28.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { selectedTabIndex = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Active",
                        fontFamily = SourceSerif4,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { selectedTabIndex = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Inactive",
                        fontFamily = SourceSerif4,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            if (currentRulesList.isEmpty()) {

                Box(

                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = 100.dp),

                    contentAlignment = Alignment.Center

                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))

                        Spacer(Modifier.height(16.dp))

                        Text(

                            "No Interception Rules",

                            style = MaterialTheme.typography.titleLarge,

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.onSurface

                        )

                        Spacer(Modifier.height(8.dp))

                        Text(

                            "Tap the 'New Rule' button below to start monitoring your notifications for critical keywords.",

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant,

                            textAlign = androidx.compose.ui.text.style.TextAlign.Center

                        )

                    }

                }

            } else {

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),

                    verticalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    items(currentRulesList, key = { it.id }) { rule ->

                    AnimatedVisibility(visible = true) {

                        RuleCard(

                            rule = rule, 

                            onDelete = { viewModel.deleteRule(it.id) },

                            onEdit = { 
                                onNavigateToEditRule(it)
                            },

                            onToggleActive = { isActive -> 

                                viewModel.toggleRuleActive(rule, isActive)

                                if (!isActive) {

                                    android.widget.Toast.makeText(context, "Rule moved to Inactive", android.widget.Toast.LENGTH_SHORT).show()

                                }

                            }

                        )

                    }

                }

            }

        }

        }

        if (showSystemLogsDialog) {

            Dialog(onDismissRequest = { showSystemLogsDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {

                Card(

                    modifier = Modifier.fillMaxWidth().padding(24.dp).heightIn(max = 600.dp),

                    shape = RoundedCornerShape(24.dp),

                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),

                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)

                ) {

                    Column(modifier = Modifier.padding(24.dp)) {

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                            Text("Engine Diagnostics", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)

                            IconButton(onClick = { viewModel.clearSystemLogs(); viewModel.clearConnectionLogs() }) {
                                Icon(Icons.Outlined.CleaningServices, contentDescription = "Clear All", tint = Color.Red)
                            }

                        }

                        Spacer(Modifier.height(16.dp))

                        // Merge both buffers for display (they stay separate underneath so connection
                        // churn never evicts real errors). isConnection=true tags the green rows;
                        // "[Engine Error]" tags the red interruption rows; everything else is neutral.
                        val mergedLogs = remember(systemLogs, connectionLogs) {
                            (systemLogs.map { it to false } + connectionLogs.map { it to true })
                                .sortedByDescending { it.first }
                        }

                        if (mergedLogs.isEmpty()) {

                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {

                                Text("All systems nominal.", color = Color.White)

                            }

                        } else {

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                items(mergedLogs) { (log, isConnection) ->

                                    val isError = !isConnection && log.contains("[Engine Error]")
                                    val rowBg = when {
                                        isConnection -> Color(0xFFD9FF0B).copy(alpha = 0.15f)
                                        isError -> Color(0xFFFF5252).copy(alpha = 0.20f)
                                        else -> Color.White.copy(alpha = 0.04f)
                                    }

                                    Row(

                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(rowBg)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text(log, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))

                                        IconButton(
                                            onClick = { if (isConnection) viewModel.deleteConnectionLog(log) else viewModel.deleteSystemLog(log) },
                                            modifier = Modifier.size(24.dp)
                                        ) {

                                            Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))

                                        }

                                    }

                                }

                            }

                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (com.example.notivib.framework.utils.EngineState.isShowForegroundNotification(context)) {
                                    // Foreground notification is enabled: let the service rebind, redraw the
                                    // healthy banner, and log the restart diagnostic itself.
                                    val restartIntent = Intent(context, com.example.notivib.framework.service.EngineForegroundService::class.java).apply {
                                        action = com.example.notivib.framework.service.EngineForegroundService.ACTION_RESTART
                                    }
                                    context.startForegroundService(restartIntent)
                                } else {
                                    // User disabled the persistent notification: rebind + log directly without
                                    // forcing a foreground-service banner they opted out of.
                                    try {
                                        NotificationListenerService.requestRebind(
                                            ComponentName(context, com.example.notivib.framework.service.InterceptorService::class.java)
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    viewModel.logEngineRestart()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White)
                        ) {

                            Text("Restart Engine", fontWeight = FontWeight.Bold)

                        }

                        Spacer(Modifier.height(6.dp))

                        Button(onClick = { showSystemLogsDialog = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {

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

        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),

        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f))

    ) {

        Row(

            modifier = Modifier.padding(24.dp).fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {

                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        id = if (isActive) com.example.notivib.R.drawable.engine_active_indicator 
                        else com.example.notivib.R.drawable.engine_inactive_indicator
                    ),
                    contentDescription = if (isActive) "Engine Active" else "Engine Suspended",
                    modifier = Modifier.size(16.dp) // Adjusted size for better visibility of the detailed icon
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

                ),

                modifier = Modifier.scale(0.8f)

            )

        }

    }

}

@Composable
fun RuleCard(rule: AlarmRule, onDelete: (AlarmRule) -> Unit, onEdit: (AlarmRule) -> Unit, onToggleActive: (Boolean) -> Unit) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val cardBg = if (rule.isActive) Color(0xFFD9EA7D) else Color(0xFF444444)
    val textColor = if (rule.isActive) Color(0xFF20201E) else Color.White
    val subTextColor = if (rule.isActive) Color(0xFF444444) else Color.White.copy(alpha = 0.7f)

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF20201E),
            title = { Text("Delete Rule?", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Are you sure you want to delete this rule?\nThis action cannot be undone.", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(rule)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Delete", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9EA7D), contentColor = Color(0xFF20201E)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Cancel", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    var friendlyAppName by remember(rule.targetPackage) { mutableStateOf(if (rule.targetPackage == "ANY") "All Applications" else rule.targetPackage) }

    LaunchedEffect(rule.targetPackage) {
        if (rule.targetPackage != "ANY") {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(rule.targetPackage, 0)
                friendlyAppName = pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {}
        }
    }

    val displayTitle = if (rule.ruleName.isNotBlank()) rule.ruleName else friendlyAppName

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (rule.targetPackage == "ANY") {
                        Box(
                            modifier = Modifier.size(44.dp).background(Color(0xFF20201E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Apps, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        AppIconImage(packageName = rule.targetPackage, modifier = Modifier.size(44.dp))
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            text = displayTitle,
                            fontFamily = SourceSerif4,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            maxLines = 1
                        )
                        Text(
                            text = if (rule.targetPackage == "ANY") "all.apps" else rule.targetPackage,
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Menu", tint = textColor)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF20201E))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Rule", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onEdit(rule)
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Rule", color = Color.White) },
                            onClick = {
                                showMenu = false
                                val json = org.json.JSONObject().apply {
                                    put("ruleName", rule.ruleName)
                                    put("targetPackage", rule.targetPackage)
                                    put("keyword", rule.keyword)
                                    put("startTimeMinute", rule.startTimeMinute)
                                    put("endTimeMinute", rule.endTimeMinute)
                                    put("vibrationOnly", rule.vibrationOnly)
                                    put("isActive", rule.isActive)
                                    put("muteOutsideSchedule", rule.muteOutsideSchedule)
                                    put("remindSchedule", rule.remindSchedule)
                                    put("ignoredKeywords", rule.ignoredKeywords)
                                    put("activeDays", org.json.JSONArray(rule.activeDays))
                                    put("hasCustomTimeWindows", rule.hasCustomTimeWindows)
                                    val customWindowsObj = org.json.JSONObject()
                                    rule.customTimeWindows.forEach { (day, window) ->
                                        val windowObj = org.json.JSONObject()
                                        windowObj.put("startTimeMinute", window.startTimeMinute)
                                        windowObj.put("endTimeMinute", window.endTimeMinute)
                                        customWindowsObj.put(day.toString(), windowObj)
                                    }
                                    put("customTimeWindows", customWindowsObj)
                                }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(json.toString(4)))
                                android.widget.Toast.makeText(context, "Rule copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Rule", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Keywords", style = MaterialTheme.typography.labelSmall, color = subTextColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val keywordsList = com.example.notivib.domain.model.parseKeywords(rule.keyword)
            if (keywordsList.isEmpty()) {
                Text(
                    text = "Any Keyword",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            } else {
                val displayLimit = 3
                val visibleKeywords = keywordsList.take(displayLimit)
                val overflowCount = keywordsList.size - visibleKeywords.size

                com.example.notivib.presentation.components.SimpleFlowRow(
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 6.dp
                ) {
                    visibleKeywords.forEach { kw ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF333333),
                            contentColor = Color(0xFFD9FF0B)
                        ) {
                            Text(
                                text = kw,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (overflowCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF333333),
                            contentColor = Color(0xFFD9FF0B)
                        ) {
                            Text(
                                text = "+$overflowCount more",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Days", style = MaterialTheme.typography.labelSmall, color = subTextColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    val daysMap = mapOf(1 to "M", 2 to "T", 3 to "W", 4 to "Th", 5 to "F", 6 to "S", 7 to "Su")
                    val activeDaysString = if (rule.activeDays.size == 7) "Everyday" 
                        else rule.activeDays.sorted().joinToString(" • ") { daysMap[it] ?: "" }
                    Text(
                        text = activeDaysString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { onToggleActive(!rule.isActive) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20201E),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (rule.isActive) "Active" else "Activate",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


