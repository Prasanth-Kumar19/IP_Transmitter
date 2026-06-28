package com.proxyfarm.node.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.proxyfarm.node.data.model.NetworkInfo
import com.proxyfarm.node.data.model.PipelineStatus
import com.proxyfarm.node.data.model.ProxyState
import com.proxyfarm.node.data.repository.ProxyRepository
import com.proxyfarm.node.proxy.ProxyStats
import com.proxyfarm.node.service.ProxyService
import com.proxyfarm.node.settings.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object { private const val TAG = "MainViewModel" }
    private val repository  = ProxyRepository(application.applicationContext)
    private val appSettings = repository.appSettings

    val proxyState: StateFlow<ProxyState>       = repository.proxyState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProxyState.Idle)
    val networkInfo: StateFlow<NetworkInfo>     = repository.networkInfo.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkInfo.EMPTY)
    val pipelineStatus: StateFlow<PipelineStatus> = repository.pipelineStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PipelineStatus.LOADING)
    val settingsState: StateFlow<SettingsState> = appSettings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())
    private val _statsSnapshot = MutableStateFlow(ProxyStats.Snapshot())
    val statsSnapshot: StateFlow<ProxyStats.Snapshot> = _statsSnapshot.asStateFlow()

    fun onToggleProxy(enable: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        val intent = Intent(ctx, ProxyService::class.java).apply { action = if (enable) ProxyService.ACTION_START else ProxyService.ACTION_STOP; if (enable) putExtra(ProxyService.EXTRA_JOB_ID, "manual-debug") }
        if (enable) ctx.startForegroundService(intent) else ctx.startService(intent)
        if (!enable) _statsSnapshot.value = ProxyStats.Snapshot()
    }

    fun onRefreshDashboard() { repository.refreshDashboardNow() }

    fun onSaveSettings(vmIp: String, dashboardPort: Int, dashboardToken: String, proxyPort: Int) {
        appSettings.saveAll(vmIp, dashboardPort, dashboardToken, proxyPort)
        repository.refreshDashboardNow()
    }

    fun onResetSettings() { appSettings.resetToDefaults(); repository.refreshDashboardNow() }
    fun updateStats(snapshot: ProxyStats.Snapshot) { _statsSnapshot.value = snapshot }

    override fun onCleared() { super.onCleared(); repository.cleanup() }
}
