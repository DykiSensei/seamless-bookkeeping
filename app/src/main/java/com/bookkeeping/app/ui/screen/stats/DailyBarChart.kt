package com.bookkeeping.app.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column

// 简单日柱状图：横轴是月内日期，纵轴是当日合计金额。
// 用 Compose Canvas 直接画。
@Composable
fun DailyBarChart(
    dailyTotals: List<DailyTotal>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    if (dailyTotals.isEmpty()) return

    val maxCents = dailyTotals.maxOfOrNull { it.totalCents }?.coerceAtLeast(1L) ?: 1L
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier) {
        // 顶部刻度文字
        Text(
            text = "最高 ¥${"%.2f".format(maxCents / 100.0)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp)
        ) {
            val n = dailyTotals.size
            val chartW = size.width
            val chartH = size.height - 16.dp.toPx()  // 底部留 16dp 给日期文字
            val gap = 2.dp.toPx()
            val barW = (chartW - gap * (n - 1)) / n

            // 底部基线
            drawLine(
                color = outlineColor,
                start = Offset(0f, chartH),
                end = Offset(chartW, chartH),
                strokeWidth = 1f,
            )

            dailyTotals.forEachIndexed { index, stat ->
                val x = index * (barW + gap)
                val h = (stat.totalCents.toFloat() / maxCents.toFloat()) * chartH
                if (h > 0) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, chartH - h),
                        size = Size(barW, h),
                    )
                }

                // 每周一画一次日期标签（1/8/15/22/29...）
                if ((stat.day - 1) % 7 == 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(
                            stat.day.toString(),
                            x + barW / 2f,
                            size.height,
                            paint,
                        )
                    }
                }
            }
        }
    }
}
