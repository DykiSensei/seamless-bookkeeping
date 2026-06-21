package com.bookkeeping.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bookkeeping.app.data.settings.SettingsPreferences
import com.bookkeeping.app.notification.KeepAliveService
import com.bookkeeping.app.ui.navigation.RootNavigation
import com.bookkeeping.app.ui.theme.BookkeepingAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// @AndroidEntryPoint = 让 Hilt 能给这个 Activity 及其下属 Composable 注入依赖
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsPreferences

    // Android 13+ 必须运行时申请 POST_NOTIFICATIONS，否则前台服务的常驻通知不可见
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 结果不需要处理：拒绝就只是看不到常驻通知，不影响其他功能 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        // 如果用户开了「常驻通知保活」就启动前台服务。
        // Android 12+ 限制后台启动前台服务，这里在 Activity onCreate 是合法时机。
        if (settings.keepAliveEnabledBlocking()) {
            KeepAliveService.start(this)
        }

        setContent {
            BookkeepingAppTheme {
                RootNavigation()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
