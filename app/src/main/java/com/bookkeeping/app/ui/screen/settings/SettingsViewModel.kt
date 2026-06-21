package com.bookkeeping.app.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookkeeping.app.data.settings.SettingsPreferences
import com.bookkeeping.app.notification.KeepAliveService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefs: SettingsPreferences,
) : ViewModel() {

    val keepAliveEnabled: StateFlow<Boolean> = prefs.observeKeepAliveEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setKeepAlive(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setKeepAliveEnabled(enabled)
            if (enabled) KeepAliveService.start(appContext) else KeepAliveService.stop(appContext)
        }
    }
}
