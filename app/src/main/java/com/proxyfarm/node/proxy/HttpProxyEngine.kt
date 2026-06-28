package com.proxyfarm.node.proxy

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URL

class HttpProxyEngine(private val proxyPort: Int = 8080) {
    companion object {
        private const val TAG                = "HttpProxyEngine"
        private const val BUFFER_SIZE        = 8192
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val SO_TIMEOUT_MS      = 30_000
    }
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var serverSocket: ServerSocket? = null
    var stats: ProxyStats? = null

    fun start() {
        Log.i(TAG, "Starting proxy on port $proxyPort")
        serverSocket = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress("0.0.0.0", proxyPort)) }
        while (engineScope.isActive) {
            try {
                val cs = serverSocket!!.accept(); cs.soTimeout = SO_TIMEOUT_MS
                engineScope.launch { handleClient(cs) }
            } catch (e: SocketException) { if (!engineScope.isActive) Log.i(TAG, "Stopped"); else stats?.recordError(); break }
            catch (e: Exception) { Log.e(TAG, "Accept error: ${e.message}"); stats?.recordError() }
        }
    }

    fun stop() { engineScope.cancel(); try { serverSocket?.close() } catch (_: Exception) {}; serverSocket = null }

    private suspend fun handleClient(cs: Socket) {
        try {
            cs.use { s ->
                val inp = s.getInputStream(); val out = s.getOutputStream()
                val reader = BufferedReader(InputStreamReader(inp))
                val rl = reader.readLine()?.trim() ?: return
                val parts = rl.split(" "); if (parts.size < 3) return
                val method = parts[0].uppercase(); val target = parts[1]; val version = parts[2]
                when (method) {
                    "CONNECT" -> { stats?.recordConnectionOpened(true);  handleHttpsTunnel(target, out, inp) }
                    else      -> { stats?.recordConnectionOpened(false); handleHttpRequest(method, target, version, reader, out) }
                }
            }
        } catch (e: SocketException) { Log.d(TAG, "Client disconnected")
        } catch (e: Exception) { Log.e(TAG, "Client error: ${e.message}"); stats?.recordError()
        } finally { stats?.recordConnectionClosed() }
    }

    private fun handleHttpRequest(method: String, target: String, version: String, reader: BufferedReader, out: OutputStream) {
        val (host, port, path) = try { val u = URL(target); Triple(u.host, if (u.port == -1) 80 else u.port, if (u.file.isNullOrEmpty()) "/" else u.file) } catch (_: Exception) { Triple(target, 80, "/") }
        val headers = mutableListOf<String>(); var line = reader.readLine()
        while (!line.isNullOrBlank()) { if (!line.startsWith("Proxy-", ignoreCase = true)) headers.add(line); line = reader.readLine() }
        try {
            Socket().use { up ->
                up.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS); up.soTimeout = SO_TIMEOUT_MS
                val sb = StringBuilder("$method $path $version\r\n"); headers.forEach { sb.append("$it\r\n") }; sb.append("\r\n")
                up.getOutputStream().apply { write(sb.toString().toByteArray(Charsets.ISO_8859_1)); flush() }
                stats?.recordBytesIn(pipeStream(up.getInputStream(), out))
            }
        } catch (e: Exception) { Log.e(TAG, "HTTP error: ${e.message}"); stats?.recordError(); try { out.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray()) } catch (_: Exception) {} }
    }

    private fun handleHttpsTunnel(target: String, out: OutputStream, inp: InputStream) {
        val ci = target.lastIndexOf(':'); val host = if (ci > 0) target.substring(0, ci) else target; val port = if (ci > 0) target.substring(ci + 1).toIntOrNull() ?: 443 else 443
        try {
            Socket().use { up ->
                up.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS); up.soTimeout = SO_TIMEOUT_MS
                out.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.ISO_8859_1)); out.flush()
                var bu = 0L; var bd = 0L
                val t1 = Thread { try { bu = pipeStream(inp, up.getOutputStream()) } catch (_: Exception) {} }
                val t2 = Thread { try { bd = pipeStream(up.getInputStream(), out) } catch (_: Exception) {} }
                t1.start(); t2.start(); t1.join(); t2.join()
                stats?.recordBytesOut(bu); stats?.recordBytesIn(bd)
            }
        } catch (e: Exception) { Log.e(TAG, "HTTPS error: ${e.message}"); stats?.recordError(); try { out.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray()) } catch (_: Exception) {} }
    }

    private fun pipeStream(inp: InputStream, out: OutputStream): Long {
        val buf = ByteArray(BUFFER_SIZE); var n: Int; var total = 0L
        while (inp.read(buf).also { n = it } != -1) { out.write(buf, 0, n); out.flush(); total += n }
        return total
    }
}
