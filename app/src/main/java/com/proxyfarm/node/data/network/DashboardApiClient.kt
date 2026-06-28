package com.proxyfarm.node.data.network

import android.util.Log
import com.proxyfarm.node.data.model.PipelineStatus
import com.proxyfarm.node.data.model.TaskInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Calls /api/state?token=listingbot123 — the correct endpoint
 * from the Flask dashboard server.
 *
 * Response shape:
 * {
 *   "status": "running",
 *   "current_task": "keyword",
 *   "last_updated": "2026-06-27T14:32:01",
 *   "tasks": {
 *     "keyword":             {"status": "completed", "runtime_seconds": 45},
 *     "asinvariants":        {"status": "running"},
 *     "listingrestriction":  {"status": "pending"},
 *     "trademark_inventory": {"status": "pending"},
 *     "dotin_dotcom":        {"status": "pending"}
 *   }
 * }
 */
object DashboardApiClient {

    private const val TAG             = "DashboardApiClient"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT    = 15_000

    // ── Pipeline State ────────────────────────────────────────────

    suspend fun fetchPipelineStatus(baseUrl: String): PipelineStatus = withContext(Dispatchers.IO) {
        try {
            // Use /api/state endpoint — correct Flask route
            val stateUrl = buildStateUrl(baseUrl)
            Log.d(TAG, "Fetching state: $stateUrl")
            val (code, body) = httpGet(stateUrl)
            if (code != HttpURLConnection.HTTP_OK) {
                return@withContext PipelineStatus.error("Server returned HTTP $code")
            }
            parseStateJson(body)
        } catch (e: Exception) {
            Log.e(TAG, "fetchPipelineStatus failed: ${e.message}", e)
            PipelineStatus.error(e.message ?: "Network error")
        }
    }

    // ── Public IP ─────────────────────────────────────────────────

    suspend fun fetchPublicIpv4(): String? = withContext(Dispatchers.IO) {
        try {
            val (code, body) = httpGet("https://api.ipify.org?format=json")
            if (code != 200) return@withContext null
            JSONObject(body).optString("ip").ifBlank { null }
        } catch (e: Exception) { null }
    }

    // ── URL Builder ───────────────────────────────────────────────

    /**
     * Converts the dashboard URL to the state API URL.
     * e.g. "http://34.71.36.248/dashboard?token=listingbot123"
     *      → "http://34.71.36.248/api/state?token=listingbot123"
     */
    private fun buildStateUrl(dashboardUrl: String): String {
        return try {
            val url = URL(dashboardUrl)
            val token = url.query?.split("&")
                ?.find { it.startsWith("token=") }
                ?.removePrefix("token=") ?: "listingbot123"
            val base = "${url.protocol}://${url.host}${if (url.port != -1 && url.port != 80) ":${url.port}" else ""}"
            "$base/api/state?token=$token"
        } catch (e: Exception) {
            // Fallback: replace /dashboard with /api/state
            dashboardUrl.replace("/dashboard", "/api/state")
        }
    }

    // ── JSON Parser ───────────────────────────────────────────────

    private fun parseStateJson(raw: String): PipelineStatus {
        return try {
            val json     = JSONObject(raw)
            val status   = json.optString("status", "unknown")
            val curTask  = json.optString("current_task", "—")
            val lastUpd  = json.optString("last_updated", "")
            val tasksObj = json.optJSONObject("tasks")

            // Parse individual tasks
            val tasks = mutableListOf<TaskInfo>()
            val taskDefs = listOf(
                "keyword"             to "1. Keyword ASIN Scraper",
                "asinvariants"        to "2. ASIN Variants",
                "listingrestriction"  to "3. Listing Restriction Check",
                "trademark_inventory" to "4. TMS Scraper (Trademark)",
                "dotin_dotcom"        to "5. Simple Dot (Domain Check)"
            )
            for ((key, label) in taskDefs) {
                val t       = tasksObj?.optJSONObject(key)
                val tStatus = t?.optString("status", "pending") ?: "pending"
                val runtime = t?.optInt("runtime_seconds", 0) ?: 0
                tasks.add(TaskInfo(key, label, tStatus, runtime))
            }

            // Map pipeline status to counts
            val activePipelines = if (status == "running") 1 else 0
            val errorCount      = if (status == "failed") 1 else 0
            val completedToday  = tasks.count { it.status == "completed" }
            val queuedJobs      = tasks.count { it.status == "pending" }

            PipelineStatus(
                isLoading       = false,
                activePipelines = activePipelines,
                queuedJobs      = queuedJobs,
                completedToday  = completedToday,
                errorCount      = errorCount,
                lastRefreshed   = utcTimestamp(),
                rawJson         = raw,
                errorMessage    = null,
                pipelineStatus  = status,
                currentTask     = curTask,
                tasks           = tasks
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            PipelineStatus.error("Malformed response: ${e.message}")
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────

    private fun httpGet(urlString: String): Pair<Int, String> {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.apply {
                requestMethod  = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                setRequestProperty("Accept",     "application/json")
                setRequestProperty("User-Agent", "FleetProxy/1.0 Android")
                doInput        = true
            }
            conn.connect()
            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body   = stream?.use { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() } ?: ""
            Log.d(TAG, "GET $urlString → HTTP $code")
            Pair(code, body)
        } finally { conn.disconnect() }
    }

    private fun utcTimestamp(): String =
        SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
