package com.bookkeeping.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

// 顶层属性：app 全局只有一份 settings DataStore
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

// 全局设置项。目前只放前台保活开关，后续可以扩展。
@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun observeKeepAliveEnabled(): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_KEEP_ALIVE] ?: false }

    suspend fun setKeepAliveEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_KEEP_ALIVE] = enabled }
    }

    // 同步读快照值。在 MainActivity.onCreate 里只需要"当前值"决定是否启动 Service，
    // 不需要 observe；用 runBlocking 拿一下，DataStore 本地读很快不会卡 UI。
    fun keepAliveEnabledBlocking(): Boolean = runBlocking {
        context.settingsDataStore.data.first()[KEY_KEEP_ALIVE] ?: false
    }

    companion object {
        private val KEY_KEEP_ALIVE = booleanPreferencesKey("keep_alive_enabled")
    }
}
