package com.proxyfarm.node.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.proxyfarm.node.data.model.NetworkInfo
import com.proxyfarm.node.data.model.PipelineStatus
import com.proxyfarm.node.data.model.ProxyState
import com.proxyfarm.node.data.network.DashboardApiClient
import com.proxyfarm.node.service.ProxyService
import com.proxyfarm.node.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProxyRepository(private val context: Context) {

    companion object {
        private const val TAG              = "ProxyRepository"
        private const val POLL_INTERVAL_MS = 30_000L
    }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Initialize appSettings immediately (not lazy) ─────────────
    val appSettings = AppSettings(context)

    private val _proxyState     = MutableStateFlow<ProxyState>(ProxyState.Idle)
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    private val _networkInfo    = MutableStateFlow(NetworkInfo.EMPTY)
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    private val _pipelineStatus = MutableStateFlow(PipelineStatus.LOADING)
    val pipelineStatus: StateFlow<PipelineStatus> = _pipelineStatus.asStateFlow()

    init {
        registerProxyStatusReceiver()
        registerNetworkCallback()
        startDashboardPoller()
        // Poll service status every 2 seconds to update UI
    repoScope.launch {
        while (true) {
            delay(2_000)
            try {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE)
                    as android.app.ActivityManager
                @Suppress("DEPRECATION")
                val running = manager.getRunningServices(Integer.MAX_VALUE)
                    .any { it.service.className == 
                        "com.proxyfarm.node.service.ProxyService" }
                val currentState = _proxyState.value
                if (running && currentState !is ProxyState.Active) {
                    _proxyState.value = ProxyState.Active(
                        jobId = "active",
                        port  = appSettings.currentProxyPort
                    )
                } else if (!running && currentState is ProxyState.Active) {
                    _proxyState.value = ProxyState.Idle
                }
            } catch (e: Exception) {
                Log.w(TAG, "Service check error: ${e.message}")
            }
        }
    }

    private fun checkServiceRunning() {
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
            @Suppress("DEPRECATION")
            val running = manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == "com.proxyfarm.node.service.ProxyService" }
            if (running) {
                Log.d(TAG, "Service already running — updating state")
                _proxyState.value = ProxyState.Active(
                    jobId = "active",
                    port  = appSettings.currentProxyPort
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkServiceRunning error: ${e.message}")
        }
    }

    private val proxyStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val isRunning = intent?.getBooleanExtra(
                ProxyService.EXTRA_IS_RUNNING, false) ?: false
            val jobId = intent?.getStringExtra(
                ProxyService.EXTRA_JOB_ID) ?: "unknown"
            Log.d(TAG, "Broadcast received → isRunning=$isRunning job=$jobId")
            _proxyState.value = if (isRunning)
                ProxyState.Active(jobId = jobId, port = appSettings.currentProxyPort)
            else
                ProxyState.Idle
        }
    }

    private fun registerProxyStatusReceiver() {
        val filter = IntentFilter(ProxyService.BROADCAST_STATUS)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    proxyStatusReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(proxyStatusReceiver, filter)
            }
            Log.d(TAG, "ProxyStatusReceiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver: ${e.message}")
        }
    }

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            repoScope.launch { refreshNetworkInfo() }
        }
        override fun onLost(network: Network) {
            _networkInfo.value = NetworkInfo.EMPTY
        }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            repoScope.launch { refreshNetworkInfo() }
        }
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                networkCallback
            )
        } catch (e: Exception) {
            Log.e(TAG, "NetworkCallback error: ${e.message}")
        }
    }

    private suspend fun refreshNetworkInfo() {
        try {
            val caps = connectivityManager
                .getNetworkCapabilities(connectivityManager.activeNetwork)
            val isCellular = caps?.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isWifi = caps?.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI) == true
            val type = when {
                isCellular -> resolveCellularGeneration()
                isWifi     -> "Wi-Fi"
                else       -> "Not Connected"
            }
            val publicIp = DashboardApiClient.fetchPublicIpv4()
            _networkInfo.value = NetworkInfo(type, isCellular, publicIp)
        } catch (e: Exception) {
            Log.e(TAG, "refreshNetworkInfo error: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCellularGeneration(): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
                as TelephonyManager
            when (tm.networkType) {
                TelephonyManager.NETWORK_TYPE_NR     -> "Cellular 5G"
                TelephonyManager.NETWORK_TYPE_LTE    -> "Cellular 4G LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA  -> "Cellular 3G H+"
                TelephonyManager.NETWORK_TYPE_UMTS   -> "Cellular 3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS   -> "Cellular 2G"
                else                                 -> "Cellular"
            }
        } catch (e: Exception) { "Cellular" }
    }

    private fun startDashboardPoller() {
        repoScope.launch {
            while (true) {
                try {
                    _pipelineStatus.value = DashboardApiClient
                        .fetchPipelineStatus(appSettings.dashboardUrl)
                } catch (e: Exception) {
                    _pipelineStatus.value = PipelineStatus.error(
                        e.message ?: "Poll failed")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshDashboardNow() {
        repoScope.launch {
            _pipelineStatus.value = PipelineStatus.LOADING
            _pipelineStatus.value = DashboardApiClient
                .fetchPipelineStatus(appSettings.dashboardUrl)
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(proxyStatusReceiver)
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup error: ${e.message}")
        }
    }
}
