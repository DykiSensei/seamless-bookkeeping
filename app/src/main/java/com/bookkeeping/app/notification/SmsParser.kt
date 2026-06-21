package com.bookkeeping.app.notification

import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType

// 解析银行短信文本，提取金额、收支方向、银行、卡尾号。
//
// 典型样本：
//   【工商银行】您尾号1234的工银信用卡，于06月15日18:30消费人民币25.00元，余额1234.56元
//   【招商银行】您尾号1234招行一卡通06月15日15:20消费人民币25.00元
//   【建设银行】您尾号5678账户12月25日 工资到账人民币 8000.00元
//   【中国银行】您尾号9876账户 06月15日 转入人民币 500.00元
//
// 设计原则：不能确定收支方向的短信跳过（避免把验证码、广告短信误入库）。
object SmsParser {

    // 银行简称 → 标准全称（用于提取银行名 + 区分非银行短信）
    // 顺序：长的在前，避免 "工行" 命中前 "工商银行" 没匹配上
    private val BANK_NAMES = linkedMapOf(
        "工商银行" to "工商银行",
        "招商银行" to "招商银行",
        "建设银行" to "建设银行",
        "中国银行" to "中国银行",
        "农业银行" to "农业银行",
        "交通银行" to "交通银行",
        "邮政储蓄银行" to "邮政储蓄银行",
        "邮储银行" to "邮政储蓄银行",
        "中信银行" to "中信银行",
        "光大银行" to "光大银行",
        "民生银行" to "民生银行",
        "浦发银行" to "浦发银行",
        "平安银行" to "平安银行",
        "广发银行" to "广发银行",
        "兴业银行" to "兴业银行",
        "华夏银行" to "华夏银行",
        "工行" to "工商银行",
        "招行" to "招商银行",
        "建行" to "建设银行",
        "中行" to "中国银行",
        "农行" to "农业银行",
        "交行" to "交通银行",
    )

    // 金额：要求带 "元"，可选 "人民币" 前缀。避免误匹配日期数字
    private val AMOUNT_REGEX = Regex("""(?:人民币)?\s*([0-9]+(?:\.[0-9]{1,2})?)\s*元""")

    // 卡尾号 / 账户尾号
    private val CARD_NO_REGEX = Regex("""尾号\s*(\d{4})""")

    // 收入关键词
    private val INCOME_KEYWORDS = listOf(
        "存入", "转入", "到账", "到帐",
        "退款", "返款", "返现",
        "工资", "薪资", "薪水",
        "收入", "收款",
        "代发", "汇入", "汇款",
    )

    // 支出关键词
    private val EXPENSE_KEYWORDS = listOf(
        "消费", "支出", "支付",
        "扣款", "扣除", "扣费",
        "转出", "汇出",
        "取款", "ATM取款", "ATM 取款", "提现",
        "缴费", "还款",  // 还款是支出
    )

    fun parse(sender: String?, body: String, timestampMs: Long): TransactionEntity? {
        if (body.isBlank()) return null

        // 1. 识别银行（不是银行短信直接返回 null）
        val bank = detectBank(body) ?: return null

        // 2. 提金额
        val amountYuan = extractAmount(body) ?: return null
        if (amountYuan <= 0) return null

        // 3. 判收支方向。无法确定时返回 null（避免误入库验证码等）
        val type = when {
            INCOME_KEYWORDS.any { body.contains(it) } -> TransactionType.INCOME
            EXPENSE_KEYWORDS.any { body.contains(it) } -> TransactionType.EXPENSE
            else -> return null
        }

        // 4. 提卡尾号，组装账户名
        val cardLast4 = CARD_NO_REGEX.find(body)?.groupValues?.get(1).orEmpty()
        val account = if (cardLast4.isNotEmpty()) "$bank ($cardLast4)" else bank

        // 5. 自动分类（基于短信正文：能识别"工资到账"→SALARY、"加油"→TRANSPORT 等）
        val category = CategoryClassifier.classify(merchant = "", contextText = body)

        return TransactionEntity(
            amountCents = (amountYuan * 100).toLong(),
            type = type.name,
            category = category.name,
            merchant = "",
            note = "[银行短信] " + body.take(80),
            source = TransactionSource.BANK_SMS.name,
            account = account,
            timestamp = timestampMs,
        )
    }

    private fun detectBank(body: String): String? {
        for ((keyword, fullName) in BANK_NAMES) {
            if (body.contains(keyword)) return fullName
        }
        return null
    }

    private fun extractAmount(body: String): Double? {
        // 找所有金额，取第一个（通常第一个就是交易金额，"余额"在后面）
        return AMOUNT_REGEX.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
