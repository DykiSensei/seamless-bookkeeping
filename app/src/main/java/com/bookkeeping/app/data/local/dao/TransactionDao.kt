package com.bookkeeping.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bookkeeping.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

// DAO = Data Access Object
// 这是 Room 的"接口模板"，写出 SQL 注解，Room 自动生成实现类。
// 嵌入式类比：声明寄存器读写接口，Room 帮你生成具体的访问代码。
@Dao
interface TransactionDao {

    // 插入。如果 id 冲突就替换。返回新插入的 id
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    // 查所有交易，按时间倒序。
    // Flow<List<...>> 表示"响应式":数据库变了，订阅者自动收到新列表。
    // 嵌入式类比：寄存器变化时自动触发中断给上层。
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    // 查指定时间范围内的（用于"本月"统计等）
    @Query("SELECT * FROM transactions WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp DESC")
    fun observeBetween(startMs: Long, endMs: Long): Flow<List<TransactionEntity>>

    // 统计某段时间内的总支出（单位:分）。COALESCE 处理"没数据时返回 0 而不是 null"
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startMs AND timestamp < :endMs")
    fun observeTotalExpenseBetween(startMs: Long, endMs: Long): Flow<Long>

    // 总收入（单位:分）
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'INCOME' AND timestamp >= :startMs AND timestamp < :endMs")
    fun observeTotalIncomeBetween(startMs: Long, endMs: Long): Flow<Long>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    // 用于自动抓取路径去重：在时间窗口内找同来源 + 同金额的记录。
    // 命中即认为是重复（同一笔交易被通知/无障碍/SMS 多个通道抓到了）。
    @Query("""
        SELECT * FROM transactions
        WHERE source = :source AND amountCents = :amountCents
          AND timestamp >= :windowStartMs AND timestamp <= :windowEndMs
        LIMIT 1
    """)
    suspend fun findRecentSameAmount(
        source: String,
        amountCents: Long,
        windowStartMs: Long,
        windowEndMs: Long,
    ): TransactionEntity?
}
