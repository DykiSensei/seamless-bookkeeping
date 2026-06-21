package com.bookkeeping.app.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionType
import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

// 统计页 ViewModel：本月各分类的支出聚合
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    // 收入还是支出（切换显示哪个方向的统计）
    private val _viewingType = MutableStateFlow(TransactionType.EXPENSE)
    val viewingType: StateFlow<TransactionType> = _viewingType

    // 本月按分类聚合后的列表（金额降序）
    val categoryStats: StateFlow<List<CategoryStat>> = run {
        val (start, end) = currentMonthRange()
        combine(
            repository.observeBetween(start, end),
            _viewingType,
        ) { transactions, type ->
            // 1. 按 type 过滤
            val filtered = transactions.filter { it.type == type.name }
            if (filtered.isEmpty()) return@combine emptyList()

            // 2. 按 category 分组聚合
            val total = filtered.sumOf { it.amountCents }
            filtered
                .groupBy { runCatching { TransactionCategory.valueOf(it.category) }.getOrDefault(TransactionCategory.OTHER) }
                .map { (cat, list) ->
                    val catTotal = list.sumOf { it.amountCents }
                    CategoryStat(
                        category = cat,
                        totalCents = catTotal,
                        count = list.size,
                        percentage = if (total > 0) catTotal.toFloat() / total else 0f,
                    )
                }
                .sortedByDescending { it.totalCents }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    // 本月该方向的总金额（分）
    val totalCents: StateFlow<Long> = categoryStats
        .map { list -> list.sumOf { it.totalCents } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setViewingType(type: TransactionType) {
        _viewingType.value = type
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
