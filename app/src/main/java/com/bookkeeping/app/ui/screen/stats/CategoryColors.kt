package com.bookkeeping.app.ui.screen.stats

import androidx.compose.ui.graphics.Color
import com.bookkeeping.app.data.local.entity.TransactionCategory

// 每个分类的固定颜色，统一饼图、柱状图、列表标记的视觉
// 采用 Material Design 调色板里饱和度中等的色调，避免过艳
private val PALETTE: Map<TransactionCategory, Color> = mapOf(
    TransactionCategory.FOOD          to Color(0xFFE57373),  // 红 - 餐饮
    TransactionCategory.TRANSPORT     to Color(0xFF4FC3F7),  // 蓝 - 交通
    TransactionCategory.SHOPPING      to Color(0xFFBA68C8),  // 紫 - 购物
    TransactionCategory.ENTERTAINMENT to Color(0xFFF06292),  // 粉 - 娱乐
    TransactionCategory.HOUSING       to Color(0xFF7986CB),  // 靛 - 住房
    TransactionCategory.UTILITIES     to Color(0xFF4DB6AC),  // 青 - 水电
    TransactionCategory.HEALTH        to Color(0xFFA1887F),  // 棕 - 医疗
    TransactionCategory.EDUCATION     to Color(0xFFFFB74D),  // 橙 - 教育
    TransactionCategory.SALARY        to Color(0xFFAED581),  // 绿 - 工资
    TransactionCategory.TRANSFER      to Color(0xFF9575CD),  // 深紫 - 转账
    TransactionCategory.OTHER         to Color(0xFF90A4AE),  // 灰 - 其他
)

fun categoryColor(category: TransactionCategory): Color =
    PALETTE[category] ?: Color(0xFF90A4AE)
