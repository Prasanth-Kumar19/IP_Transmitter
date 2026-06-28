package com.proxyfarm.node

import com.proxyfarm.node.proxy.ProxyStats
import org.junit.Assert.*
import org.junit.Test

class ProxyEngineTest {
    @Test fun `ProxyStats records connections correctly`() {
        val stats = ProxyStats()
        stats.recordConnectionOpened(isHttps = false)
        stats.recordConnectionOpened(isHttps = true)
        val snap = stats.snapshot.value
        assertEquals(2, snap.totalConnections)
        assertEquals(1, snap.httpRequests)
        assertEquals(1, snap.httpsRequests)
    }
    @Test fun `ProxyStats active connections never go below zero`() {
        val stats = ProxyStats()
        stats.recordConnectionClosed()
        assertTrue(stats.snapshot.value.activeConnections >= 0)
    }
    @Test fun `ProxyStats reset clears all counters`() {
        val stats = ProxyStats()
        stats.recordConnectionOpened(isHttps = true)
        stats.recordBytesIn(1024L)
        stats.recordError()
        stats.reset()
        val snap = stats.snapshot.value
        assertEquals(0, snap.totalConnections)
        assertEquals(0L, snap.totalBytesIn)
        assertEquals(0, snap.errorCount)
    }
}
