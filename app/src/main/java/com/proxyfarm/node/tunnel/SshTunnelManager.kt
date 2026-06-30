package com.proxyfarm.node.tunnel

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SshTunnelManager {

    private const val TAG = "SshTunnelManager"
    private var session: Session? = null

    suspend fun startTunnel(
        serverHost:     String,
        serverPort:     Int    = 22,
        serverUser:     String = "hello",
        serverPassword: String = "",
        privateKey:     String = "",
        localPort:      Int    = 8080,
        remotePort:     Int    = 9090
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting SSH tunnel → $serverHost:$remotePort")
            val jsch = JSch()

            when {
                privateKey.isNotBlank() -> {
                    Log.i(TAG, "Using private key authentication")

                    // Reconstruct key properly line by line
                    val rawLines = privateKey
                        .trim()
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .split("\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    Log.d(TAG, "Key lines: ${rawLines.size}")
                    Log.d(TAG, "First: ${rawLines.firstOrNull()}")
                    Log.d(TAG, "Last: ${rawLines.lastOrNull()}")

                    // Rebuild with proper PEM format
                    val sb = StringBuilder()
                    for (line in rawLines) {
                        sb.append(line)
                        sb.append("\n")
                    }
                    val cleanKey = sb.toString()

                    jsch.addIdentity(
                        "proxy_key",
                        cleanKey.toByteArray(Charsets.UTF_8),
                        null,
                        null
                    )
                }
                serverPassword.isNotBlank() -> {
                    Log.i(TAG, "Using password authentication")
                }
                else -> {
                    Log.e(TAG, "No authentication method provided")
                    return@withContext false
                }
            }

            val s = jsch.getSession(serverUser, serverHost, serverPort)

            if (privateKey.isBlank() && serverPassword.isNotBlank()) {
                s.setPassword(serverPassword)
                s.setConfig("PreferredAuthentications", "password,keyboard-interactive")
            } else {
                s.setConfig("PreferredAuthentications", "publickey")
            }

            s.setConfig("StrictHostKeyChecking",  "no")
            s.setConfig("ServerAliveInterval",    "30")
            s.setConfig("ServerAliveCountMax",    "5")
            s.connect(30_000)

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
