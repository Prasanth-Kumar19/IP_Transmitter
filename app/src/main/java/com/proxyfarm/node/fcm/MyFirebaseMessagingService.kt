package com.proxyfarm.node.fcm

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.proxyfarm.node.data.network.RegistrationApiClient
import com.proxyfarm.node.service.ProxyService

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG  = "FCMService"
        const val KEY_ACTION   = "action"
        const val KEY_JOB_ID   = "job_id"
        const val ACTION_START = "START_PROXY"
        const val ACTION_STOP  = "STOP_PROXY"
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val action = remoteMessage.data[KEY_ACTION]?.trim()
        val jobId  = remoteMessage.data[KEY_JOB_ID] ?: "unknown"
        Log.i(TAG, "FCM received → action=$action  job_id=$jobId")
        when (action) {
            ACTION_START -> applicationContext.startForegroundService(Intent(applicationContext, ProxyService::class.java).apply { this.action = ProxyService.ACTION_START; putExtra(ProxyService.EXTRA_JOB_ID, jobId) })
            ACTION_STOP  -> applicationContext.startService(Intent(applicationContext, ProxyService::class.java).apply { this.action = ProxyService.ACTION_STOP })
            else         -> Log.w(TAG, "Unknown action '$action'")
        }
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed: $token")
        // Token rotated — push the new one to the server so commands keep working.
        RegistrationApiClient.registerToken(applicationContext, token)
    }
}
