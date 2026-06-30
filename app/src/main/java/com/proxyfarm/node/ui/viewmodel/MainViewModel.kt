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

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val repository  = ProxyRepository(application.applicationContext)
    private val appSettings = repository.appSettings

    // ── Exposed StateFlows ────────────────────────────────────────

    val proxyState: StateFlow<ProxyState> = repository.proxyState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProxyState.Idle)

    val networkInfo: StateFlow<NetworkInfo> = repository.networkInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkInfo.EMPTY)

    val pipelineStatus: StateFlow<PipelineStatus> = repository.pipelineStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PipelineStatus.LOADING)

    val settingsState: StateFlow<SettingsState> = appSettings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    private val _statsSnapshot = MutableStateFlow(ProxyStats.Snapshot())
    val statsSnapshot: StateFlow<ProxyStats.Snapshot> = _statsSnapshot.asStateFlow()

    // ── User Actions ──────────────────────────────────────────────

    fun onToggleProxy(enable: Boolean) {
        Log.i(TAG, "onToggleProxy → enable=$enable")
        val ctx = getApplication<Application>().applicationContext
        if (enable) {
            val intent = Intent(ctx, ProxyService::class.java).apply {
                action = ProxyService.ACTION_START
                putExtra(ProxyService.EXTRA_JOB_ID, "manual-debug")
            }
            try {
                ctx.startForegroundService(intent)
                Log.i(TAG, "startForegroundService called")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}")
                try { ctx.startService(intent) } catch (e2: Exception) {
                    Log.e(TAG, "startService also failed: ${e2.message}")
                }
            }
        } else {
            val intent = Intent(ctx, ProxyService::class.java).apply {
                action = ProxyService.ACTION_STOP
            }
            try { ctx.startService(intent) } catch (_: Exception) {}
            _statsSnapshot.value = ProxyStats.Snapshot()
        }
    }

    fun onRefreshDashboard() {
        repository.refreshDashboardNow()
    }

    fun updateStats(snapshot: ProxyStats.Snapshot) {
        _statsSnapshot.value = snapshot
    }

    // ── Settings Actions ──────────────────────────────────────────

    fun onSaveSettings(
        vmIp:          String,
        dashboardPort: Int,
        dashboardToken: String,
        proxyPort:     Int,
        sshUser:       String,
        sshPassword:   String,
        sshPort:       Int,
        remotePort:    Int,
        sshPrivateKey: String
    ) {
        Log.i(TAG, "Saving settings → ip=$vmIp  remotePort=$remotePort")
        appSettings.saveAll(
            vmIp           = vmIp,
            dashboardPort  = dashboardPort,
            dashboardToken = dashboardToken,
            proxyPort      = proxyPort,
            sshUser        = sshUser,
            sshPassword    = sshPassword,
            sshPort        = sshPort,
            remotePort     = remotePort,
            sshPrivateKey  = sshPrivateKey
        )
        repository.refreshDashboardNow()
    }

    fun onResetSettings() {
        Log.i(TAG, "Resetting settings to defaults")
        appSettings.resetToDefaults()
        repository.refreshDashboardNow()
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
        Log.d(TAG, "MainViewModel cleared")
    }
}
