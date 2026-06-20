package com.bookkeeping.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bookkeeping.app.data.local.dao.TransactionDao
import com.bookkeeping.app.data.local.entity.TransactionEntity

// Room 数据库本体。
// entities = 这个库里有哪些"表"
// version = 数据库版本号；每次改 Entity 结构需要 +1 并写 Migration
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false  // 不导出 schema 文件；正式上线建议开
)
abstract class BookkeepingDatabase : RoomDatabase() {
    // 暴露 DAO 给外部使用。Room 会自动生成实现。
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DB_NAME = "bookkeeping.db"
    }
}
