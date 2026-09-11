# `QUERY_ALL_PACKAGES` and Google Play package-visibility compliance

## Where and why the permission is used today

- Declared in `app/src/main/AndroidManifest.xml:12`:
  `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />`
- `README.md:104` documents the stated justification: "Required to query the `PackageManager` for a highly sanitized list of user-facing launcher apps."
- The actual app-enumeration code is `getInstalledApps()` in
  `app/src/main/java/com/example/notivib/presentation/rules_list/RulesListScreen.kt:151-190+`. It **already** uses a
  scoped-friendly pattern:
  ```kotlin
  val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
  val resolveInfoList = pm.queryIntentActivities(intent, ...)
  ```
  This is the launcher-intent query, not `getInstalledApplications()`. It is filtered further to drop labels that look
  like raw package/class names (`RulesListScreen.kt:187`). It is cached in `AppListCache` (`RulesListScreen.kt:145-149`)
  and reused by both `EditRuleScreen.kt:117` (app picker) and `NotificationLogScreen.kt:176` (search/autocomplete).
- Icon loading (`AppIconImage`, `NotificationLogScreen.kt:349-378`) calls
  `context.packageManager.getApplicationIcon(packageName)` **by exact package name**, for two different kinds of
  packages:
  - Picker/target-app icons (`EditRuleScreen.kt:462,529`, `NotificationLogScreen.kt:261`) — packages that came from the
    launcher-intent enumeration above.
  - Notification-log-entry icons (`NotificationLogScreen.kt:306`, `AppIconImage(packageName = log.packageName, ...)`) —
    the package that actually **posted** a notification, which is arbitrary and not necessarily a launcher app.
- Label-by-package lookups for a saved rule's target app: `EditRuleScreen.kt:103-104` and
  `RulesListScreen.kt:878-879`, both `getApplicationInfo(rule.targetPackage, 0)` + `getApplicationLabel(...)`, wrapped
  in try/catch that falls back to the raw package string.
- `AlarmActivity.kt` (`RuleAppPill`, lines 128-178): tries `getApplicationIcon(appName)` and `getApplicationIcon(ruleName)`
  directly (note: the `appName` parameter here is actually a **label string** resolved upstream by
  `InterceptorService`, not a package name — this call almost always throws and falls through). Its fallback,
  `pm.getInstalledApplications(0)` (`AlarmActivity.kt:158`), linearly scans **every installed package** to match by
  label. This is the one enumeration call in the app that is not scoped to launcher apps and genuinely depends on
  broad visibility to find non-launcher senders.
