package com.bookkeeping.app.notification.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType

// 微信红包：用户点开红包后展示的"恭喜发财"/"红包详情"页面。
// 第一版策略：
//   - 页面含「红包」
//   - 含「已存入零钱」/「拆到」/「领取」等收入语义
//   - 提取金额
class WechatRedPacketExtractor : AccessibilityExtractor {

    override val packageName: String = AccessibilityCaptureService.PKG_WECHAT

    override fun extract(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): TransactionEntity? {
        val allText = root.collectAllText()
        if (allText.isEmpty()) return null

        if (!allText.contains("红包")) return null

        val incomeHit = INCOME_HINTS.any { allText.contains(it) }
        if (!incomeHit) return null

        if (EXPENSE_NEGATIVES.any { allText.contains(it) }) return null

        val amount = extractAmountYuan(allText) ?: return null
        if (amount <= 0) return null

        return TransactionEntity(
            amountCents = (amount * 100).toLong(),
            type = TransactionType.INCOME.name,
            // 红包没有更合适的分类，归到 OTHER；用户可手动改
            category = TransactionCategory.OTHER.name,
            merchant = "",
            note = "[无障碍] 微信红包",
            source = TransactionSource.WECHAT.name,
            account = "微信",
            timestamp = System.currentTimeMillis(),
        )
    }

    companion object {
        private val INCOME_HINTS = listOf(
            "已存入零钱", "已存入", "拆到", "领取了", "领到", "微信红包",
        )
        // 自己发出的红包页面也带金额，需排除
        private val EXPENSE_NEGATIVES = listOf(
            "发出红包", "塞钱进红包", "支付成功",
        )
    }
}
