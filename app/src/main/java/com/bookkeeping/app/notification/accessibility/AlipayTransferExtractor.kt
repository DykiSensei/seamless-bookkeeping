package com.bookkeeping.app.notification.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType

// 支付宝转账详情页抽取。
//
// 用户从通知"某某给你转了一笔钱"点击进入后会到这个页面，页面包含金额、对方姓名。
// 第一版策略：
//   - 当前包名 com.eg.android.AlipayGphone
//   - 页面文本同时含「转账」+ 收款语义（"已收钱"/"已存入"/"已到账"等）
//   - 提取首个 "X.XX 元" 或 "￥X.XX" 作为金额
//
// 局限：支付宝有「转账给好友」(支出) 和 "收到转账"(收入)两种详情页，UI 结构相近。
// 这里只识别收入侧（无金额通知场景）。支出有付款成功通知，不需要兜底。
class AlipayTransferExtractor : AccessibilityExtractor {

    override val packageName: String = AccessibilityCaptureService.PKG_ALIPAY

    override fun extract(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): TransactionEntity? {
        val allText = root.collectAllText()
        if (allText.isEmpty()) return null

        // 必要条件：含「转账」字样
        if (!allText.contains("转账")) return null

        // 收入信号：明确收到的措辞
        val incomeHit = INCOME_HINTS.any { allText.contains(it) }
        if (!incomeHit) return null

        // 反向排除：如果同时含明确支出语义则放弃
        if (EXPENSE_NEGATIVES.any { allText.contains(it) }) return null

        val amount = extractAmountYuan(allText) ?: return null
        if (amount <= 0) return null

        val merchant = guessCounterpart(allText)

        return TransactionEntity(
            amountCents = (amount * 100).toLong(),
            type = TransactionType.INCOME.name,
            category = TransactionCategory.TRANSFER.name,
            merchant = merchant,
            note = "[无障碍] 支付宝转账入账",
            source = TransactionSource.ALIPAY.name,
            account = "支付宝",
            timestamp = System.currentTimeMillis(),
        )
    }

    // 在前缀关键词后找 2~10 字的对方称谓；找不到返回空。
    private fun guessCounterpart(text: String): String {
        for (re in COUNTERPART_PATTERNS) {
            val m = re.find(text) ?: continue
            val name = m.groupValues[1].trim()
            if (name.length in 2..20) return name
        }
        return ""
    }

    companion object {
        private val INCOME_HINTS = listOf(
            "已收钱", "已收款", "收款成功", "成功收款", "已到账", "已存入", "存入余额宝",
            "转账已收", "对方已转账",
        )
        // 主动转账给别人 → 不属于这里要抓的"入账兜底"
        private val EXPENSE_NEGATIVES = listOf(
            "转账给", "向对方转账", "付款成功", "支付成功",
        )
        private val COUNTERPART_PATTERNS = listOf(
            Regex("""来自\s*([一-龥A-Za-z0-9*·]{2,20})"""),
            Regex("""([一-龥A-Za-z0-9*·]{2,20})\s*给你转""",),
            Regex("""([一-龥A-Za-z0-9*·]{2,20})\s*转给你"""),
            Regex("""对方\s*[:：]?\s*([一-龥A-Za-z0-9*·]{2,20})"""),
        )
    }
}