- `InterceptorService.kt:82-83` (out of scope for this change — PR #11 is pending on this file) resolves
  `getApplicationLabel`/`getApplicationInfo` for **whatever package posted the intercepted notification**, which can
  be any app on the device, launcher or not. This is the core reason the permission exists: NotiVib is a notification
  interceptor and by design needs to label arbitrary posting packages, not just user-launchable ones.

## What breaks under package visibility (API 30+) if `QUERY_ALL_PACKAGES` is removed

With a scoped `<queries>` element declaring only the `ACTION_MAIN`/`CATEGORY_LAUNCHER` intent:

| Call | Still works? | Why |
|---|---|---|
| `getInstalledApps()` (`queryIntentActivities` on MAIN/LAUNCHER) | **Yes** | Matches the declared `<queries><intent>` filter exactly — Android grants visibility for intent-filter queries that match a declared filter, independent of the permission. |
| Icon/label lookup for a package returned by the picker (target app, log entries that are launcher apps) | **Yes** | Once a package is "visible" (matched a declared `<queries>` filter), all specific-package `PackageManager` calls for that package (e.g. `getApplicationIcon`, `getApplicationInfo`, `getApplicationLabel`) succeed. |
| Icon lookup for a **notification log entry from a non-launcher package** (`NotificationLogScreen.kt:306`) | **No** — degrades gracefully | `getApplicationIcon(log.packageName)` throws `NameNotFoundException` for a package not covered by any declared `<queries>` entry; already wrapped in try/catch, so it silently shows a blank icon Box instead of crashing. |
| `AlarmActivity`'s `getInstalledApplications(0)` fallback scan (`AlarmActivity.kt:158`) | **No** — degrades gracefully | Returns only the caller's own package plus packages matching a declared `<queries>` filter (i.e., launcher apps). A non-launcher app whose notification triggered the alarm won't be found by label, so the alarm screen shows no icon (already tolerant of `null`/missing icon). |
| `InterceptorService` label resolution for an arbitrary posting package (not touched here) | **Partially** — same tradeoff | `getApplicationInfo`/`getApplicationLabel` will throw for non-launcher, non-visible packages; the existing code already falls back to the raw package name (`InterceptorService.kt:85`, `:123`). Under scoped queries this fallback will trigger more often for non-launcher apps (e.g., some system/background notification sources), so more log/alarm entries would show the raw package id instead of a friendly label. **Flagged as a follow-up for PR #11**, not fixed here. |

Net effect: the rule editor's app picker (the only place `QUERY_ALL_PACKAGES` is nominally justified for) is unaffected,
because it never used `getInstalledApplications()` in the first place. The only regressions are cosmetic — icons/labels
for packages outside the "launcher apps" visibility set falling back to a blank icon or a raw package id — and all of
those code paths already have graceful fallbacks in place (no crashes).

## Option A — Keep `QUERY_ALL_PACKAGES` + file a Play Console declaration

Play requires apps that keep this permission to submit a Permission Declaration Form asserting (per Play's current
policy) that the app's **core functionality** requires seeing all installed apps, and that no narrower mechanism
(scoped `<queries>`, `PackageInstaller.SessionInfo`, `ACTION_MAIN`/`LAUNCHER` queries, etc.) is sufficient. Accepted use
cases are narrow: device search apps, antivirus/security suites, backup/restore apps, app stores/launchers/file
managers. Google reviews the declared use case against actual app behavior at each release.

Risk of rejection for NotiVib specifically:
- NotiVib's stated visible functionality (per README and the picker UI) is "a sanitized list of user-facing launcher
  apps" — i.e., the picker itself only shows launcher apps and does not need full visibility. That undermines a
  declaration claiming full-device visibility is core, since the primary user-facing surface demonstrably works with
  a scoped launcher query today.
- The only genuine need for broad visibility is internal label resolution for arbitrary notification-posting packages
  in `InterceptorService`/`AlarmActivity`, which is a debugging/labeling nicety (falls back to package name), not a
  feature the user directly selects or depends on to operate the app.
- Play reviewers are documented as being strict/inconsistent on notification-listener-adjacent apps claiming
  `QUERY_ALL_PACKAGES`; sideload-style "intercept everything" tooling is exactly the profile Play scrutinizes most.
- A rejected declaration blocks the release entirely until the permission is removed or use case rewritten, i.e. worse
  than just doing Option B upfront.

## Option B — Replace with scoped `<queries>` (launcher intent) — chosen implementation

Exactly what changes:
- Manifest: remove the `QUERY_ALL_PACKAGES` `<uses-permission>` and add:
  ```xml
  <queries>
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent>
  </queries>
  ```
- No code change was required in the app picker's enumeration (`getInstalledApps()`), because it already queries by
  the same `ACTION_MAIN`/`CATEGORY_LAUNCHER` intent rather than `getInstalledApplications()`. The manifest `<queries>`
  entry is what makes that same query return the **full** launcher-app result set once the broad permission is gone
  (without a declared `<queries>` match, `queryIntentActivities` would be filtered down to only visible packages, which
  under default visibility rules is just the calling app itself).
- What the picker still shows: unchanged — the same "highly sanitized list of user-facing launcher apps" as before,
  because that was already the entire input set.
- Tradeoffs: notification-log icons and `AlarmActivity`'s icon fallback for **non-launcher** notification sources may
  show a blank icon (already handled), and `InterceptorService` labels for non-launcher packages may fall back to the
  raw package name more often (already handled, pending a possible future improvement in PR #11 e.g. adding a
  `<queries><package>` entry for specific known non-launcher senders, or accepting the raw-package-name fallback as
  permanent behavior).

## Recommendation

**Option B (scoped `<queries>`)** is the safer and expected path for this app, and is what this PR implements:

1. The feature that is actually user-facing and permission-relevant (the rule editor's app picker) already works
   entirely off a scoped launcher-intent query — it never needed `getInstalledApplications()` or the broad permission.
2. Removing the permission removes a listed Play distribution blocker with no user-visible functionality loss in the
   picker.
3. The only downside — occasional blank icons / raw package names for non-launcher notification senders in the log
   and alarm screens — already degrades gracefully today (try/catch fallbacks exist everywhere) and is a cosmetic
   nicety, not core functionality.
4. Filing an Option A declaration would put a real release at risk of rejection for a feature (full visibility) that
   the app's own UI doesn't actually expose or depend on, and Play is documented as unusually strict on this
   permission for notification-listener-style apps.

## Residual follow-up (not in this change)

- `InterceptorService.kt` (`getApplicationInfo`/`getApplicationLabel` for arbitrary posting packages, lines 82-83) is
  under active work in PR #11 and intentionally not touched here. Its label-resolution fallback-to-package-name
  behavior will now trigger more often for non-launcher senders once `QUERY_ALL_PACKAGES` is removed; worth revisiting
  there (e.g., accept the fallback as final behavior, or special-case a short allowlist of known non-launcher senders
  via `<queries><package>`).
- On-device verification is still required (see manual test steps in the implementation PR); package-visibility
  filtering behavior cannot be exercised by a plain `assembleDebug`/unit-test run.
