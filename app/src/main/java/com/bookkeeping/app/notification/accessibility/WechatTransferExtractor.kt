package com.bookkeeping.app.notification.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType

// 微信转账：用户在聊天里收到"转账消息卡片"或点开后进入"转账详情页"时触发。
//
// 微信新版个人转账不发系统通知，必须靠这里兜底。
// 第一版策略：
//   - 页面含「转账」字样
//   - 含「已收钱」/「已存入零钱」/「待确认」等收入语义
//   - 提取金额
//
// 局限：聊天列表里只是一条卡片预览，可能 text 里没有完整金额，会漏抓——
// 那种情况要求用户点开转账详情页才能稳定抓到，第一版接受这个限制。
class WechatTransferExtractor : AccessibilityExtractor {

    override val packageName: String = AccessibilityCaptureService.PKG_WECHAT

    override fun extract(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): TransactionEntity? {
        val allText = root.collectAllText()
        if (allText.isEmpty()) return null

        if (!allText.contains("转账")) return null
        // 排除红包页面（让 RedPacketExtractor 处理）
        if (allText.contains("红包")) return null

        val incomeHit = INCOME_HINTS.any { allText.contains(it) }
        if (!incomeHit) return null

        if (EXPENSE_NEGATIVES.any { allText.contains(it) }) return null

        val amount = extractAmountYuan(allText) ?: return null
        if (amount <= 0) return null

        return TransactionEntity(
            amountCents = (amount * 100).toLong(),
            type = TransactionType.INCOME.name,
            category = TransactionCategory.TRANSFER.name,
            merchant = "",
            note = "[无障碍] 微信转账入账",
            source = TransactionSource.WECHAT.name,
            account = "微信",
            timestamp = System.currentTimeMillis(),
        )
    }

    companion object {
        private val INCOME_HINTS = listOf(
            "已收钱", "已收款", "已存入零钱", "已存入", "对方已收钱", "转账成功",
            "待确认收款", "请收钱", "收钱",
        )
        private val EXPENSE_NEGATIVES = listOf(
            "转账给", "你发起", "等待对方", "未领取",
        )
    }
}
