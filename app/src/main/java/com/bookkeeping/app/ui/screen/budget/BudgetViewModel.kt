package com.bookkeeping.app.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookkeeping.app.data.budget.BudgetPreferences
import com.bookkeeping.app.data.budget.BudgetSettings
import com.bookkeeping.app.data.local.entity.TransactionCategory
import com.bookkeeping.app.data.local.entity.TransactionType
import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetPrefs: BudgetPreferences,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val monthRange: Pair<Long, Long> = currentMonthRange()

    val budget: StateFlow<BudgetSettings> = budgetPrefs.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetSettings())

    // 本月各分类的支出（cents）。只统计 EXPENSE。
    val expenseByCategory: StateFlow<Map<TransactionCategory, Long>> =
        transactionRepository.observeBetween(monthRange.first, monthRange.second)
            .map { txs ->
                txs.filter { it.type == TransactionType.EXPENSE.name }
                    .groupBy {
                        runCatching { TransactionCategory.valueOf(it.category) }
                            .getOrDefault(TransactionCategory.OTHER)
                    }
                    .mapValues { (_, list) -> list.sumOf { it.amountCents } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // 本月总支出
    val totalExpenseCents: StateFlow<Long> = expenseByCategory
        .map { it.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setMonthlyTotal(yuan: Double) {
        viewModelScope.launch {
            budgetPrefs.setMonthlyTotal((yuan * 100).toLong())
        }
    }

    fun setCategoryBudget(category: TransactionCategory, yuan: Double) {
        viewModelScope.launch {
            budgetPrefs.setCategoryBudget(category, (yuan * 100).toLong())
        }
    }

    fun removeCategoryBudget(category: TransactionCategory) {
        viewModelScope.launch {
            budgetPrefs.setCategoryBudget(category, 0L)
        }
    }

    // 本月还剩几天（含今天）
    fun daysRemaining(): Int {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return lastDay - today + 1
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
