package com.proxyfarm.node.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Registers this device with the Flask dashboard server.
 *
 * Called when proxy starts so:
 *  - fleet_map.py shows correct port and UP status
 *  - fcm_sender.py can target this device by device_id
 *  - Dashboard shows which phone is handling which pipeline
 *
 * POST /api/register?token=listingbot123
 * {
 *   "device_id":     "279c6a8acef2ba80",
 *   "fcm_token":     "fxyz123...",
 *   "model":         "Xiaomi 21061119BI",
 *   "android_sdk":   33,
 *   "assigned_port": 9090,
 *   "registered_at": 1234567890
 * }
 */
object DeviceRegistrationClient {

    private const val TAG = "DeviceRegistration"

    @SuppressLint("HardwareIds")
    suspend fun register(
        context:       Context,
        dashboardBase: String,
        token:         String,
        proxyPort:     Int
    ) = withContext(Dispatchers.IO) {
        try {
            // ── Get FCM token ─────────────────────────────────────
            val fcmToken = FirebaseMessaging.getInstance().token.await()

            // ── Get device ID ─────────────────────────────────────
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            // ── Build payload ─────────────────────────────────────
            val payload = JSONObject().apply {
                put("device_id",     deviceId)
                put("fcm_token",     fcmToken)
                put("model",         "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                put("android_sdk",   Build.VERSION.SDK_INT)
                put("assigned_port", proxyPort)
                put("registered_at", System.currentTimeMillis())
            }

            // ── POST to server ────────────────────────────────────
            val url = "$dashboardBase/api/register?token=$token"
            Log.i(TAG, "Registering device=$deviceId port=$proxyPort → $url")

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod  = "POST"
                connectTimeout = 10_000
                readTimeout    = 10_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept",       "application/json")
                setRequestProperty("User-Agent",   "IPTransmitter/1.0 Android")
                doOutput       = true
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            Log.i(TAG, "Registration response: HTTP $code")

            if (code == HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "✅ Device registered successfully")
            } else {
                Log.w(TAG, "⚠ Registration returned HTTP $code")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration failed: ${e.message}", e)
        }
    }
}
