package com.proxyfarm.node.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.proxyfarm.node.ProxyFarmApplication.Companion.CHANNEL_PROXY_SERVICE
import com.proxyfarm.node.proxy.HttpProxyEngine
import com.proxyfarm.node.proxy.ProxyStats
import com.proxyfarm.node.settings.AppSettings
import com.proxyfarm.node.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Long-running foreground service.
 *
 * Now reads the proxy port dynamically from [AppSettings] so the
 * operator can change it from the Settings screen and it takes
 * effect on the next START command.
 */
class ProxyService : Service() {

    companion object {
        private const val TAG         = "ProxyService"
        const val NOTIFICATION_ID     = 1001
        const val WAKE_LOCK_TAG       = "FleetProxy::ProxyCpuWakeLock"
        const val ACTION_START        = "com.proxyfarm.node.ACTION_START_PROXY"
        const val ACTION_STOP         = "com.proxyfarm.node.ACTION_STOP_PROXY"
        const val EXTRA_JOB_ID        = "extra_job_id"
        const val BROADCAST_STATUS    = "com.proxyfarm.node.PROXY_STATUS"
        const val EXTRA_IS_RUNNING    = "extra_is_running"
    }

    private val serviceScope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var proxyEngine: HttpProxyEngine?    = null
    val proxyStats = ProxyStats()
    private var currentJobId  = "idle"

    // Reads proxy port from SharedPreferences on each start
    private val appSettings by lazy { AppSettings(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentJobId = intent.getStringExtra(EXTRA_JOB_ID) ?: "unknown"
                Log.i(TAG, "START  job=$currentJobId  port=${appSettings.currentProxyPort}")
                startForegroundWithNotification()
                acquireWakeLock()
                startProxyEngine()
                broadcastStatus(true)
            }
            ACTION_STOP -> {
                Log.i(TAG, "STOP")
                shutdownAndStop()
            }
            else -> {
                Log.w(TAG, "Redelivered intent — restarting")
                startForegroundWithNotification()
                acquireWakeLock()
                startProxyEngine()
                broadcastStatus(true)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        shutdownAndStop()
        super.onDestroy()
    }

    // ── Foreground Notification ───────────────────────────────────

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ProxyService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val port = appSettings.currentProxyPort
        return NotificationCompat.Builder(this, CHANNEL_PROXY_SERVICE)
            .setContentTitle("IP Transmitter Node Active")
            .setContentText("Job: $currentJobId  •  Port $port")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Proxy", stopIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ── WakeLock ──────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).also {
            it.acquire()
            Log.d(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ── Proxy Engine ──────────────────────────────────────────────

    private fun startProxyEngine() {
        proxyStats.reset()
        val port = appSettings.currentProxyPort
        proxyEngine = HttpProxyEngine(proxyPort = port).also { it.stats = proxyStats }
        serviceScope.launch {
            try {
                Log.i(TAG, "Launching HttpProxyEngine on port $port")
                proxyEngine?.start()
            } catch (e: Exception) {
                Log.e(TAG, "ProxyEngine crashed: ${e.message}", e)
                broadcastStatus(false)
                stopSelf()
            }
        }
    }

    private fun stopProxyEngine() {
        try { proxyEngine?.stop() } catch (e: Exception) { Log.w(TAG, "Stop error: ${e.message}") }
        proxyEngine = null
    }

    // ── Shutdown ──────────────────────────────────────────────────

    private fun shutdownAndStop() {
        broadcastStatus(false)
        stopProxyEngine()
        releaseWakeLock()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastStatus(isRunning: Boolean) {
        sendBroadcast(Intent(BROADCAST_STATUS).apply {
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_JOB_ID, currentJobId)
        })
        Log.d(TAG, "Status broadcast → isRunning=$isRunning")
    }
}