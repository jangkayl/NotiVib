package com.example.notivib.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.notivib.framework.utils.ReminderDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.protectedNoticesDataStore by preferencesDataStore(name = "protected_notices_prefs")

data class ProtectedNotice(
    val key: String,          // "<packageName>|<conversationOrTitle>", unique per copy
    val notifId: Int = key.hashCode(),         // key.hashCode(), used for NotificationManager.notify
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val ruleName: String
)

@Singleton
class ProtectedNoticeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val NOTICES_KEY = stringPreferencesKey("protected_notices_json")

    val notices: Flow<List<ProtectedNotice>> = context.protectedNoticesDataStore.data.map { prefs ->
        parseNotices(prefs[NOTICES_KEY] ?: "[]")
    }

    suspend fun getAll(): List<ProtectedNotice> {
        val prefs = context.protectedNoticesDataStore.data.first()
        return parseNotices(prefs[NOTICES_KEY] ?: "[]")
    }

    suspend fun upsert(notice: ProtectedNotice): List<ProtectedNotice> {
        var result: List<ProtectedNotice> = emptyList()
        context.protectedNoticesDataStore.edit { prefs ->
            val current = parseNotices(prefs[NOTICES_KEY] ?: "[]")
            val (updated, evicted) = upsertNoticeInList(current, notice)
            if (evicted) {
                ReminderDiagnostics.log(context, "[Protect] Evicted oldest (cap reached)")
            }
            prefs[NOTICES_KEY] = serializeNotices(updated)
            result = updated
        }
        return result
    }

    suspend fun remove(key: String): List<ProtectedNotice> {
        var result: List<ProtectedNotice> = emptyList()
        context.protectedNoticesDataStore.edit { prefs ->
            val current = parseNotices(prefs[NOTICES_KEY] ?: "[]")
            val updated = removeNoticeFromList(current, key)
            prefs[NOTICES_KEY] = serializeNotices(updated)
            result = updated
        }
        return result
    }

    suspend fun clear() {
        context.protectedNoticesDataStore.edit { prefs ->
            prefs[NOTICES_KEY] = "[]"
        }
    }
}

internal fun upsertNoticeInList(current: List<ProtectedNotice>, notice: ProtectedNotice): Pair<List<ProtectedNotice>, Boolean> {
    val list = current.filter { it.key != notice.key }.toMutableList()
    list.add(0, notice)
    val evicted = list.size > 40
    while (list.size > 40) {
        list.removeAt(list.lastIndex)
    }
    return Pair(list, evicted)
}

internal fun removeNoticeFromList(current: List<ProtectedNotice>, key: String): List<ProtectedNotice> {
    return current.filter { it.key != key }
}

internal fun parseNotices(json: String): List<ProtectedNotice> {
    val list = mutableListOf<ProtectedNotice>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val key = obj.getString("key")
            list.add(
                ProtectedNotice(
                    key = key,
                    notifId = obj.optInt("notifId", key.hashCode()),
                    packageName = obj.optString("packageName", ""),
                    appName = obj.optString("appName", ""),
                    title = obj.optString("title", ""),
                    text = obj.optString("text", ""),
                    timeMillis = obj.optLong("timeMillis", 0L),
                    ruleName = obj.optString("ruleName", "")
                )
            )
        }
    } catch (e: Exception) {
        // Malformed storage: return what parsed so far, and say so instead of failing silently.
        android.util.Log.w("ProtectedNotice", "Failed to parse stored notices: ${e.message}")
    }
    return list
}

internal fun serializeNotices(notices: List<ProtectedNotice>): String {
    val array = JSONArray()
    notices.forEach { notice ->
        val obj = JSONObject()
        obj.put("key", notice.key)
        obj.put("notifId", notice.notifId)
        obj.put("packageName", notice.packageName)
        obj.put("appName", notice.appName)
        obj.put("title", notice.title)
        obj.put("text", notice.text)
        obj.put("timeMillis", notice.timeMillis)
        obj.put("ruleName", notice.ruleName)
        array.put(obj)
    }
    return array.toString()
}
