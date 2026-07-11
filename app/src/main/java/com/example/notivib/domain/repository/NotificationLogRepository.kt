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

    private val scope = CoroutineScope(Dispatchers.IO)

    val logs: StateFlow<List<NotificationLog>> = context.logsDataStore.data.map { prefs ->
        parseInterceptLogs(prefs[INTERCEPT_LOGS_KEY] ?: "[]")
    }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val systemLogs: StateFlow<List<String>> = context.logsDataStore.data.map { prefs ->
        parseSystemLogs(prefs[SYSTEM_LOGS_KEY] ?: "[]")
    }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addSystemLog(message: String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val now = java.time.LocalDateTime.now().format(formatter)
        val log = "[$now] $message"
        
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseSystemLogs(prefs[SYSTEM_LOGS_KEY] ?: "[]").toMutableList()
                current.add(0, log)
                if (current.size > 50) current.removeLast()
                prefs[SYSTEM_LOGS_KEY] = serializeSystemLogs(current)
            }
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

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addLog(appName: String, packageName: String, title: String, text: String, matchedRule: String?) {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val log = NotificationLog(today, now, appName, packageName, title, text, matchedRule)
        
        scope.launch {
            context.logsDataStore.edit { prefs ->
                val current = parseInterceptLogs(prefs[INTERCEPT_LOGS_KEY] ?: "[]").toMutableList()
                
                val latestFromPackage = current.firstOrNull { it.packageName == packageName }
                if (latestFromPackage != null && latestFromPackage.title == title && latestFromPackage.text == text) {
                    return@edit
                }

                current.add(0, log)
                if (current.size > 150) current.removeLast()
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
