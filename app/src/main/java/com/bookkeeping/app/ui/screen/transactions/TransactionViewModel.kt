package com.bookkeeping.app.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionEntity
import com.bookkeeping.app.data.local.entity.TransactionSource
import com.bookkeeping.app.data.local.entity.TransactionType
import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ViewModel：UI 的"状态机"。
// 屏幕旋转/进入后台时 ViewModel 不会被销毁，所以数据不丢失。
// @HiltViewModel 让 Hilt 自动注入 Repository。
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    // 所有交易（按时间倒序）。Compose 会订阅这个 StateFlow，数据库变化时自动重组 UI。
    val transactions: StateFlow<List<TransactionEntity>> = repository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),  // 没人订阅 5s 后停止上游
            initialValue = emptyList()
        )

    // 本月总支出（单位：分）
    val monthlyExpenseCents: StateFlow<Long> = run {
        val (start, end) = currentMonthRange()
        repository.observeTotalExpenseBetween(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    }

    // 本月总收入（单位：分）
    val monthlyIncomeCents: StateFlow<Long> = run {
        val (start, end) = currentMonthRange()
        repository.observeTotalIncomeBetween(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    }

    // 新建一笔交易（被 AddTransactionDialog 调用）
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

    // 返回 [本月第一天 00:00, 下月第一天 00:00) 的毫秒时间戳
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
