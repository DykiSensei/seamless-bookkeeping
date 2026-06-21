package com.bookkeeping.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

// 接收 SMS_RECEIVED 广播，解析银行短信入库。
// Hilt 直接支持 BroadcastReceiver：@AndroidEntryPoint 就能 @Inject。
//
// 嵌入式类比：注册"短信到达"中断的 ISR。
@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TransactionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // 一条短信可能因长度被拆成多条 PDU，Telephony 帮我们合并
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // 拼接同一条短信的所有 part
        val sender = messages.first().originatingAddress
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val timestamp = messages.first().timestampMillis

        Log.d(TAG, "SMS from $sender: $fullBody")

        val parsed = SmsParser.parse(sender, fullBody, timestamp) ?: return

        scope.launch {
            val id = repository.insert(parsed)
            Log.i(TAG, "Auto-captured bank SMS: id=$id amount=${parsed.amountCents} account=${parsed.account}")
        }
    }

    companion object {
        private const val TAG = "BkSmsReceiver"
    }
}
