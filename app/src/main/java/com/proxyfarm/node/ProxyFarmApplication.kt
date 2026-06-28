package com.proxyfarm.node

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp

class ProxyFarmApplication : Application() {
    companion object {
        const val TAG = "FleetProxy"
        const val CHANNEL_PROXY_SERVICE = "proxy_farm_channel"
        const val CHANNEL_FCM_ALERTS    = "fcm_alerts_channel"
    }
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "FleetProxy application starting")
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val proxyChannel = NotificationChannel(CHANNEL_PROXY_SERVICE, "Fleet Proxy Service", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Persistent notification while the proxy tunnel is active"
            setShowBadge(false); enableLights(false); enableVibration(false)
        }
        val fcmChannel = NotificationChannel(CHANNEL_FCM_ALERTS, "Cloud Command Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Notifications for incoming START / STOP proxy commands"
        }
        manager.createNotificationChannels(listOf(proxyChannel, fcmChannel))
    }
}
