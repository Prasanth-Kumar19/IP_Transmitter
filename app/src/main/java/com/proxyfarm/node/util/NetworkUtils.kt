package com.proxyfarm.node.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Stateless utility functions for network introspection.
 * All functions are safe to call from any thread.
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    // ─────────────────────────────────────────────────────────────
    // Connectivity Checks
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the device currently has an active, validated
     * internet connection of any transport type.
     */
    fun isConnected(context: Context): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Returns true if the active network transport is cellular
     * (i.e. mobile data, not Wi-Fi or Ethernet).
     * The proxy farm should ideally only route traffic when this
     * returns true to ensure scraping IPs are residential cellular.
     */
    fun isCellular(context: Context): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Returns true if the active network transport is Wi-Fi.
     * Used to warn the operator that proxy traffic will not appear
     * as a cellular IP to the scraping target.
     */
    fun isWifi(context: Context): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Returns the estimated downstream bandwidth in Kbps for the
     * active network, or 0 if unavailable.
     */
    fun getDownstreamBandwidthKbps(context: Context): Int {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return 0
        return try {
            caps.linkDownstreamBandwidthKbps
        } catch (e: Exception) {
            Log.w(TAG, "Could not read downstream bandwidth: ${e.message}")
            0
        }
    }

    /**
     * Returns the estimated upstream bandwidth in Kbps for the
     * active network, or 0 if unavailable.
     */
    fun getUpstreamBandwidthKbps(context: Context): Int {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return 0
        return try {
            caps.linkUpstreamBandwidthKbps
        } catch (e: Exception) {
            Log.w(TAG, "Could not read upstream bandwidth: ${e.message}")
            0
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Validation Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Validates that a string is a plausible IPv4 address.
     * Used to sanity-check the public IP returned by ipify.
     */
    fun isValidIpv4(ip: String): Boolean {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val n = part.toIntOrNull() ?: return false
            n in 0..255
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Formatting Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Converts a bandwidth value in Kbps to a human-readable string.
     * e.g. 1500 → "1.5 Mbps", 800 → "800 Kbps"
     */
    fun formatBandwidth(kbps: Int): String = when {
        kbps <= 0     -> "—"
        kbps >= 1_000 -> "${"%.1f".format(kbps / 1_000.0)} Mbps"
        else          -> "$kbps Kbps"
    }
}