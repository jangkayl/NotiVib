# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- NotiVib-specific rules ---
#
# Activities, Services, Receivers, and Application declared in AndroidManifest.xml are
# already kept automatically by AGP/R8 (it reads the merged manifest), so no explicit
# -keep is needed for MainActivity, AlarmActivity, InterceptorService,
# ActiveAlarmService, EngineForegroundService, ScheduleReceiver, ScheduleReminderReceiver,
# BootReceiver, or NotiVibApp.
#
# Domain models in com.example.notivib.domain.model and the repositories in
# com.example.notivib.domain.repository serialize themselves manually via org.json
# (JSONObject/JSONArray field access by string key, not reflection), so R8 renaming/
# removing their members does not break (de)serialization - no keep rules needed there.
#
# Hilt: the Dagger/Hilt Gradle plugin and hilt-android ship their own consumer
# ProGuard/R8 rules (keeping generated components, @Inject constructors, etc. via
# @Keep/annotation-driven rules bundled in the AAR), so no manual Hilt rules are required.
#
# Kotlin coroutines: kotlinx-coroutines-android bundles its own consumer rules
# (e.g. for the internal ServiceLoader-based Dispatchers lookup); nothing extra needed.
#
# AndroidX DataStore (datastore-preferences): API surface is generic (Preferences/Flow),
# no reflection on app-specific classes - nothing extra needed.
#
# Kept defensively: keep the Notification Listener API surface and its Kotlin
# metadata-driven override resolution intact for the one service that binds to it.
-keep class com.example.notivib.framework.service.InterceptorService { *; }
