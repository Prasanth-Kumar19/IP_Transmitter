package com.proxyfarm.node.proxy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe, real-time statistics collector for the proxy engine.
 *
 * Uses [AtomicLong] / [AtomicInteger] for lock-free updates from
 * multiple concurrent coroutine/thread contexts, and exposes a
 * [StateFlow] snapshot for the UI layer to observe.
 *
 * Lifecycle: reset each time [HttpProxyEngine] starts a new session.
 */
class ProxyStats {

    // ── Atomic counters (updated from IO threads) ─────────────────
    private val _totalBytesIn      = AtomicLong(0L)
    private val _totalBytesOut     = AtomicLong(0L)
    private val _totalConnections  = AtomicInteger(0)
    private val _activeConnections = AtomicInteger(0)
    private val _errorCount        = AtomicInteger(0)
    private val _httpRequests      = AtomicInteger(0)
    private val _httpsRequests     = AtomicInteger(0)

    // ── Snapshot StateFlow (observed by UI) ───────────────────────
    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // Update API (called from proxy engine internals)
    // ─────────────────────────────────────────────────────────────

    fun recordBytesIn(bytes: Long) {
        _totalBytesIn.addAndGet(bytes)
        publishSnapshot()
    }

    fun recordBytesOut(bytes: Long) {
        _totalBytesOut.addAndGet(bytes)
        publishSnapshot()
    }

    fun recordConnectionOpened(isHttps: Boolean) {
        _totalConnections.incrementAndGet()
        _activeConnections.incrementAndGet()
        if (isHttps) _httpsRequests.incrementAndGet()
        else         _httpRequests.incrementAndGet()
        publishSnapshot()
    }

    fun recordConnectionClosed() {
        val current = _activeConnections.decrementAndGet()
        if (current < 0) _activeConnections.set(0)
        publishSnapshot()
    }

    fun recordError() {
        _errorCount.incrementAndGet()
        publishSnapshot()
    }

    // ─────────────────────────────────────────────────────────────
    // Reset (called when engine restarts)
    // ─────────────────────────────────────────────────────────────

    fun reset() {
        _totalBytesIn.set(0L)
        _totalBytesOut.set(0L)
        _totalConnections.set(0)
        _activeConnections.set(0)
        _errorCount.set(0)
        _httpRequests.set(0)
        _httpsRequests.set(0)
        publishSnapshot()
    }

    // ─────────────────────────────────────────────────────────────
    // Snapshot publication
    // ─────────────────────────────────────────────────────────────

    private fun publishSnapshot() {
        _snapshot.value = Snapshot(
            totalBytesIn      = _totalBytesIn.get(),
            totalBytesOut     = _totalBytesOut.get(),
            totalConnections  = _totalConnections.get(),
            activeConnections = _activeConnections.get(),
            errorCount        = _errorCount.get(),
            httpRequests      = _httpRequests.get(),
            httpsRequests     = _httpsRequests.get()
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Snapshot data class (immutable — safe to pass to UI)
    // ─────────────────────────────────────────────────────────────

    data class Snapshot(
        val totalBytesIn:      Long = 0L,
        val totalBytesOut:     Long = 0L,
        val totalConnections:  Int  = 0,
        val activeConnections: Int  = 0,
        val errorCount:        Int  = 0,
        val httpRequests:      Int  = 0,
        val httpsRequests:     Int  = 0
    ) {
        val totalBytes: Long
            get() = totalBytesIn + totalBytesOut

        val bytesInReadable: String  get() = totalBytesIn.toReadableBytes()
        val bytesOutReadable: String get() = totalBytesOut.toReadableBytes()
        val totalBytesReadable: String get() = totalBytes.toReadableBytes()

        private fun Long.toReadableBytes(): String = when {
            this < 1_024L         -> "$this B"
            this < 1_048_576L     -> "${"%.1f".format(this / 1_024.0)} KB"
            this < 1_073_741_824L -> "${"%.1f".format(this / 1_048_576.0)} MB"
            else                  -> "${"%.2f".format(this / 1_073_741_824.0)} GB"
        }
    }
}