package com.bookkeeping.app.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// SMS 权限工具：检查 RECEIVE_SMS / READ_SMS 是否已授权
object SmsPermissionHelper {

    // 我们需要的两个 SMS 权限
    // RECEIVE_SMS：实时接收新到的短信广播
    // READ_SMS：读取已存在的短信（可选，但很多 ROM 要一起授）
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
    )

    fun isGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
