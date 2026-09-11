package com.example.notivib.domain.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.logsDataStore by preferencesDataStore(name = "notification_logs_prefs")

data class NotificationLog(
    val date: String,
    val time: String,
    val appName: String,
    val packageName: String,
    val title: String,
    val text: String,
    val matchedRule: String?
)

@Singleton
class NotificationLogRepository @Inject constructor(@ApplicationContext private val context: Context) {
    
    private val INTERCEPT_LOGS_KEY = stringPreferencesKey("intercept_logs")
    private val SYSTEM_LOGS_KEY = stringPreferencesKey("system_logs")
    private val CONNECTION_LOGS_KEY = stringPreferencesKey("connection_logs")

    private val scope = CoroutineScope(Dispatchers.IO)

    val logs: StateFlow<List<NotificationLog>> = context.logsDataStore.data.map { prefs ->
        parseInterceptLogs(prefs[INTERCEPT_LOGS_KEY] ?: "[]")
    }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val systemLogs: StateFlow<List<String>> = context.logsDataStore.data.map { prefs ->
        parseSystemLogs(prefs[SYSTEM_LOGS_KEY] ?: "[]")
    }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    // Routine listener connect/disconnect churn is noisy on some OEMs. It lives in its own
    // larger buffer so it never evicts real errors/restarts from the 15-entry diagnostics log.
    val connectionLogs: StateFlow<List<String>> = context.logsDataStore.data.map { prefs ->
        parseSystemLogs(prefs[CONNECTION_LOGS_KEY] ?: "[]")
    }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    fun addSystemLog(message: String) {
        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        }
        val log = "[$now] $message"
        
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseSystemLogs(prefs[SYSTEM_LOGS_KEY] ?: "[]").toMutableList()
                current.add(0, log)
                if (current.size > 15) {
                    val dropFrom = maxOf(0, current.size - 5)
                    current.subList(dropFrom, current.size).clear()
                }
                prefs[SYSTEM_LOGS_KEY] = serializeSystemLogs(current)
            }
        }
    }

    fun addConnectionLog(message: String) {
        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        }
        val log = "[$now] $message"

        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseSystemLogs(prefs[CONNECTION_LOGS_KEY] ?: "[]").toMutableList()
                current.add(0, log)
                if (current.size > 50) {
                    val dropFrom = maxOf(0, current.size - 10)
                    current.subList(dropFrom, current.size).clear()
                }
                prefs[CONNECTION_LOGS_KEY] = serializeSystemLogs(current)
            }
        }
    }

    fun deleteConnectionLog(log: String) {
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseSystemLogs(prefs[CONNECTION_LOGS_KEY] ?: "[]").toMutableList()
                current.remove(log)
                prefs[CONNECTION_LOGS_KEY] = serializeSystemLogs(current)
            }
        }
    }

    fun clearConnectionLogs() {
        scope.launch {
            context.logsDataStore.edit { prefs -> prefs[CONNECTION_LOGS_KEY] = "[]" }
        }
    }

    fun deleteSystemLog(log: String) {
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseSystemLogs(prefs[SYSTEM_LOGS_KEY] ?: "[]").toMutableList()
                current.remove(log)
                prefs[SYSTEM_LOGS_KEY] = serializeSystemLogs(current)
            }
        }
    }

    fun clearSystemLogs() {
        scope.launch {
            context.logsDataStore.edit { prefs -> prefs[SYSTEM_LOGS_KEY] = "[]" }
        }
    }

    fun addLog(appName: String, packageName: String, title: String, text: String, matchedRule: String?) {
        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        } else {
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        }
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        }
        val log = NotificationLog(today, now, appName, packageName, title, text, matchedRule)
        
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseInterceptLogs(prefs[INTERCEPT_LOGS_KEY] ?: "[]").toMutableList()
                
                val latestFromPackage = current.firstOrNull { it.packageName == packageName }
                if (latestFromPackage != null && latestFromPackage.title == title && latestFromPackage.text == text) {
                    return@edit
                }

                current.add(0, log)
                if (current.size > 100) {
                    current.subList(current.size - 20, current.size).clear()
                }
                prefs[INTERCEPT_LOGS_KEY] = serializeInterceptLogs(current)
            }
        }
    }

    fun deleteInterceptLog(log: NotificationLog) {
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseInterceptLogs(prefs[INTERCEPT_LOGS_KEY] ?: "[]").toMutableList()
                current.removeAll { it.date == log.date && it.time == log.time && it.packageName == log.packageName && it.title == log.title }
                prefs[INTERCEPT_LOGS_KEY] = serializeInterceptLogs(current)
            }
        }
    }

    fun clearInterceptLogs() {
        scope.launch {
            context.logsDataStore.edit { prefs -> prefs[INTERCEPT_LOGS_KEY] = "[]" }
        }
    }

    private fun parseInterceptLogs(json: String): List<NotificationLog> {
        val list = mutableListOf<NotificationLog>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(NotificationLog(
                    date = if (obj.has("date")) obj.getString("date") else LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    time = obj.getString("time"),
                    appName = obj.getString("appName"),
                    packageName = obj.getString("packageName"),
                    title = obj.getString("title"),
                    text = obj.getString("text"),
                    matchedRule = if (obj.has("matchedRule")) obj.getString("matchedRule") else null
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private fun serializeInterceptLogs(logs: List<NotificationLog>): String {
        val array = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject()
            obj.put("date", log.date)
            obj.put("time", log.time)
            obj.put("appName", log.appName)
            obj.put("packageName", log.packageName)
            obj.put("title", log.title)
            obj.put("text", log.text)
            log.matchedRule?.let { obj.put("matchedRule", it) }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSystemLogs(json: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {}
        return list
    }

    private fun serializeSystemLogs(logs: List<String>): String {
        val array = JSONArray()
        logs.forEach { array.put(it) }
        return array.toString()
    }
}
