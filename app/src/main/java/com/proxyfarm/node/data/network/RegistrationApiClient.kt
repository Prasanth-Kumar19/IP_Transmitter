package com.proxyfarm.node.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.proxyfarm.node.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-registers this device's FCM token with the dashboard server so the
 * sender no longer needs a hardcoded token.
 *
 * POSTs to /api/register?token=<dashboard_token> with body:
 * {
 *   "device_id":     "<ANDROID_ID>",
 *   "fcm_token":     "<current FCM token>",
 *   "model":         "Pixel 7",
 *   "android_sdk":   34,
 *   "registered_at": "2026-06-30T14:32:01Z"
 * }
 *
 * Call registerCurrentToken() on app startup (token already exists), and
 * registerToken() from onNewToken() (token just rotated).
 */
object RegistrationApiClient {

    private const val TAG             = "RegistrationApiClient"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT    = 15_000

    // Fire-and-forget scope for background registration.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fetches the current FCM token from Firebase, then registers it. */
    fun registerCurrentToken(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                registerToken(context, task.result)
            } else {
                Log.e(TAG, "Could not fetch FCM token", task.exception)
            }
        }
    }

    /** Registers a known token (use from onNewToken). */
    fun registerToken(context: Context, token: String) {
        if (token.isBlank()) return
        scope.launch {
            val ok = postRegistration(context.applicationContext, token)
            Log.i(TAG, if (ok) "✅ Token registered with server" else "⚠ Token registration failed")
        }
    }

    private suspend fun postRegistration(context: Context, token: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val settings = AppSettings(context)
                val url      = buildRegisterUrl(settings)
                val body     = JSONObject().apply {
                    put("device_id",     deviceId(context))
                    put("fcm_token",     token)
                    put("model",         "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("android_sdk",   Build.VERSION.SDK_INT)
                    put("registered_at", System.currentTimeMillis())
                }.toString()

                val (code, _) = httpPost(url, body)
                Log.d(TAG, "POST $url → HTTP $code")
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                false
            }
        }

    private fun buildRegisterUrl(s: AppSettings): String {
        val port = s.currentDashboardPort
        val base = if (port == 80 || port == 443) s.currentVmIp else "${s.currentVmIp}:$port"
        return "http://$base/api/register?token=${s.currentDashboardToken}"
    }

    @SuppressLint("HardwareIds")
    private fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    private fun httpPost(urlString: String, jsonBody: String): Pair<Int, String> {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.apply {
                requestMethod  = "POST"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                doOutput       = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept",       "application/json")
                setRequestProperty("User-Agent",   "FleetProxy/1.0 Android")
            }
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp   = stream?.use { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() } ?: ""
            Pair(code, resp)
        } finally { conn.disconnect() }
    }
}
