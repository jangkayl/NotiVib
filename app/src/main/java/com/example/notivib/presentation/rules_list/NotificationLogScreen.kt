package com.example.notivib.presentation.rules_list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.notivib.domain.repository.NotificationLog
import com.example.notivib.presentation.theme.SourceSerif4
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationLogScreen(
    viewModel: RulesListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val logs by viewModel.logs.collectAsState()
    var showTrackedAppsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var trackedApps by remember { mutableStateOf(com.example.notivib.framework.utils.EngineState.getTrackedApps(context)) }
    var searchLogQuery by remember { mutableStateOf("") }
    var displayedLogsCount by remember { mutableStateOf(20) }
    
    LaunchedEffect(searchLogQuery) {
        displayedLogsCount = 20
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(com.example.notivib.presentation.theme.icons.arrow_back_ios_new, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {

                    IconButton(onClick = { showTrackedAppsDialog = true }) {
                        Icon(com.example.notivib.presentation.theme.icons.list_alt_add, contentDescription = "Tracked Apps", tint = Color.White)
                    }
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearInterceptLogs() }) {
                            Icon(com.example.notivib.presentation.theme.icons.mop, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        val filteredLogs = remember(logs, searchLogQuery) {
            if (searchLogQuery.isBlank()) logs
            else logs.filter { 
                it.title.contains(searchLogQuery, true) || 
                it.text.contains(searchLogQuery, true) || 
                it.appName.contains(searchLogQuery, true) ||
                it.packageName.contains(searchLogQuery, true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchLogQuery,
                onValueChange = { searchLogQuery = it },
                label = { Text("Search notifications...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("No notifications recorded yet.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val visibleLogs = filteredLogs.take(displayedLogsCount)
                    itemsIndexed(visibleLogs, key = { index, log -> "${log.date}_${log.time}_${log.packageName}_${log.title}_$index" }) { index, log ->
                        LogItemCard(log = log, onDelete = { viewModel.deleteInterceptLog(log) })
                    }
                    
                    if (displayedLogsCount < filteredLogs.size) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = { displayedLogsCount += 20 },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9FF0B), contentColor = Color.Black),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Load More", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    if (showTrackedAppsDialog) {
        TrackedAppsDialog(
            initialTrackedApps = trackedApps,
            onDismiss = { showTrackedAppsDialog = false },
            onSave = { selectedApps ->
                trackedApps = selectedApps
                com.example.notivib.framework.utils.EngineState.setTrackedApps(context, selectedApps)
                showTrackedAppsDialog = false
            }
        )
    }
}

@Composable
fun TrackedAppsDialog(
    initialTrackedApps: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApps by remember { mutableStateOf(initialTrackedApps) }
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                Text(
                    text = "Tracked Apps",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Select apps to record in your history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                val filteredApps = remember(installedApps, searchQuery) {
                    installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        .sortedWith(
                            compareByDescending<AppInfo> { initialTrackedApps.contains(it.packageName) }
                                .thenBy { it.name.lowercase() }
                        )
                }
                val isAllSelected = selectedApps.contains("ALL_APPS")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedApps = if (isAllSelected) selectedApps - "ALL_APPS" else selectedApps + "ALL_APPS"
                        }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Track All Applications",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Switch(
                            checked = isAllSelected, 
                            onCheckedChange = { selectedApps = if (it) selectedApps + "ALL_APPS" else selectedApps - "ALL_APPS" },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedApps.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedApps = if (isSelected) {
                                        selectedApps - app.packageName
                                    } else {
                                        selectedApps + app.packageName
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(packageName = app.packageName, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedApps) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun LogItemCard(log: NotificationLog, onDelete: () -> Unit) {
    var showActions by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (log.matchedRule != null) Color(0xFF2E3D20) else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg_color"
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable { showActions = !showActions },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (showActions) 8.dp else 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIconImage(packageName = log.packageName, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(log.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Text(log.time, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(8.dp))
            Text(log.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(log.text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Text("Package: ${log.packageName}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            if (log.matchedRule != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TRIGGERED BY: ${log.matchedRule}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (showActions) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private object AppIconCache {
    private val cache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(100)

    fun get(packageName: String): androidx.compose.ui.graphics.ImageBitmap? = cache.get(packageName)
    fun put(packageName: String, bitmap: androidx.compose.ui.graphics.ImageBitmap) = cache.put(packageName, bitmap)
}

@Composable
fun AppIconImage(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf(AppIconCache.get(packageName)) }
    
    LaunchedEffect(packageName) {
        if (bitmap == null && packageName.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val d = context.packageManager.getApplicationIcon(packageName)
                    val width = if (d.intrinsicWidth > 0) d.intrinsicWidth else 100
                    val height = if (d.intrinsicHeight > 0) d.intrinsicHeight else 100
                    val b = d.toBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val imgBitmap = b.asImageBitmap()
                    AppIconCache.put(packageName, imgBitmap)
                    bitmap = imgBitmap
                } catch (e: Exception) {}
            }
        }
    }
    
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        androidx.compose.foundation.layout.Box(modifier = modifier)
    }
}

@Preview
@Composable
fun LogItemCardPreview() {
    MaterialTheme {
        LogItemCard(
            log = NotificationLog(
                date = "2026-07-11",
                time = "10:30:15",
                appName = "WhatsApp",
                packageName = "com.whatsapp",
                title = "Emergency Meeting",
                text = "We need to talk right now.",
                matchedRule = "Emergency"
            ),
            onDelete = {}
        )
    }
}