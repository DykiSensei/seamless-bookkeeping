package com.bookkeeping.app.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType
import com.bookkeeping.app.data.repository.TransactionRepository
import com.bookkeeping.app.notification.NotificationParser
import com.bookkeeping.app.notification.SmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    // 当前选中的分类筛选；null = 不筛选（显示全部）
    private val _categoryFilter = MutableStateFlow<TransactionCategory?>(null)
    val categoryFilter: StateFlow<TransactionCategory?> = _categoryFilter

    // 原始所有交易（数据库 → Flow → StateFlow）
    private val allTransactions: StateFlow<List<TransactionEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 过滤后用于显示的交易列表 = allTransactions × categoryFilter
    // combine 把两个 Flow 合并：任一变化都触发重新计算
    // 嵌入式类比：两个输入信号通过组合逻辑产生新的输出
    val displayedTransactions: StateFlow<List<TransactionEntity>> =
        combine(allTransactions, _categoryFilter) { list, filter ->
            if (filter == null) list else list.filter { it.category == filter.name }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 本月支出/收入：注意基于全部数据（不受 filter 影响）
    val monthlyExpenseCents: StateFlow<Long> = run {
        val (start, end) = currentMonthRange()
        repository.observeTotalExpenseBetween(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    }

    val monthlyIncomeCents: StateFlow<Long> = run {
        val (start, end) = currentMonthRange()
        repository.observeTotalIncomeBetween(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _categoryFilter.value = category
    }

    fun addTransaction(
        amountYuan: Double,
        type: TransactionType,
        category: TransactionCategory,
        merchant: String,
        note: String,
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                amountCents = (amountYuan * 100).toLong(),
                type = type.name,
                category = category.name,
                merchant = merchant,
                note = note,
                source = TransactionSource.MANUAL.name,
                timestamp = System.currentTimeMillis(),
            )
            repository.insert(entity)
        }
    }

    fun delete(entity: TransactionEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }

    // DEBUG ONLY：模拟收到一条通知，走 Parser → Repository 这条完整链路。
    // 用于在模拟器上验证 Parser 解析规则，不依赖真实的 NotificationListenerService。
    fun simulateNotification(packageName: String, title: String, text: String) {
        val parsed = NotificationParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            timestampMs = System.currentTimeMillis(),
        ) ?: return
        viewModelScope.launch { repository.insert(parsed) }
    }

    // DEBUG ONLY：模拟收到一条银行短信
    fun simulateSms(sender: String, body: String) {
        val parsed = SmsParser.parse(
            sender = sender,
            body = body,
            timestampMs = System.currentTimeMillis(),
        ) ?: return
        viewModelScope.launch { repository.insert(parsed) }
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }
}
