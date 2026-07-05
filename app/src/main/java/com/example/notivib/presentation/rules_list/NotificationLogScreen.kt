package com.example.notivib.presentation.rules_list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.notivib.domain.repository.NotificationLog

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTrackedAppsDialog = true }) {
                        Icon(Icons.Default.FactCheck, contentDescription = "Tracked Apps", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearInterceptLogs() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text("No interceptions recorded yet.", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
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
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLogs) { log ->
                        LogItemCard(log = log, onDelete = { viewModel.deleteInterceptLog(log) })
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
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                Text(
                    text = "Tracked Apps",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Select apps to record in your history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Track All Applications",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                HorizontalDivider()
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
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
        targetValue = if (log.matchedRule != null) Color(0xFF1E3A1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "bg_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable { showActions = !showActions },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (showActions) 8.dp else 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIconImage(packageName = log.packageName, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(log.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(log.time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Spacer(Modifier.height(8.dp))
            Text(log.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(4.dp))
            Text(log.text, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text("Package: ${log.packageName}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            
            if (log.matchedRule != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TRIGGERED BY: ${log.matchedRule}", color = Color.Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            
            if (showActions) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconImage(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var drawable by remember(packageName) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    LaunchedEffect(packageName) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val d = context.packageManager.getApplicationIcon(packageName)
                drawable = d
            } catch (e: Exception) {}
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { imageView ->
            if (drawable != null) {
                imageView.setImageDrawable(drawable)
            } else {
                imageView.setImageResource(android.R.color.transparent)
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
fun LogItemCardPreview() {
    MaterialTheme {
        LogItemCard(
            log = NotificationLog(
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
