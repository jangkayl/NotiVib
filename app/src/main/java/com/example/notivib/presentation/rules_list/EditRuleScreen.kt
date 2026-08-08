package com.example.notivib.presentation.rules_list

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notivib.domain.model.AlarmRule
import com.example.notivib.presentation.theme.SourceSerif4
import com.example.notivib.presentation.theme.HostGrotesk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleScreen(
    rule: AlarmRule?,
    onNavigateBack: () -> Unit,
    viewModel: RulesListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var ruleName by remember { mutableStateOf(rule?.ruleName ?: "") }
    val keywordChips = remember {
        mutableStateListOf<String>().apply {
            addAll(com.example.notivib.domain.model.parseKeywords(rule?.keyword ?: ""))
        }
    }
    var targetPackage by remember { mutableStateOf(rule?.targetPackage ?: "ANY") }
    var activeDays by remember { mutableStateOf(rule?.activeDays ?: setOf(1, 2, 3, 4, 5, 6, 7)) }

    var vibrationOnly by remember { mutableStateOf(rule?.vibrationOnly ?: false) }
    var muteOutsideSchedule by remember { mutableStateOf(rule?.muteOutsideSchedule ?: false) }
    var remindSchedule by remember { mutableStateOf(rule?.remindSchedule ?: false) }
    val ignoredKeywordChips = remember {
        mutableStateListOf<String>().apply {
            val triggerParsed = com.example.notivib.domain.model.parseKeywords(rule?.keyword ?: "").map { it.lowercase() }
            val ignoredParsed = com.example.notivib.domain.model.parseKeywords(rule?.ignoredKeywords ?: "")
                .filter { !triggerParsed.contains(it.lowercase()) }
            addAll(ignoredParsed)
        }
    }

    var triggerInputText by remember { mutableStateOf("") }
    var ignoredInputText by remember { mutableStateOf("") }

    var hasCustomTimeWindows by remember { mutableStateOf(rule?.hasCustomTimeWindows ?: false) }
    var customTimeWindows by remember {
        mutableStateOf(rule?.customTimeWindows ?: emptyMap<Int, com.example.notivib.domain.model.TimeWindow>())
    }
    var startTimeMinute by remember { mutableStateOf(rule?.startTimeMinute ?: 0) }
    var endTimeMinute by remember { mutableStateOf(rule?.endTimeMinute ?: 1439) }

    val outsideScheduleMinutes = remember(activeDays, hasCustomTimeWindows, startTimeMinute, endTimeMinute, customTimeWindows) {
        calculateOutsideScheduleMinutes(activeDays, hasCustomTimeWindows, startTimeMinute, endTimeMinute, customTimeWindows)
    }
    val hasOutsideSchedule = outsideScheduleMinutes >= 30

    LaunchedEffect(hasOutsideSchedule) {
        if (!hasOutsideSchedule) {
            muteOutsideSchedule = false
        }
    }

    var appName by remember { 
        mutableStateOf(
            if (rule == null || rule.targetPackage == "ANY") "Any App"
            else {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(rule.targetPackage, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    rule.targetPackage
                }
            }
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)
    }

    // Colors matching the design
    val darkSurface = Color(0xFF5B5B5B)
    val accentColor = Color(0xFFD9FF0B)
    val textColor = Color(0xFFE0E0E0)

    val initialKeywordParsed = remember(rule) { com.example.notivib.domain.model.parseKeywords(rule?.keyword ?: "") }
    val initialIgnoredParsed = remember(rule) { com.example.notivib.domain.model.parseKeywords(rule?.ignoredKeywords ?: "") }
    val currentKeywordString = keywordChips.joinToString("|||")
    val currentIgnoredString = ignoredKeywordChips.joinToString("|||")

    val hasUnsavedChanges = remember(
        ruleName, currentKeywordString, targetPackage, activeDays, vibrationOnly, muteOutsideSchedule, 
        remindSchedule, hasCustomTimeWindows, customTimeWindows, startTimeMinute, endTimeMinute, currentIgnoredString,
        triggerInputText, ignoredInputText
    ) {
        triggerInputText.trim().isNotEmpty() ||
        ignoredInputText.trim().isNotEmpty() ||
        ruleName != (rule?.ruleName ?: "") ||
        keywordChips.toList() != initialKeywordParsed ||
        targetPackage != (rule?.targetPackage ?: "ANY") ||
        activeDays != (rule?.activeDays ?: setOf(1, 2, 3, 4, 5, 6, 7)) ||
        vibrationOnly != (rule?.vibrationOnly ?: false) ||
        muteOutsideSchedule != (rule?.muteOutsideSchedule ?: false) ||
        remindSchedule != (rule?.remindSchedule ?: false) ||
        ignoredKeywordChips.toList() != initialIgnoredParsed ||
        hasCustomTimeWindows != (rule?.hasCustomTimeWindows ?: false) ||
        customTimeWindows != (rule?.customTimeWindows ?: emptyMap<Int, com.example.notivib.domain.model.TimeWindow>()) ||
        startTimeMinute != (rule?.startTimeMinute ?: 0) ||
        endTimeMinute != (rule?.endTimeMinute ?: 1439)
    }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    androidx.activity.compose.BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedChangesDialog = true
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                        if (rule != null) {
                            viewModel.deleteRule(rule.id)
                            onNavigateBack()
                        }
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
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color(0xFF20201E)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Cancel", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            containerColor = Color(0xFF20201E),
            title = { Text("Unsaved Changes", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedChangesDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Discard", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showUnsavedChangesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color(0xFF20201E)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Keep Editing", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(com.example.notivib.presentation.theme.icons.arrow_back_ios_new, contentDescription = "Back", tint = Color.White)
                    }
                },
                title = { 
                    Text(
                        if (rule != null) "Edit Rule" else "New Rule", 
                        fontFamily = SourceSerif4, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    ) 
                },
                actions = {
                    if (rule != null) {
                        IconButton(onClick = { 
                            showDeleteConfirmDialog = true
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete Rule", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = handleBack,
                    modifier = Modifier.padding(end = 24.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White,
                        fontFamily = SourceSerif4,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Button(
                    onClick = {
                        if (triggerInputText.trim().isNotEmpty()) {
                            val ok = tryAddKeyword(triggerInputText, keywordChips, ignoredKeywordChips, "Trigger Keywords", "Ignored Keywords", context) {
                                triggerInputText = ""
                            }
                            if (!ok) return@Button
                        }

                        if (ignoredInputText.trim().isNotEmpty()) {
                            val ok = tryAddKeyword(ignoredInputText, ignoredKeywordChips, keywordChips, "Ignored Keywords", "Trigger Keywords", context) {
                                ignoredInputText = ""
                            }
                            if (!ok) return@Button
                        }

                        if (keywordChips.isEmpty()) {
                            android.widget.Toast.makeText(context, "Please add at least one trigger keyword", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val existingRules = viewModel.rules.value
                        val isDuplicateName = existingRules.any { it.ruleName.equals(ruleName, ignoreCase = true) && it.ruleName.isNotEmpty() && it.id != rule?.id }
                        
                        if (isDuplicateName) {
                            android.widget.Toast.makeText(context, "A rule with this name already exists", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val isExactDuplicate = existingRules.any { 
                            it.id != rule?.id &&
                            it.targetPackage == targetPackage &&
                            it.keyword == keywordChips.joinToString("|||") &&
                            it.ignoredKeywords == ignoredKeywordChips.joinToString("|||") &&
                            it.activeDays == activeDays &&
                            it.startTimeMinute == startTimeMinute &&
                            it.endTimeMinute == endTimeMinute &&
                            it.vibrationOnly == vibrationOnly &&
                            it.muteOutsideSchedule == muteOutsideSchedule &&
                            it.remindSchedule == remindSchedule &&
                            it.hasCustomTimeWindows == hasCustomTimeWindows &&
                            it.customTimeWindows == customTimeWindows
                        }
                        
                        if (isExactDuplicate) {
                            android.widget.Toast.makeText(context, "An identical rule already exists for this app", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.saveRule(
                            id = rule?.id,
                            ruleName = ruleName,
                            targetPackage = targetPackage,
                            keyword = keywordChips.joinToString("|||"),
                            startTimeMinute = startTimeMinute,
                            endTimeMinute = endTimeMinute,
                            vibrationOnly = vibrationOnly,
                            isActive = rule?.isActive ?: true,
                            activeDays = activeDays,
                            hasCustomTimeWindows = hasCustomTimeWindows,
                            customTimeWindows = customTimeWindows,
                            muteOutsideSchedule = muteOutsideSchedule,
                            remindSchedule = remindSchedule,
                            ignoredKeywords = ignoredKeywordChips.joinToString("|||")
                        )
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(140.dp)
                        .height(50.dp)
                ) {
                    Text("Save Rule", fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Configure interception parameters for this rule.",
                fontFamily = HostGrotesk,
                color = textColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text("Rule Name", color = Color.White, fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ruleName,
                onValueChange = { if (it.length <= 50) ruleName = it },
                placeholder = { Text("e.g. Work Rule", color = Color.Gray, fontFamily = HostGrotesk) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = darkSurface,
                    unfocusedContainerColor = darkSurface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = accentColor,
                    unfocusedTextColor = accentColor
                )
            )
            
            Spacer(Modifier.height(24.dp))

            KeywordChipInputGroup(
                title = "Trigger Keywords",
                placeholder = "Type keyword & tap +",
                textInput = triggerInputText,
                onTextInputChange = { triggerInputText = it },
                keywords = keywordChips,
                otherKeywords = ignoredKeywordChips,
                otherTitle = "Ignored Keywords",
                darkSurface = darkSurface,
                accentColor = accentColor,
                textColor = textColor
            )

            Spacer(Modifier.height(24.dp))

            KeywordChipInputGroup(
                title = "Ignored Keywords",
                placeholder = "Type keyword & tap +",
                helperText = "Notifications containing these words will be skipped even if they match trigger keywords.",
                textInput = ignoredInputText,
                onTextInputChange = { ignoredInputText = it },
                keywords = ignoredKeywordChips,
                otherKeywords = keywordChips,
                otherTitle = "Trigger Keywords",
                darkSurface = darkSurface,
                accentColor = accentColor,
                textColor = textColor
            )

            Spacer(Modifier.height(24.dp))

            Text("Target Application", color = Color.White, fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = darkSurface),
                modifier = Modifier.fillMaxWidth().clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (targetPackage == "ANY") {
                        Icon(Icons.Outlined.Apps, contentDescription = null, tint = accentColor, modifier = Modifier.size(48.dp))
                    } else {
                        AppIconImage(packageName = targetPackage, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(appName, fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 18.sp)
                        Text(if (targetPackage == "ANY") "ALL APPLICATIONS" else targetPackage, fontFamily = HostGrotesk, color = Color.White, fontSize = 14.sp)
                    }
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color.White)
                }
            }

            if (expanded) {
                Dialog(onDismissRequest = { expanded = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = darkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Select Target Application", color = Color.White, fontFamily = SourceSerif4, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search apps...", color = Color.LightGray) },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = accentColor) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF404040),
                                    unfocusedContainerColor = Color(0xFF404040),
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            targetPackage = "ANY"
                                            appName = "Any App"
                                            expanded = false
                                        }.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Apps, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Text("ALL APPLICATIONS", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    }
                                    HorizontalDivider(color = Color.Gray)
                                }
                                val filteredApps = installedApps.filter { 
                                    it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) 
                                }
                                items(filteredApps) { app ->
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
                                            Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(app.packageName, color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Active Days", color = Color.White, fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val daysOfWeek = listOf("M" to 1, "T" to 2, "W" to 3, "Th" to 4, "F" to 5, "S" to 6, "Su" to 7)
                daysOfWeek.forEach { (label, day) ->
                    val isActive = activeDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isActive) accentColor else darkSurface, CircleShape)
                            .clickable {
                                val newDays = activeDays.toMutableSet()
                                if (isActive) {
                                    if (newDays.size > 1) newDays.remove(day)
                                } else {
                                    newDays.add(day)
                                }
                                activeDays = newDays
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = SourceSerif4,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.Black else Color.White
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { hasCustomTimeWindows = !hasCustomTimeWindows }.padding(vertical = 8.dp)) {
                Checkbox(
                    checked = hasCustomTimeWindows, 
                    onCheckedChange = { hasCustomTimeWindows = it },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor, checkmarkColor = Color.Black, uncheckedColor = Color.White)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Custom schedule per active day", fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Set different start and end times for each day.", fontFamily = HostGrotesk, color = textColor, fontSize = 12.sp)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (!hasCustomTimeWindows) {
                var showStartTimePicker by remember { mutableStateOf(false) }
                var showEndTimePicker by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", color = textColor, fontSize = 14.sp, fontFamily = HostGrotesk)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = darkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(formatTime(startTimeMinute), fontWeight = FontWeight.Bold, color = accentColor, fontFamily = SourceSerif4)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", color = textColor, fontSize = 14.sp, fontFamily = HostGrotesk)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = darkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(formatTime(endTimeMinute), fontWeight = FontWeight.Bold, color = accentColor, fontFamily = SourceSerif4)
                        }
                    }
                }

                if (showStartTimePicker) {
                    CustomTimePickerDialog(
                        title = "Select Global Start Time",
                        initialHour = startTimeMinute / 60,
                        initialMinute = startTimeMinute % 60,
                        onCancel = { showStartTimePicker = false },
                        onConfirm = { hour, min ->
                            startTimeMinute = hour * 60 + min
                            showStartTimePicker = false
                        }
                    )
                }
                if (showEndTimePicker) {
                    CustomTimePickerDialog(
                        title = "Select Global End Time",
                        initialHour = endTimeMinute / 60,
                        initialMinute = endTimeMinute % 60,
                        onCancel = { showEndTimePicker = false },
                        onConfirm = { hour, min ->
                            endTimeMinute = hour * 60 + min
                            showEndTimePicker = false
                        }
                    )
                }
            } else {
                val daysOfWeek = listOf(1 to "Monday", 2 to "Tuesday", 3 to "Wednesday", 4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday")
                daysOfWeek.forEach { (dayInt, dayName) ->
                    if (activeDays.contains(dayInt)) {
                        val dayWindow = customTimeWindows[dayInt] ?: com.example.notivib.domain.model.TimeWindow(0, 1439)
                        var showDayStartPicker by remember { mutableStateOf(false) }
                        var showDayEndPicker by remember { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(dayName, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontFamily = HostGrotesk)
                            Button(
                                onClick = { showDayStartPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = darkSurface),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(formatTime(dayWindow.startTimeMinute), color = accentColor)
                            }
                            Text(" - ", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                            Button(
                                onClick = { showDayEndPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = darkSurface),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(formatTime(dayWindow.endTimeMinute), color = accentColor)
                            }
                        }
                        if (showDayStartPicker) {
                            CustomTimePickerDialog(
                                title = "Select Start Time ($dayName)",
                                initialHour = dayWindow.startTimeMinute / 60,
                                initialMinute = dayWindow.startTimeMinute % 60,
                                onCancel = { showDayStartPicker = false },
                                onConfirm = { hour, min ->
                                    val newMin = hour * 60 + min
                                    customTimeWindows = customTimeWindows.toMutableMap().apply { put(dayInt, dayWindow.copy(startTimeMinute = newMin)) }
                                    showDayStartPicker = false
                                }
                            )
                        }
                        if (showDayEndPicker) {
                            CustomTimePickerDialog(
                                title = "Select End Time ($dayName)",
                                initialHour = dayWindow.endTimeMinute / 60,
                                initialMinute = dayWindow.endTimeMinute % 60,
                                onCancel = { showDayEndPicker = false },
                                onConfirm = { hour, min ->
                                    val newMin = hour * 60 + min
                                    customTimeWindows = customTimeWindows.toMutableMap().apply { put(dayInt, dayWindow.copy(endTimeMinute = newMin)) }
                                    showDayEndPicker = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { remindSchedule = !remindSchedule }.padding(vertical = 8.dp)) {
                Checkbox(
                    checked = remindSchedule, 
                    onCheckedChange = { remindSchedule = it },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor, checkmarkColor = Color.Black, uncheckedColor = Color.White)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Remind when schedule starts/ends", fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Triggers a soft alarm when the window opens/closes.", fontFamily = HostGrotesk, color = textColor, fontSize = 12.sp)
                }
            }

            if (hasOutsideSchedule) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { muteOutsideSchedule = !muteOutsideSchedule }.padding(vertical = 8.dp)) {
                    Checkbox(
                        checked = muteOutsideSchedule, 
                        onCheckedChange = { muteOutsideSchedule = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor, checkmarkColor = Color.Black, uncheckedColor = Color.White)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Mute notifications outside schedule", fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Silently deletes notifications outside the active window.", fontFamily = HostGrotesk, color = textColor, fontSize = 12.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vibrationOnly = !vibrationOnly }.padding(vertical = 8.dp)) {
                Switch(
                    checked = vibrationOnly, 
                    onCheckedChange = { vibrationOnly = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accentColor, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = darkSurface)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Vibration Only Mode", fontFamily = HostGrotesk, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("If enabled, audio alarms will not play.", fontFamily = HostGrotesk, color = textColor, fontSize = 12.sp)
                }
            }
            
            Spacer(Modifier.height(48.dp)) // Extra padding at the bottom so it's not hidden by the bottom bar
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = Color(0xFF161618)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    text = title,
                    color = Color.White,
                    fontFamily = HostGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { 
                        Text(
                            text = "Cancel", 
                            color = Color.White, 
                            fontFamily = HostGrotesk, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp
                        ) 
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9FF0B), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) { 
                        Text(
                            text = "Select", 
                            fontFamily = HostGrotesk, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp,
                            softWrap = false
                        ) 
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    title: String = "Select Time",
    initialHour: Int,
    initialMinute: Int,
    onCancel: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    var showKeyboardInput by remember { mutableStateOf(true) }

    TimePickerDialog(
        title = title,
        onCancel = onCancel,
        onConfirm = {
            onConfirm(timePickerState.hour, timePickerState.minute)
        },
        toggle = {
            IconButton(onClick = { showKeyboardInput = !showKeyboardInput }) {
                Icon(
                    imageVector = if (showKeyboardInput) Icons.Outlined.Schedule else Icons.Outlined.Keyboard,
                    contentDescription = if (showKeyboardInput) "Switch to Clock Dial" else "Switch to Keyboard Input",
                    tint = Color(0xFFD9FF0B)
                )
            }
        }
    ) {
        if (showKeyboardInput) {
            TimeInput(
                state = timePickerState,
                colors = customTimePickerColors()
            )
        } else {
            TimePicker(
                state = timePickerState,
                colors = customTimePickerColors()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun customTimePickerColors(): TimePickerColors {
    val accentColor = Color(0xFFD9FF0B)
    return TimePickerDefaults.colors(
        clockDialColor = Color(0xFF20201E),
        clockDialSelectedContentColor = Color.Black,
        clockDialUnselectedContentColor = Color.White,
        selectorColor = accentColor,
        timeSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
        timeSelectorUnselectedContainerColor = Color(0xFF20201E),
        timeSelectorSelectedContentColor = accentColor,
        timeSelectorUnselectedContentColor = Color.White,
        periodSelectorBorderColor = Color.Transparent,
        periodSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
        periodSelectorUnselectedContainerColor = Color(0xFF20201E),
        periodSelectorSelectedContentColor = accentColor,
        periodSelectorUnselectedContentColor = Color.White
    )
}

fun calculateOutsideScheduleMinutes(
    activeDays: Set<Int>,
    hasCustomTimeWindows: Boolean,
    startTimeMinute: Int,
    endTimeMinute: Int,
    customTimeWindows: Map<Int, com.example.notivib.domain.model.TimeWindow>
): Int {
    var totalOutsideMinutes = 0
    for (day in 1..7) {
        if (!activeDays.contains(day)) {
            totalOutsideMinutes += 1440
        } else {
            val (start, end) = if (hasCustomTimeWindows) {
                val window = customTimeWindows[day] ?: com.example.notivib.domain.model.TimeWindow(0, 1439)
                window.startTimeMinute to window.endTimeMinute
            } else {
                startTimeMinute to endTimeMinute
            }
            
            val activeMinutes = if (start == 0 && (end == 1439 || end == 1440)) {
                1440
            } else if (start < end) {
                (end - start + 1).coerceAtMost(1440)
            } else if (start > end) {
                ((1440 - start) + end + 1).coerceAtMost(1440)
            } else {
                1440
            }
            
            val outsideMinutes = (1440 - activeMinutes).coerceAtLeast(0)
            totalOutsideMinutes += outsideMinutes
        }
    }
    return totalOutsideMinutes
}

fun tryAddKeyword(
    rawText: String,
    targetList: SnapshotStateList<String>,
    otherList: List<String>,
    targetTitle: String,
    otherTitle: String,
    context: android.content.Context,
    onSuccess: () -> Unit = {}
): Boolean {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) return true
    if (targetList.any { it.equals(trimmed, ignoreCase = true) }) {
        onSuccess()
        return true
    }
    if (otherList.any { it.equals(trimmed, ignoreCase = true) }) {
        android.widget.Toast.makeText(context, "Cannot add: '$trimmed' already exists in $otherTitle", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
    if (targetList.size >= 15) {
        android.widget.Toast.makeText(context, "Maximum 15 keywords allowed", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
    targetList.add(trimmed)
    onSuccess()
    return true
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordChipInputGroup(
    title: String,
    placeholder: String,
    helperText: String? = null,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    keywords: SnapshotStateList<String>,
    otherKeywords: List<String> = emptyList(),
    otherTitle: String = "",
    darkSurface: Color,
    accentColor: Color,
    textColor: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val addKeyword: () -> Unit = {
        tryAddKeyword(textInput, keywords, otherKeywords, title, otherTitle, context) {
            onTextInputChange("")
        }
    }

    Column {
        Text(
            text = title,
            color = Color.White,
            fontFamily = HostGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                placeholder = { Text(placeholder, color = Color.Gray, fontFamily = HostGrotesk) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { addKeyword() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = darkSurface,
                    unfocusedContainerColor = darkSurface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = accentColor,
                    unfocusedTextColor = accentColor
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = addKeyword,
                modifier = Modifier
                    .size(50.dp)
                    .background(darkSurface, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Keyword",
                    tint = accentColor
                )
            }
        }

        if (helperText != null) {
            Text(
                text = helperText,
                fontFamily = HostGrotesk,
                color = textColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (keywords.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            com.example.notivib.presentation.components.SimpleFlowRow(
                horizontalSpacing = 8.dp,
                verticalSpacing = 8.dp
            ) {
                keywords.forEach { kw ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF404040),
                        contentColor = Color.White
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = kw,
                                fontFamily = HostGrotesk,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = accentColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { keywords.remove(kw) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

