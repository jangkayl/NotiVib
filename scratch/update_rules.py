import os
import re

filepath = r"c:\Users\Ryan\StudioProjects\NotiVib\app\src\main\java\com\example\notivib\presentation\rules_list\RulesListScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

old_signature = """fun RulesListScreen(
    viewModel: RulesListViewModel = hiltViewModel(),
    onNavigateToLogs: () -> Unit = {}
) {"""
new_signature = """fun RulesListScreen(
    viewModel: RulesListViewModel = hiltViewModel(),
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {"""
content = content.replace(old_signature, new_signature)

old_icon_btn = """IconButton(onClick = { showStatusCards = !showStatusCards }) {
                        Icon(Icons.Default.Settings, contentDescription = "Toggle Status Cards", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }"""
new_icon_btn = """IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }"""
content = content.replace(old_icon_btn, new_icon_btn)

old_state = "val systemLogs by viewModel.systemLogs.collectAsState()"
new_state = """val systemLogs by viewModel.systemLogs.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val activeRules = rules.filter { it.isActive }
    val inactiveRules = rules.filter { !it.isActive }
    val currentRulesList = if (selectedTabIndex == 0) activeRules else inactiveRules"""
content = content.replace(old_state, new_state)

start_marker = "AnimatedVisibility(visible = showStatusCards) {"
end_marker = "if (rules.isEmpty()) {"

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx != -1 and end_idx != -1:
    tab_row_code = """
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(if (activeRules.isEmpty()) "Active" else "Active (${activeRules.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(if (inactiveRules.isEmpty()) "Inactive" else "Inactive (${inactiveRules.size})", fontWeight = FontWeight.Bold) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                )
            }
            
            if (currentRulesList.isEmpty()) {
"""
    content = content[:start_idx] + tab_row_code + content[end_idx + len(end_marker):]

content = content.replace("items(rules, key = { it.id }) { rule ->", "items(currentRulesList, key = { it.id }) { rule ->")

imports_to_add = """
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
"""
if "import androidx.compose.material3.TabRow" not in content:
    content = content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\n" + imports_to_add)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")

