package com.bookkeeping.app.data.budget

import com.bookkeeping.app.data.local.entity.TransactionCategory

// 用户配置的预算（月维度）
data class BudgetSettings(
    val monthlyTotalCents: Long = 0L,   // 总月预算（0 = 未设置）
    val perCategory: Map<TransactionCategory, Long> = emptyMap(),  // 各分类月预算（只含已设置的）
)
