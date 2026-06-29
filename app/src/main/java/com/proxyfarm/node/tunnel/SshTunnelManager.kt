package com.proxyfarm.node.tunnel

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reverse SSH tunnel — phone connects OUT to server.
 * All config comes from AppSettings (dynamic VM IP).
 *
 * server:remotePort → phone:localProxyPort
 * Scraper uses: http://127.0.0.1:remotePort
 */
object SshTunnelManager {

    private const val TAG = "SshTunnelManager"
    private var session: Session? = null

    /**
     * @param serverHost     VM IP from AppSettings (dynamic)
     * @param serverPort     SSH port (default 22)
     * @param serverUser     SSH username
     * @param serverPassword SSH password
     * @param localPort      Proxy port on phone (e.g. 8080)
     * @param remotePort     Port exposed on server (e.g. 9090)
     */
    suspend fun startTunnel(
        serverHost:     String,
        serverPort:     Int    = 22,
        serverUser:     String,
        serverPassword: String,
        localPort:      Int,
        remotePort:     Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting reverse tunnel → $serverHost:$remotePort")
            val jsch = JSch()
            val s    = jsch.getSession(serverUser, serverHost, serverPort)
            s.setPassword(serverPassword)
            s.setConfig("StrictHostKeyChecking",  "no")
            s.setConfig("ServerAliveInterval",    "30")
            s.setConfig("ServerAliveCountMax",    "3")
            s.connect(30_000)

            // server:remotePort → phone localhost:localPort
            s.setPortForwardingR("0.0.0.0", remotePort, "127.0.0.1", localPort)

            session = s
            Log.i(TAG, "✅ Tunnel active: $serverHost:$remotePort → phone:$localPort")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Tunnel failed: ${e.message}", e)
            false
        }
    }

    fun stopTunnel() {
        try {
            session?.disconnect()
            Log.i(TAG, "Tunnel stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Stop error: ${e.message}")
        } finally {
            session = null
        }
    }

    val isConnected: Boolean
        get() = session?.isConnected == true
}