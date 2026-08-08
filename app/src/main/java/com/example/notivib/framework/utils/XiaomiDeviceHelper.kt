package com.example.notivib.framework.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.example.notivib.framework.service.InterceptorService

object XiaomiDeviceHelper {

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        val finger = Build.FINGERPRINT?.lowercase() ?: ""
        return manufacturer.contains("xiaomi") || 
               brand.contains("xiaomi") || 
               brand.contains("redmi") || 
               brand.contains("poco") ||
               finger.contains("xiaomi") ||
               finger.contains("miui")
    }

    fun getAutostartIntent(context: Context): Intent {
        val intent = Intent()
        try {
            intent.component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        } catch (e: Exception) {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = android.net.Uri.parse("package:${context.packageName}")
        }
        return intent
    }

    fun getMiuiPermissionsIntent(context: Context): Intent {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        try {
            intent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            intent.putExtra("extra_pkgname", context.packageName)
        } catch (e: Exception) {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = android.net.Uri.parse("package:${context.packageName}")
        }
        return intent
    }

    fun requestRebindListener(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val componentName = ComponentName(context, InterceptorService::class.java)
                NotificationListenerService.requestRebind(componentName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
