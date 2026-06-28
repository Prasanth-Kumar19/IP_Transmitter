package com.proxyfarm.node.data.model

data class NetworkInfo(
    val connectionType: String = "Unknown",
    val isCellular: Boolean    = false,
    val publicIpv4: String?    = null,
    val signalStrength: String = "—"
) {
    companion object {
        val EMPTY = NetworkInfo(connectionType = "Not Connected", isCellular = false, publicIpv4 = null)
    }
    val transportLabel: String get() = if (isCellular) "📶 $connectionType" else "📡 $connectionType"
}
