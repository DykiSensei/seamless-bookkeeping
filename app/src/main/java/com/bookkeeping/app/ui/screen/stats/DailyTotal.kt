package com.bookkeeping.app.ui.screen.stats

// 单日合计（每日柱状图用）
data class DailyTotal(
    val day: Int,          // 月内第几天 1..31
    val totalCents: Long,  // 当日合计（按当前 viewingType）
)
