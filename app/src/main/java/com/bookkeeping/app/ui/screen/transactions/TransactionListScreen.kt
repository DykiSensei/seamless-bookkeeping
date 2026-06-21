package com.bookkeeping.app.ui.screen.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionType
import com.bookkeeping.app.notification.NotificationListenerHelper
import com.bookkeeping.app.notification.SmsPermissionHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.displayedTransactions.collectAsState()
    val monthlyExpense by viewModel.monthlyExpenseCents.collectAsState()
    val monthlyIncome by viewModel.monthlyIncomeCents.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // 监听 Activity 生命周期：每次回到前台时重新检查两类权限状态
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationListenerEnabled by remember { mutableStateOf(NotificationListenerHelper.isEnabled(context)) }
    var smsPermissionGranted by remember { mutableStateOf(SmsPermissionHelper.isGranted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationListenerEnabled = NotificationListenerHelper.isEnabled(context)
                smsPermissionGranted = SmsPermissionHelper.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // SMS 权限请求 launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        smsPermissionGranted = results.values.all { it }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("无感记账") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建账单")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 通知监听未授权时显示提示横幅
            if (!notificationListenerEnabled) {
                NotificationPermissionBanner(
                    onClick = { NotificationListenerHelper.openSettings(context) }
                )
            }

            // SMS 权限未授权时显示提示横幅
            if (!smsPermissionGranted) {
                SmsPermissionBanner(
                    onClick = { smsPermissionLauncher.launch(SmsPermissionHelper.REQUIRED_PERMISSIONS) }
                )
            }

            // 顶部统计卡片（基于全月数据，不受 filter 影响）
            MonthlySummaryCard(
                monthlyExpenseCents = monthlyExpense,
                monthlyIncomeCents = monthlyIncome,
            )

            // 分类筛选 chip 行
            CategoryFilterRow(
                selected = selectedCategory,
                onSelect = viewModel::setCategoryFilter,
            )

            if (transactions.isEmpty()) {
                EmptyState(filterActive = selectedCategory != null)
            } else {
                // 按日期分组渲染
                GroupedTransactionList(
                    transactions = transactions,
                    onDelete = viewModel::delete,
                )
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, type, category, merchant, note ->
                viewModel.addTransaction(amount, type, category, merchant, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MonthlySummaryCard(
    monthlyExpenseCents: Long,
    monthlyIncomeCents: Long,
) {
    val balanceCents = monthlyIncomeCents - monthlyExpenseCents
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本月结余",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${if (balanceCents >= 0) "+" else "−"}¥${"%.2f".format(kotlin.math.abs(balanceCents) / 100.0)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SummaryStat(
                    label = "收入",
                    amount = "+¥${"%.2f".format(monthlyIncomeCents / 100.0)}",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    label = "支出",
                    amount = "−¥${"%.2f".format(monthlyExpenseCents / 100.0)}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CategoryFilterRow(
    selected: TransactionCategory?,
    onSelect: (TransactionCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("全部") }
        )
        TransactionCategory.entries.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(if (selected == cat) null else cat) },
                label = { Text(cat.displayName) }
            )
        }
    }
}

@Composable
private fun GroupedTransactionList(
    transactions: List<TransactionEntity>,
    onDelete: (TransactionEntity) -> Unit,
) {
    // 按 dayKey 分组（同一天 = 同一组）。LinkedHashMap 保留插入顺序（即时间倒序）
    val groups = remember(transactions) {
        transactions.groupBy { dayKey(it.timestamp) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { (dayKey, dayItems) ->
            // 每组的头部：日期 + 当日合计
            item(key = "header_$dayKey") {
                DayHeader(dayKey = dayKey, items = dayItems)
            }
            // 该组内的交易项
            items(dayItems, key = { it.id }) { tx ->
                TransactionItem(transaction = tx, onDelete = { onDelete(tx) })
            }
        }
    }
}

@Composable
private fun DayHeader(dayKey: String, items: List<TransactionEntity>) {
    val expense = items.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountCents }
    val income = items.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountCents }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel(dayKey),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (income > 0) {
            Text(
                text = "+¥${"%.2f".format(income / 100.0)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        if (expense > 0) {
            Text(
                text = "−¥${"%.2f".format(expense / 100.0)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
) {
    val type = runCatching { TransactionType.valueOf(transaction.type) }.getOrDefault(TransactionType.EXPENSE)
    val category = runCatching { TransactionCategory.valueOf(transaction.category) }.getOrDefault(TransactionCategory.OTHER)
    val amount = "%.2f".format(transaction.amountCents / 100.0)
    val sign = if (type == TransactionType.INCOME) "+" else "−"
    val amountColor =
        if (type == TransactionType.INCOME) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName +
                        if (transaction.merchant.isNotBlank()) " · ${transaction.merchant}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = timeOnlyFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$sign¥$amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun EmptyState(filterActive: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (filterActive) "该分类下还没有记录" else "还没有账单",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!filterActive) {
                Text(
                    text = "点击右下角 + 添加第一笔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionBanner(onClick: () -> Unit) {
    PermissionBanner(
        title = "未开启通知监听",
        body = "点击此处开启「通知使用权」，App 才能自动抓取支付宝 / 微信付款记录",
        onClick = onClick,
    )
}

@Composable
private fun SmsPermissionBanner(onClick: () -> Unit) {
    PermissionBanner(
        title = "未开启短信权限",
        body = "点击此处授予短信权限，App 才能自动抓取银行交易短信",
        onClick = onClick,
    )
}

@Composable
private fun PermissionBanner(title: String, body: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// === 时间分组辅助 ===

private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val monthDayFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())

// 把毫秒时间戳转成 "yyyy-MM-dd" 字符串作为分组 key
private fun dayKey(timestampMs: Long): String = dayKeyFormat.format(Date(timestampMs))

// 把 dayKey 转成展示用的标签："今天" / "昨天" / "MM月dd日"
private fun dayLabel(dayKey: String): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val todayKey = dayKeyFormat.format(today.time)
    val yesterdayKey = dayKeyFormat.format(Date(today.timeInMillis - 86_400_000L))
    return when (dayKey) {
        todayKey -> "今天"
        yesterdayKey -> "昨天"
        else -> {
            // 解析回 Date，然后格式化为 MM月dd日
            runCatching { monthDayFormat.format(dayKeyFormat.parse(dayKey)!!) }.getOrDefault(dayKey)
        }
    }
}
