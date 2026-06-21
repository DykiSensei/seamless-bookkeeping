package com.bookkeeping.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bookkeeping.app.MainActivity
import com.bookkeeping.app.R

// 前台保活服务。
//
// 用途：在国产 ROM 上，系统会激进地杀掉后台进程，导致 NotificationListenerService
// 被解绑且不会重启，自动抓账失效。前台服务挂一个常驻通知，能显著降低被杀概率。
//
// 注意：本 Service 自身不做任何业务。真正的抓取逻辑在
// PaymentNotificationListenerService 和 SmsBroadcastReceiver。它只是"占位"，让系统认为
// app 处于活跃状态、保留 app 进程，从而保住通知监听服务。
//
// 默认关闭，用户在设置页主动开启。
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        val notification = buildNotification(this)

        // Android 10+ (API 29) 启动前台服务需要明确传 foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        // 进程被杀后系统应尝试重启
        return START_STICKY
    }

    private fun buildNotification(context: Context): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, openIntent, pendingFlags)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("无感记账正在运行")
            .setContentText("保持后台抓取支付宝/微信/银行短信")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }

        // 创建通知 channel。重复调用安全（系统对同 id 是幂等的）。
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "后台保活",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "保持自动抓账服务在后台运行"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
