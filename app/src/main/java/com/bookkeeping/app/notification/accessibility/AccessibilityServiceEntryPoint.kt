package com.bookkeeping.app.notification.accessibility

import com.bookkeeping.app.data.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Hilt EntryPoint：AccessibilityService 不在 @AndroidEntryPoint 的支持列表里，
// 用 EntryPointAccessors.fromApplication 手动拉一条专用接出。
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccessibilityServiceEntryPoint {
    fun transactionRepository(): TransactionRepository
}
