package com.bookkeeping.app.ui.screen.stats

import com.bookkeeping.app.data.local.entity.TransactionCategory

// 单个分类的统计聚合数据
data class CategoryStat(
    val category: TransactionCategory,
    val totalCents: Long,  // 该分类的总金额（"分"为单位）
    val count: Int,        // 笔数
    val percentage: Float, // 占总数的比例 [0..1]
)
