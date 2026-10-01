# Implementation plan: Protected notifications

**Goal.** Each rule gets two independent switches: **Ring alarm** and **Protect notification**. When a rule with Protect on matches an incoming notification, NotiVib posts its own copy. "Clear all" and swipe don't remove that copy; only its **Acknowledge** button does. The app's original notification is not touched.

## Rules for the implementer

- Read `AGENTS.md`, `Technical_Specification.md` and `UI_UX_Design_System.md` first.
- Work on a new branch off `main`. Do not push. Do not touch `.idea/`.
- Follow the layer order: model → data → domain → framework → presentation.
- Remove unused imports, leave no dead code, keep existing comments, match existing style.
- Build: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat compileDebugKotlin`
- Run `testDebugUnitTest`; all 33 existing tests must keep passing.
- Do not add dependencies.
- Do not change the order in which rules are matched.

## Step 1: Model

**`domain/model/AlarmRule.kt`:** add two fields at the end of `AlarmRule`:

```kotlin
val ringAlarm: Boolean = true,
val protectNotification: Boolean = false
```

The defaults keep every existing rule behaving as it does today.

**`domain/model/RuleSerialization.kt`:**
- In `serializeRules`: `obj.put("ringAlarm", rule.ringAlarm)` and `obj.put("protectNotification", rule.protectNotification)`.
- In `parseRules`: `obj.optBoolean("ringAlarm", true)` and `obj.optBoolean("protectNotification", false)`. Old backups must still import.
- Extend `RuleSerializationTest.kt` with a round-trip that includes both fields, and a test that JSON without them parses to the defaults.

**`presentation/rules_list/RulesListScreen.kt`:** there are two hand-written copies of rule JSON at about lines 379 and 953 (grep `remindSchedule`). If they are still used, add both fields there too. If they are dead code, do not delete them; report it instead.

## Step 2: Evaluation (no signature change)

**`domain/usecase/EvaluateNotificationUseCase.kt`:** keep `EvaluationResult.TriggerAlarm(rule, matchedKeywords)`. It now means "rule matched". Do not rename it; the existing tests use it. Callers decide what to do from the flags on `rule`.

Known limitation: evaluation stops at the first matching rule. Note it in a comment. Do not change the matching order.

## Step 3: Storage

**New `domain/repository/ProtectedNoticeRepository.kt`**: `@Singleton`, `@Inject constructor(@ApplicationContext context)`. Copy the pattern of `NotificationLogRepository`: its own `preferencesDataStore(name = "protected_notices_prefs")`, JSON in one string key, `CoroutineScope(Dispatchers.IO)`.

```kotlin
data class ProtectedNotice(
    val key: String,          // "<packageName>|<conversationOrTitle>", unique per copy
    val notifId: Int,         // key.hashCode(), used for NotificationManager.notify
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val ruleName: String
)
```

Methods:
- `suspend fun getAll(): List<ProtectedNotice>`, and `val notices: Flow<List<ProtectedNotice>>`.
- `fun upsert(notice: ProtectedNotice)`: replaces an entry with the same `key` and moves it to the front.
- `fun remove(key: String)`.
- `fun clear()`.
- Cap of 40 entries. When over the cap, drop the oldest and log it (Step 9).
- `parse` / `serialize` helpers built with `org.json`, like the log repository.

Privacy: message text is stored on the device until Acknowledge removes it.

## Step 4: Manager

**New `domain/manager/ProtectedNoticeManager.kt`** (an `object`). Log with `ReminderDiagnostics.log(context, msg)`, as the reminder code does.

- Channel `protected_notice_channel`, name "Protected Notifications", `IMPORTANCE_DEFAULT`, sound null, vibration off. Create it in a helper before the first post. Protect-only must not alarm.
- `fun post(context, notice)`:
  - Build with `NotificationCompat.Builder`: `setOngoing(true)`, `setAutoCancel(false)`, `setOnlyAlertOnce(true)`, `setCategory(CATEGORY_MESSAGE)`.
  - Small icon: `R.drawable.notivib_new_logo` if it renders acceptably as a small icon, otherwise `android.R.drawable.ic_dialog_info` (as `EngineForegroundService` does).
  - Title `"${notice.appName}: ${notice.title}"`, text `notice.text`, plus `BigTextStyle` with the text. `setWhen(timeMillis)`, `setShowWhen(true)`.
  - Content intent: `packageManager.getLaunchIntentForPackage(packageName)` in `PendingIntent.getActivity(..., FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`. If that returns null, fall back to opening `MainActivity`. Tapping does NOT remove the notice.
  - Action "Acknowledge": intent to `ProtectedNoticeReceiver` with `ACTION_ACK` and the key as an extra. Request code = `notifId`, flags `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`.
  - Delete intent: intent to `ProtectedNoticeReceiver` with `ACTION_REPOST` and the key.
  - `NotificationManagerCompat.notify(notifId, notification)` in a try/catch for `SecurityException` (POST_NOTIFICATIONS denied). On failure, log `[Protect] FAILED ...`.
- `fun acknowledge(context, key)`: cancel the notification, remove it from the repository, log.
- `fun restoreAll(context, notices)`: post each one without a per-notice log; log one summary line.

To reach the repository from a non-Hilt object, add an `@EntryPoint` helper like `framework/utils/ReminderDiagnostics.kt`, or inject it into the `@AndroidEntryPoint` receiver.

## Step 5: Receiver

**New `framework/receiver/ProtectedNoticeReceiver.kt`**: `@AndroidEntryPoint`, repository injected. Use `goAsync()` and always call `finish()` in `finally`. Catch every `Throwable`.
- `ACTION_ACK` → `ProtectedNoticeManager.acknowledge`.
- `ACTION_REPOST` → if the key is still in the repository, re-post it; otherwise do nothing.

Declare it in `AndroidManifest.xml` with `android:exported="false"`, like `ScheduleReminderReceiver`.

## Step 6: Interceptor

**`framework/service/InterceptorService.kt`**, in the `TriggerAlarm` branch (around line 163):
- Change the existing `if (!ActiveAlarmService.isAlarmRunning) { ... triggerAlarm(...) }` to `if (evaluationResult.rule.ringAlarm && !ActiveAlarmService.isAlarmRunning)`. The body is unchanged.
- Before that, add `if (evaluationResult.rule.protectNotification)`:
  - `key = "$packageName|${conversationTitle.ifEmpty { title }}"`, `notifId = key.hashCode()`
  - `text = fullText.ifEmpty { "No Content" }`, `timeMillis = System.currentTimeMillis()`
  - `appName` and `ruleName` from what is already in scope
  - Then `repository.upsert(...)` and `ProtectedNoticeManager.post(...)`.
- This runs inside the existing `scope.launch` try/catch. The service already ignores NotiVib's own package, so there is no self-loop.
- Log `[Protect] Posted` or `[Protect] Updated` (Updated if the key already existed), with `"<app>: <title>"` and the rule name.

## Step 7: Reboot restore

**`framework/receiver/BootReceiver.kt`**: on `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`, restore protected notices.
- Read all from the repository and call `restoreAll`, in a `goAsync` coroutine.
- This must also happen when the engine is globally disabled: do not put it behind `isGloballyEnabled`.
- After a reboot the original content intent is gone, so tap opens the app's launcher activity (Step 4 already does that).

## Step 8: UI

**`presentation/rules_list/EditRuleScreen.kt`:**
- Add two state variables next to `remindSchedule` (line ~67): `ringAlarm` (default `rule?.ringAlarm ?: true`) and `protectNotification` (default `rule?.protectNotification ?: false`).
- Add two `Switch` rows in the same style as the `remindSchedule` row (around line 714). Labels: "Ring alarm" and "Protect notification (stays until you acknowledge)".
- Wire both into the dirty-check (~lines 132–143), the save call (~182) and the duplicate-rule comparison (~352).
- **Guardrail:** block saving, with the kind of message the editor already shows for other invalid states, when `!ringAlarm && !protectNotification && !muteOutsideSchedule`. Such a rule does nothing.

**`presentation/rules_list/RulesListViewModel.kt`:** add both parameters to the `saveRule` parameter list (line ~70) and pass them into `AlarmRule(...)` (line ~100).

**Rule card:** show a small "Protected" badge in `RulesListScreen` when `protectNotification` is on. Follow `UI_UX_Design_System.md`.

## Step 9: Diagnostics

Log through `ReminderDiagnostics.log` with the `[Protect]` prefix:
- `Posted` / `Updated`
- `Acknowledged`
- `Re-posted after swipe`
- `Restored N after reboot`
- `Evicted oldest (cap reached)`
- `FAILED ...` (POST_NOTIFICATIONS denied or another exception)

## Step 10: Docs

Update `Technical_Specification.md` (two rule fields, storage, manager, receiver, diagnostics) and `UI_UX_Design_System.md` (two switches, badge).

## Tests to add (JVM only, no new dependencies)

- Serialization round-trip and defaults (Step 1).
- Evaluation: a rule with `ringAlarm = false, protectNotification = true` still returns `TriggerAlarm` on a match.
- Repository: if the parse/serialize helpers can be `internal` and Context-free, test upsert ordering, the cap and removal. Otherwise say it is untestable.

**Manual device checks to list in your report:**
- A protect-only rule makes no sound.
- "Clear all" leaves the copy in place.
- A second message in the same chat updates the copy instead of adding one.
- Tapping opens the app and the copy stays.
- Acknowledge removes it.
- Reboot restores it.

## Out of scope

- Removing the original notification.
- Any new permission.
- Changing rule matching order.
- A screen for managing pending notices.

## Reviewer checklist (Claude will review against this)

- Both flag defaults are correct, serialization is backward compatible, and both duplicate JSON copies in `RulesListScreen` are handled.
- `ringAlarm` gates only the alarm; the protect branch runs whether or not an alarm is already ringing.
- The notification is ongoing, tapping never removes it, and request codes and keys cannot collide.
- Both receivers use `goAsync` with `finish()` in `finally`, and nothing throws uncaught.
- Boot restore also runs when the engine is disabled.
- The "does nothing" rule is blocked, and the docs are updated.
