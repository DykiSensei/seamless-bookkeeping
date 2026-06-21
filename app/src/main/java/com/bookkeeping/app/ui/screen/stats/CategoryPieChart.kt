package com.bookkeeping.app.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 简单饼图：基于 CategoryStat 列表绘制各分类占比。
// 用 Compose Canvas 直接画，不依赖第三方图表库。
@Composable
fun CategoryPieChart(
    stats: List<CategoryStat>,
    modifier: Modifier = Modifier,
) {
    if (stats.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左：饼图本体
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val total = stats.sumOf { it.totalCents }
                if (total <= 0) return@Canvas
                val diameter = size.minDimension
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f  // 12 点方向起始
                stats.forEach { stat ->
                    val sweep = (stat.totalCents.toFloat() / total.toFloat()) * 360f
                    drawArc(
                        color = categoryColor(stat.category),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                    startAngle += sweep
                }
            }
        }

        // 右：图例（最多显示前 6 个，剩余合并）
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            stats.take(6).forEach { stat ->
                LegendItem(
                    color = categoryColor(stat.category),
                    label = stat.category.displayName,
                    percentage = stat.percentage,
                )
            }
            if (stats.size > 6) {
                val restPct = stats.drop(6).sumOf { it.percentage.toDouble() }.toFloat()
                LegendItem(color = Color.Gray, label = "其他", percentage = restPct)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, percentage: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = " $label",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${"%.1f".format(percentage * 100)}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

