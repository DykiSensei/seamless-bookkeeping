package com.bookkeeping.app.notification

import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Hilt EntryPoint：让 NotificationListenerService 能拿到 Repository
// 背景：Hilt 不直接支持 NotificationListenerService（不在 @AndroidEntryPoint 的注入列表里）
// 解决方案：定义 EntryPoint 接口 + 用 EntryPointAccessors.fromApplication() 手动获取
//
// 嵌入式类比：Hilt 是自动布线的"主总线"，NotificationListenerService 不在总线上，
//          所以拉一条"专用引脚"接出来。
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationListenerEntryPoint {
    fun transactionRepository(): TransactionRepository
}
