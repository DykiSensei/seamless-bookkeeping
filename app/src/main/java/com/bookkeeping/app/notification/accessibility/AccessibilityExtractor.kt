package com.bookkeeping.app.notification.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bookkeeping.app.data.local.entity.TransactionEntity

// 一个 Extractor 负责一种"页面 → 交易"的识别。
// 例如 AlipayTransferExtractor 只识别"支付宝转账详情页"并产出交易。
//
// 返回 null 表示当前页面不匹配 / 信息不足，跳过即可。
interface AccessibilityExtractor {

    // 限定包名：服务在分发事件时已按 packageName 路由，这里只是声明用途
    val packageName: String

    fun extract(
        root: AccessibilityNodeInfo,
        event: AccessibilityEvent,
    ): TransactionEntity?
}
