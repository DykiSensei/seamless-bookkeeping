package com.bookkeeping.app.ui.screen.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookkeeping.app.data.local.entity.TransactionCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val budget by viewModel.budget.collectAsState()
    val expenseByCategory by viewModel.expenseByCategory.collectAsState()
    val totalExpense by viewModel.totalExpenseCents.collectAsState()
    val daysRemaining = remember { viewModel.daysRemaining() }

    // 对话框状态
    var showEditTotal by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("预算") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 总预算卡片
            item(key = "total_budget") {
                TotalBudgetCard(
                    budgetCents = budget.monthlyTotalCents,
                    spentCents = totalExpense,
                    daysRemaining = daysRemaining,
                    onEdit = { showEditTotal = true },
                )
            }

            // 分类预算分组标题
            item(key = "category_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "分类预算",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showAddCategory = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加分类预算")
                    }
                }
            }

            if (budget.perCategory.isEmpty()) {
                item(key = "category_empty") {
                    Text(
                        text = "还没有分类预算。点击右上角 + 添加。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // 各分类预算
                val rows = budget.perCategory.entries.toList()
                items(rows, key = { it.key.name }) { (cat, budgetCents) ->
                    CategoryBudgetCard(
                        category = cat,
                        budgetCents = budgetCents,
                        spentCents = expenseByCategory[cat] ?: 0L,
                        onEdit = { editingCategory = cat },
                        onRemove = { viewModel.removeCategoryBudget(cat) },
                    )
                }
            }
        }
    }

    // 总预算对话框
    if (showEditTotal) {
        EditBudgetDialog(
            title = "设置月预算",
            initialYuan = budget.monthlyTotalCents / 100.0,
            pickCategory = false,
            onDismiss = { showEditTotal = false },
            onConfirm = { _, yuan ->
                viewModel.setMonthlyTotal(yuan)
                showEditTotal = false
            },
        )
    }

    // 添加分类预算对话框
    if (showAddCategory) {
        EditBudgetDialog(
            title = "添加分类预算",
            pickCategory = true,
            excludeCategories = budget.perCategory.keys,
            onDismiss = { showAddCategory = false },
            onConfirm = { cat, yuan ->
                if (cat != null) viewModel.setCategoryBudget(cat, yuan)
                showAddCategory = false
            },
        )
    }

    // 编辑某分类预算对话框
    editingCategory?.let { cat ->
        EditBudgetDialog(
            title = "${cat.displayName} 预算",
            initialYuan = (budget.perCategory[cat] ?: 0L) / 100.0,
            pickCategory = false,
            initialCategory = cat,
            onDismiss = { editingCategory = null },
            onConfirm = { _, yuan ->
                viewModel.setCategoryBudget(cat, yuan)
                editingCategory = null
            },
        )
    }
}

@Composable
private fun TotalBudgetCard(
    budgetCents: Long,
    spentCents: Long,
    daysRemaining: Int,
    onEdit: () -> Unit,
) {
    val hasBudget = budgetCents > 0
    val ratio = if (hasBudget) (spentCents.toFloat() / budgetCents.toFloat()) else 0f
    val remainingCents = (budgetCents - spentCents).coerceAtLeast(0L)
    val overSpent = spentCents > budgetCents && hasBudget

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = onEdit,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "本月总预算",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            if (!hasBudget) {
                Text(
                    text = "未设置，点击此处设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    text = "¥${"%.2f".format(spentCents / 100.0)} / ¥${"%.2f".format(budgetCents / 100.0)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(vertical = 4.dp),
                    color = progressColor(ratio),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = if (overSpent) "已超支 ¥${"%.2f".format((spentCents - budgetCents) / 100.0)}"
                               else "剩余 ¥${"%.2f".format(remainingCents / 100.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overSpent) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "剩 $daysRemaining 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    category: TransactionCategory,
    budgetCents: Long,
    spentCents: Long,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val ratio = if (budgetCents > 0) (spentCents.toFloat() / budgetCents.toFloat()) else 0f
    val overSpent = spentCents > budgetCents

    Card(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "¥${"%.2f".format(spentCents / 100.0)} / ¥${"%.2f".format(budgetCents / 100.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (overSpent) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(top = 4.dp),
                color = progressColor(ratio),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
            if (overSpent) {
                Text(
                    text = "已超支 ¥${"%.2f".format((spentCents - budgetCents) / 100.0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

// 进度条颜色：< 80% 主题色，80-100% 黄，> 100% 红
@Composable
private fun progressColor(ratio: Float): Color = when {
    ratio < 0.8f -> MaterialTheme.colorScheme.primary
    ratio < 1.0f -> Color(0xFFFB8C00)  // 橙
    else -> MaterialTheme.colorScheme.error
}
