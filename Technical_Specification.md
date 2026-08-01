# Technical Specification: NotiVib — Notification Alarm Interceptor

## Executive Summary
NotiVib is a persistent Android application that continuously monitors incoming device notifications and triggers a robust, un-ignorable hardware alarm (audio and vibration) when user-defined conditions are met. It supports schedule-based interception, per-app rules with keyword filtering, ignored keywords for fine-grained exclusions, focus-mode muting outside active schedules, and schedule reminders. The app features a dark-mode UI built with Jetpack Compose and Material 3.

## Architecture
The application strictly adheres to **Clean Architecture** combined with **MVVM** (Model-View-ViewModel).

### 1. Presentation (UI) Layer
- Built entirely with **Jetpack Compose** and **Material 3**.
- **Theme**: Dark mode default with neon green (`#D9FF0B`) accent for active states.
- **Typography**: Host Grotesk (body/labels), Source Serif 4 (headings/branding).
- **ViewModels**: Manage state via Kotlin `StateFlow`. `RulesListViewModel` handles all rule CRUD operations.
- **Navigation**: State-driven navigation via `AppNavigation` composable with `Destination` enum (RulesList, Logs, Settings, EditRule).
- **Screens**:
  - `RulesListScreen` — Active/Inactive tabs, rule cards, FAB for new rules
  - `EditRuleScreen` — Full rule editor (keywords, ignored keywords, app selector, schedule, options)
  - `SettingsScreen` — Engine toggle, persistent notification toggle, permission management
  - `NotificationLogScreen` — Intercepted notification history and system logs
  - `AlarmActivity` — Full-screen alarm display
  - `PermissionsScreen` — Onboarding flow for required permissions

### 2. Domain Layer
Pure Kotlin modules containing core business logic. No Android framework dependencies.

- **Models**:
  - `AlarmRule` — Complete rule definition with 15 fields including schedule, keywords, ignored keywords, mute mode, and custom time windows per day.
  - `TimeWindow` — Per-day custom start/end time (minute-of-day).

- **Use Cases**:
  - `EvaluateNotificationUseCase` — Core evaluation engine. Checks incoming notifications against all active rules. Returns `TriggerAlarm`, `Mute`, or `Ignore`. Supports:
    - Per-app or "ANY" app targeting
    - Trigger keyword chip/tag matching (title + text) with delimiter-aware parsing (`|||` delimiter, `,` legacy fallback)
    - Ignored keyword chip/tag exclusion (`|||` delimiter, `,` legacy fallback)
    - Time window evaluation (including overnight spans)
    - Custom per-day time windows
    - Active day filtering
    - Focus-mode muting outside schedules (`muteOutsideSchedule`)
  - `SaveRuleUseCase` — Persists a rule to DataStore
  - `DeleteRuleUseCase` — Removes a rule by ID
  - `GetRulesUseCase` — Returns reactive Flow of all rules

- **Managers**:
  - `ScheduleManager` — Evaluates all rules to determine if interception should be active. Schedules `AlarmManager` exact alarms for the next schedule state transition. Returns `true` if any rule is currently active OR any rule has `muteOutsideSchedule` enabled.
  - `ScheduleReminderManager` — Schedules start/end reminder notifications for rules with `remindSchedule` enabled. Uses per-rule request codes derived from rule ID hash.

### 3. Data & Framework Layer

- **Data Persistence**:
  - `RulesDataStore` — Preferences DataStore with manual JSON serialization/deserialization of `AlarmRule` list. Supports atomic read-modify-write operations. All fields are backward-compatible using `optString`/`optBoolean`/`optInt` for graceful migration.
  - `RuleRepositoryImpl` — Thin wrapper exposing DataStore as `RuleRepository` interface.

- **Services**:
  - `InterceptorService` (NotificationListenerService) — Intercepts all incoming notifications. Checks `EngineState.shouldIntercept()` before processing. Evaluates via `EvaluateNotificationUseCase`. Handles alarm triggering and notification muting.
  - `ActiveAlarmService` (Foreground Service) — Plays alarm sound and/or vibration pattern. Runs as `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. Displays full-screen notification that launches `AlarmActivity`.
  - `EngineForegroundService` — Optional persistent notification showing "NotiVib Active" status. Uses `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.

- **Receivers**:
  - `ScheduleReceiver` — Triggered by `AlarmManager` or manual broadcast. Re-evaluates all rules via `ScheduleManager`, updates `EngineState`, and starts/stops `EngineForegroundService`.
  - `ScheduleReminderReceiver` — Handles start/end schedule reminder notifications with follow-up rescheduling.

- **Engine State** (`EngineState` singleton via SharedPreferences):
  - `isGloballyEnabled` — Master on/off switch
  - `isScheduleActive` — Whether any rule window is currently active (or any mute rule exists)
  - `showForegroundNotification` — Whether to display the persistent notification
  - `trackedApps` — Set of packages to log (for notification history)
  - `shouldIntercept()` — Returns `isGloballyEnabled && isScheduleActive`

- **Dependency Injection**: Hilt (`@AndroidEntryPoint`, `@HiltViewModel`, `@Module`/`@InstallIn`)

## System Requirements & Permissions
- `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` — Read incoming notifications
- `android.permission.FOREGROUND_SERVICE` — Base foreground service requirement
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` — Alarm audio playback
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` — Engine status notification
- `android.permission.POST_NOTIFICATIONS` (Android 13+) — Display notifications
- `android.permission.VIBRATE` — Trigger vibration
- `android.permission.SCHEDULE_EXACT_ALARM` — Schedule precise rule transitions
- `android.permission.SYSTEM_ALERT_WINDOW` — Full-screen alarm overlay
- **Battery Optimization Exemption** — User prompted via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

## Data Flow
1. **DataStore → Domain**: Rule changes emit via Kotlin `Flow`.
2. **Domain → UI**: ViewModel collects `Flow` and exposes `StateFlow` to Compose.
3. **Unidirectional Data Flow**: UI observes state → User interaction → ViewModel → UseCase → DataStore → Flow emits → UI updates.
4. **Service Synchronization**: `InterceptorService` and `ScheduleReceiver` read rules from the same repository `Flow`, ensuring real-time rule updates without service restart.
5. **Schedule Transitions**: `ScheduleManager` sets exact `AlarmManager` alarms for the next rule start/end time. `ScheduleReceiver` re-evaluates and reschedules on each trigger.

## Notification Evaluation Pipeline
```
Notification arrives
    → EngineState.shouldIntercept()? No → pass through
    → Yes → EvaluateNotificationUseCase.evaluate()
        → For each active rule:
            → matchApp? No → skip
            → Active day? No → set pendingMute if muteOutsideSchedule → skip
            → Within time window?
                → Yes → keyword match?
                    → Yes → ignored keyword match?
                        → No → return TriggerAlarm ✓
                        → Yes → skip (ignored)
                    → No → skip
                → No → set pendingMute if muteOutsideSchedule
        → Return pendingMute or Ignore
```
