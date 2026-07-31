# AGENTS.md — NotiVib AI Agent Instructions

> **⚠️ MANDATORY: All AI agents MUST read this file in its entirety before performing ANY action on this codebase.**

## Core Rules

### 1. Read-First Policy
Before executing any task, the AI agent must:
1. Read this `AGENTS.md` file completely.
2. Read `Technical_Specification.md` to understand the system architecture.
3. Read `UI_UX_Design_System.md` to understand the design language.
4. Understand the current state of the codebase before proposing changes.

### 2. No Unsolicited Changes
- **If the user asks a question**, the AI must **only answer the question**. Do NOT implement changes, modifications, or additions to the codebase.
- **If the user asks to investigate or analyze**, the AI must **only report findings**. Do NOT modify code unless explicitly asked.
- **Only modify or create code when the user gives a direct order** (e.g., "fix this", "add this feature", "implement this", "update this").

### 3. Execution Workflow
When the user gives a direct order to modify the codebase:
1. **Research** — Thoroughly investigate the relevant files and understand the impact of the change.
2. **Plan** — For non-trivial changes, create an implementation plan and present it for user approval before executing.
3. **Execute** — Make the changes across all affected layers (model → data → domain → presentation).
4. **Verify** — Compile the project (`.\gradlew.bat compileDebugKotlin` with `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`) and confirm BUILD SUCCESSFUL.
5. **Update documentation** — If the change adds new fields, features, or screens, update `Technical_Specification.md` and/or `UI_UX_Design_System.md`.

### 4. Architecture Constraints
- The project follows **Clean Architecture + MVVM**.
- All changes must flow through the proper layers:
  - **Model** (`domain/model/`) — Data classes
  - **Repository** (`domain/repository/`, `data/repository/`, `data/local/`) — Data persistence
  - **Use Cases** (`domain/usecase/`) — Business logic
  - **Managers** (`domain/manager/`) — Scheduling and alarm management
  - **Services** (`framework/service/`) — Android services (notification listener, foreground, alarm)
  - **Receivers** (`framework/receiver/`) — Broadcast receivers
  - **Presentation** (`presentation/`) — Compose UI, ViewModels, navigation, theme
- **Never skip layers.** If a new field is added to the model, it must be serialized in the DataStore, passed through use cases, and wired into the ViewModel and UI.

### 5. Code Quality Rules
- Preserve all existing comments and docstrings unrelated to the change.
- Remove unused imports after making changes.
- Do not leave dead code (unused functions, variables, or files).
- Use the project's existing code style and patterns.

---

## Project Structure

```
app/src/main/java/com/example/notivib/
├── domain/
│   ├── model/          AlarmRule, TimeWindow
│   ├── repository/     RuleRepository interface, NotificationLogRepository interface
│   ├── usecase/        EvaluateNotificationUseCase, SaveRuleUseCase, DeleteRuleUseCase, GetRulesUseCase
│   └── manager/        ScheduleManager, ScheduleReminderManager
├── data/
│   ├── local/          RulesDataStore (JSON serialization via DataStore Preferences)
│   └── repository/     RuleRepositoryImpl
├── framework/
│   ├── service/        InterceptorService, ActiveAlarmService, EngineForegroundService
│   ├── receiver/       ScheduleReceiver, ScheduleReminderReceiver
│   └── utils/          EngineState, BatteryOptimizationHelper
├── presentation/
│   ├── rules_list/     RulesListScreen, EditRuleScreen, NotificationLogScreen, RulesListViewModel
│   ├── settings/       SettingsScreen
│   ├── alarm/          AlarmActivity
│   ├── navigation/     AppNavigation
│   └── theme/          Color, Theme, Type, icons/
├── di/                 AppModule (Hilt DI)
├── MainActivity.kt
└── NotiVibApp.kt
```

---

## Key Data Model

```kotlin
data class AlarmRule(
    val id: String,
    val ruleName: String,
    val targetPackage: String,       // Package name or "ANY"
    val keyword: String,             // Comma-separated trigger keywords
    val startTimeMinute: Int,
    val endTimeMinute: Int,
    val vibrationOnly: Boolean,
    val isActive: Boolean,
    val activeDays: Set<Int>,        // 1=Sunday, 2=Monday, ..., 7=Saturday
    val hasCustomTimeWindows: Boolean,
    val customTimeWindows: Map<Int, TimeWindow>,
    val muteOutsideSchedule: Boolean,
    val remindSchedule: Boolean,
    val ignoredKeywords: String      // Comma-separated keywords to skip
)
```

---

## Notification Evaluation Priority
When a notification arrives, the evaluation follows this priority:
1. **TriggerAlarm** — If within schedule, keywords match, AND no ignored keywords match → alarm fires.
2. **Mute** — If outside schedule and `muteOutsideSchedule` is enabled → notification is silently dismissed.
3. **Ignore** — No rules match → notification passes through normally.