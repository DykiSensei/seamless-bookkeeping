package com.bookkeeping.app.notification

import android.content.Context
import android.content.Intent
import android.provider.Settings

// 通知监听权限的工具类
// Android 要求 NotificationListenerService 必须由用户在系统设置里手动授权。
object NotificationListenerHelper {

    // 检查当前 app 的 NotificationListenerService 是否被授权
    // 实现：读系统 secure setting "enabled_notification_listeners"，看里面有没有我们的包名
    fun isEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat != null && flat.split(":").any { it.startsWith("$packageName/") }
    }

    // 跳到系统的"通知使用权"设置页，让用户去打开开关
    fun openSettings(context: Context) {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
