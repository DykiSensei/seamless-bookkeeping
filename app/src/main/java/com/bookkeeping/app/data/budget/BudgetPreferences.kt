package com.bookkeeping.app.data.budget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bookkeeping.app.data.local.entity.TransactionCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore 委托必须放在 file top level（不能在 class 里）
private val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_prefs")

// 预算的持久化层，基于 DataStore Preferences
@Singleton
class BudgetPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun observe(): Flow<BudgetSettings> = context.budgetDataStore.data.map { prefs ->
        val total = prefs[KEY_MONTHLY_TOTAL] ?: 0L
        val perCategory = TransactionCategory.entries
            .filter { it != TransactionCategory.SALARY && it != TransactionCategory.TRANSFER }  // 收入类不设预算
            .mapNotNull { cat ->
                val v = prefs[longKey(cat)] ?: 0L
                if (v > 0) cat to v else null
            }
            .toMap()
        BudgetSettings(monthlyTotalCents = total, perCategory = perCategory)
    }

    suspend fun setMonthlyTotal(cents: Long) {
        context.budgetDataStore.edit { prefs ->
            if (cents > 0) prefs[KEY_MONTHLY_TOTAL] = cents
            else prefs.remove(KEY_MONTHLY_TOTAL)
        }
    }

    suspend fun setCategoryBudget(category: TransactionCategory, cents: Long) {
        context.budgetDataStore.edit { prefs ->
            val key = longKey(category)
            if (cents > 0) prefs[key] = cents else prefs.remove(key)
        }
    }

    private fun longKey(cat: TransactionCategory) = longPreferencesKey("cat_${cat.name}")

    companion object {
        private val KEY_MONTHLY_TOTAL = longPreferencesKey("monthly_total")
    }
}
