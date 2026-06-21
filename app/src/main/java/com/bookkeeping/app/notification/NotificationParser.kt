package com.bookkeeping.app.notification

import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType

// 解析支付宝 / 微信通知文本，提取金额、收支方向、商户名。
//
// 真实通知样本（调研自 v2ex / AutoAccountingOrg issues / CFANZ）：
//   - 支付宝个人收款：  title="支付宝支付"   text="成功收款 1.00 元。享免费提现等更多专属服务..."
//   - 支付宝付款成功：  title="付款成功"    text="付款成功￥3.00 (AB)湖北武汉黄陂汉口北服装城美宜佳"
//   - 支付宝退款：     title="支付宝"     text="收到退款 X.XX 元"
//   - 微信主动付款：   title="微信支付"   text="向 麦当劳 付款 12.30 元"
//   - 微信收到转账：   title="微信支付"   text="向你付款 100.00 元"  (别人付给你)
//   - 微信收转账：     text="收到 张三 的转账 100.00 元"
//
// ⚠️ 局限性：微信扫码收款从 2020 后不再发系统通知（改 app 内提示），本 Parser 无法覆盖该场景。
// 若想抓扫码收款，需要 AccessibilityService。详见 memory/project_notification_quirks。
object NotificationParser {

    private const val PKG_ALIPAY = "com.eg.android.AlipayGphone"
    private const val PKG_WECHAT = "com.tencent.mm"

    // 金额提取：只匹配带 ￥/¥ 前缀或后缀"元"的数字，避免误匹配日期等
    private val AMOUNT_REGEX_SYMBOL = Regex("""[¥￥]\s*([0-9]+(?:\.[0-9]{1,2})?)""")
    private val AMOUNT_REGEX_YUAN = Regex("""([0-9]+(?:\.[0-9]{1,2})?)\s*元""")

    // 收入关键词
    private val INCOME_KEYWORDS = listOf(
        "成功收款", "收款成功", "已收款", "您已收款",
        "到账", "已到账", "到帐",
        "收款", "收款￥", "入账",
        "收到转账", "收到红包", "转账到账",
        "向你付款", "向您付款",  // 别人付给你（微信）
        "退款", "返现",
    )

    // 支出关键词
    private val EXPENSE_KEYWORDS = listOf(
        "付款成功", "成功付款", "已付款", "您已付款",
        "支付成功", "已支付", "您已支付",
        "扣款", "扣费", "已扣款",
        "消费",
    )

    // "向 商户名 付款" 模式（区分于"向你/您 付款"）= 支出
    private val PAY_TO_MERCHANT_REGEX = Regex("""向\s*(?!你|您)(\S+?)\s*付款""")

    // 商户提取模式（按优先级试，命中即用）
    private val MERCHANT_PATTERNS = listOf(
        Regex("""向\s*(?!你|您)(\S+?)\s*付款"""),  // 微信：向 麦当劳 付款（排除"向你/您"）
        Regex("""付款给\s*(\S+?)(?:[\s，,。.！!]|$)"""),
        Regex("""付给\s*(\S+?)(?:[\s，,。.！!]|$)"""),
        Regex("""收到\s*(\S+?)\s*的转账"""),     // 微信：收到 张三 的转账
        Regex("""来自\s*(\S+?)(?:[\s，,。.！!]|$)"""),
        Regex("""转账给\s*(\S+?)(?:[\s，,。.！!]|$)"""),
        // 支付宝付款成功后面跟商户："付款成功￥3.00 (AB)湖北武汉..."
        Regex("""[¥￥][0-9.]+\s*[-—]?\s*(\S{2,})"""),
    )

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        timestampMs: Long,
    ): TransactionEntity? {
        val source = when (packageName) {
            PKG_ALIPAY -> TransactionSource.ALIPAY
            PKG_WECHAT -> TransactionSource.WECHAT
            else -> return null
        }
        return parseCore(
            full = "${title.orEmpty()} ${text.orEmpty()}".trim(),
            source = source,
            timestampMs = timestampMs,
        )
    }

    private fun parseCore(
        full: String,
        source: TransactionSource,
        timestampMs: Long,
    ): TransactionEntity? {
        if (full.isBlank()) return null

        // 1. 提金额
        val amountYuan = extractAmount(full) ?: return null
        if (amountYuan <= 0) return null

        // 2. 判收支方向。优先级：
        //    a. INCOME 关键词（包括"向你付款"这种明确信号）
        //    b. EXPENSE 关键词
        //    c. "向 商户 付款"模式（排除"向你/您"）
        //    d. 默认 EXPENSE
        val type = when {
            INCOME_KEYWORDS.any { full.contains(it) } -> TransactionType.INCOME
            EXPENSE_KEYWORDS.any { full.contains(it) } -> TransactionType.EXPENSE
            PAY_TO_MERCHANT_REGEX.containsMatchIn(full) -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }

        // 3. 提商户名
        val merchant = extractMerchant(full)

        return TransactionEntity(
            amountCents = (amountYuan * 100).toLong(),
            type = type.name,
            category = TransactionCategory.OTHER.name,
            merchant = merchant,
            note = "[自动抓取] " + full.take(80),
            source = source.name,
            account = if (source == TransactionSource.ALIPAY) "支付宝" else "微信",
            timestamp = timestampMs,
        )
    }

    private fun extractAmount(text: String): Double? {
        AMOUNT_REGEX_SYMBOL.find(text)?.let {
            return it.groupValues[1].toDoubleOrNull()
        }
        AMOUNT_REGEX_YUAN.find(text)?.let {
            return it.groupValues[1].toDoubleOrNull()
        }
        return null
    }

    private fun extractMerchant(text: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val m = pattern.find(text)
            if (m != null) {
                val merchant = m.groupValues[1].trim()
                if (merchant.length in 2..40 && !merchant.matches(Regex("""[0-9.]+"""))) {
                    return merchant
                }
            }
        }
        return ""
    }
}
