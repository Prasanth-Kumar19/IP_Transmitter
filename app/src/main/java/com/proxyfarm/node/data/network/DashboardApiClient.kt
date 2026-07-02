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

object DashboardApiClient {

    private const val TAG             = "DashboardApiClient"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT    = 15_000

    // ── Fetch state + logs together ───────────────────────────────

    suspend fun fetchPipelineStatus(dashboardUrl: String): PipelineStatus =
        withContext(Dispatchers.IO) {
            try {
                val stateUrl = buildApiUrl(dashboardUrl, "state")
                val logsUrl  = buildApiUrl(dashboardUrl, "logs")

                Log.d(TAG, "Fetching state: $stateUrl")
                Log.d(TAG, "Fetching logs:  $logsUrl")

                val (stateCode, stateBody) = httpGet(stateUrl)
                val (logsCode,  logsBody)  = httpGet(logsUrl)

                if (stateCode != HttpURLConnection.HTTP_OK) {
                    return@withContext PipelineStatus.error(
                        "Server returned HTTP $stateCode"
                    )
                }

                val logs = if (logsCode == HttpURLConnection.HTTP_OK) {
                    parseLogsJson(logsBody)
                } else {
                    emptyList()
                }

                parseStateJson(stateBody, logs)
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
     * Converts the user-entered dashboard URL to the correct API URL.
     *
     * Examples:
     *   "http://34.71.36.248/dashboard?token=listingbot123"
     *     → state: "http://34.71.36.248/api/state?token=listingbot123"
     *     → logs:  "http://34.71.36.248/api/logs?token=listingbot123"
     *
     *   "http://1.2.3.4:8080/dashboard?token=mytoken"
     *     → state: "http://1.2.3.4:8080/api/state?token=mytoken"
     *     → logs:  "http://1.2.3.4:8080/api/logs?token=mytoken"
     */
    private fun buildApiUrl(dashboardUrl: String, endpoint: String): String {
        return try {
            val url   = URL(dashboardUrl)
            val token = url.query
                ?.split("&")
                ?.find { it.startsWith("token=") }
                ?.removePrefix("token=")
                ?: "listingbot123"
            val port  = url.port
            val base  = "${url.protocol}://${url.host}${
                if (port != -1 && port != 80) ":$port" else ""
            }"
            "$base/api/$endpoint?token=$token"
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse dashboard URL: ${e.message}")
            dashboardUrl
                .replace("/dashboard", "/api/$endpoint")
        }
    }

    // ── JSON Parsers ──────────────────────────────────────────────

    private fun parseStateJson(raw: String, logs: List<String>): PipelineStatus {
        return try {
            val json     = JSONObject(raw)
            val status   = json.optString("status", "unknown")
            val curTask  = json.optString("current_task", "—")
            val tasksObj = json.optJSONObject("tasks")

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
                tasks           = tasks,
                logs            = logs
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            PipelineStatus.error("Malformed response: ${e.message}")
        }
    }

    private fun parseLogsJson(raw: String): List<String> {
        return try {
            val json  = JSONObject(raw)
            val array = json.optJSONArray("lines") ?: return emptyList()
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Logs parse error: ${e.message}")
            emptyList()
        }
    }

    // ── HTTP Primitive ────────────────────────────────────────────

    private fun httpGet(urlString: String): Pair<Int, String> {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.apply {
                requestMethod  = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                setRequestProperty("Accept",     "application/json")
                setRequestProperty("User-Agent", "IPTransmitter/1.0 Android")
                doInput        = true
            }
            conn.connect()
            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body   = stream?.use {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
            } ?: ""
            Log.d(TAG, "GET $urlString → HTTP $code (${body.length} bytes)")
            Pair(code, body)
        } finally {
            conn.disconnect()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun utcTimestamp(): String =
        SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
