package com.bookkeeping.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// 监听全系统通知。当支付宝/微信发出支付成功通知时被回调。
// 嵌入式类比：注册了一个"中断处理函数"，系统在通知到达时调用我们。
class PaymentNotificationListenerService : NotificationListenerService() {

    private lateinit var repository: TransactionRepository

    // 后台协程作用域。Service 销毁时一起 cancel。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Hilt EntryPoint 方式拿到 Repository（@AndroidEntryPoint 不支持本类）
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationListenerEntryPoint::class.java
        )
        repository = entryPoint.transactionRepository()
        Log.d(TAG, "Service created")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // 通知到达时回调（关键入口）
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()

        Log.d(TAG, "Notification from $packageName: title=$title text=$text")

        val parsed = NotificationParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            timestampMs = sbn.postTime,
        ) ?: return

        // 在后台协程里入库（IO 不能在主线程）
        scope.launch {
            // 去重：60 秒内同来源同金额的当作重复，跳过
            // 简化做法：直接 insert。后续可加重复检测。
            val id = repository.insert(parsed)
            Log.i(TAG, "Auto-captured transaction id=$id amount=${parsed.amountCents} source=${parsed.source}")
        }
    }

    companion object {
        private const val TAG = "BkNotifListener"
    }
}
