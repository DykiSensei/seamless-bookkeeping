package com.bookkeeping.app.data.repository

import com.bookkeeping.app.data.local.dao.TransactionDao
import com.bookkeeping.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// Repository：业务层与数据层的桥梁。
// 上层（ViewModel）只跟 Repository 打交道，不直接接触 DAO/网络/SharedPreferences。
// 好处：将来加缓存、加远程同步、切换数据源都只改这里，不动 UI。
// 嵌入式类比：HAL 层封装了寄存器操作，上层应用代码不直接读寄存器。
@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {
    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()

    fun observeBetween(startMs: Long, endMs: Long): Flow<List<TransactionEntity>> =
        dao.observeBetween(startMs, endMs)

    fun observeTotalExpenseBetween(startMs: Long, endMs: Long): Flow<Long> =
        dao.observeTotalExpenseBetween(startMs, endMs)

    fun observeTotalIncomeBetween(startMs: Long, endMs: Long): Flow<Long> =
        dao.observeTotalIncomeBetween(startMs, endMs)

    suspend fun insert(transaction: TransactionEntity): Long = dao.insert(transaction)

    // 自动抓取路径专用：60s 窗口内同来源同金额视为重复，跳过入库。
    // 返回 null 表示重复，返回 Long 表示新插入的 id。
    suspend fun insertIfNotDuplicate(
        transaction: TransactionEntity,
        windowMs: Long = 60_000L,
    ): Long? {
        val existing = dao.findRecentSameAmount(
            source = transaction.source,
            amountCents = transaction.amountCents,
            windowStartMs = transaction.timestamp - windowMs,
            windowEndMs = transaction.timestamp + windowMs,
        )
        return if (existing != null) null else dao.insert(transaction)
    }

    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)
}
