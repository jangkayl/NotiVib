import os

file_path = r'app\src\main\java\com\example\notivib\presentation\rules_list\RulesListScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

correct_code = '''TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("NotiVib", fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                        Spacer(Modifier.width(8.dp))
                        val isEngineActive = hasNotificationAccess && isServiceEnabled
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (isEngineActive) androidx.compose.ui.graphics.Color.Green else MaterialTheme.colorScheme.error,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = { showSystemLogsDialog = true }) {
                        Icon(Icons.Outlined.Build, contentDescription = "System Status Logs", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onNavigateToLogs,
                    containerColor = androidx.compose.ui.graphics.Color(0xFFE3E3E3),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF333333),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Outlined.History, contentDescription = "Intercept Log")
                }
                ExtendedFloatingActionButton(
                    onClick = { 
                        editingRule = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(24.dp),
                    icon = { Icon(Icons.Outlined.Add, contentDescription = "Add Rule") },
                    text = { Text("New Rule", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->'''

# We need to find the start and end indices of the broken block
start_idx = content.find('TopAppBar(')
end_idx = content.find(') { padding ->') + len(') { padding ->')

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + correct_code + content[end_idx:]
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Fixed RulesListScreen.kt successfully.")
else:
    print("Could not find start or end index.")
