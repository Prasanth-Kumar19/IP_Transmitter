package com.proxyfarm.node.data.model

sealed class ProxyState {
    object Idle     : ProxyState()
    object Starting : ProxyState()
    data class Active(val jobId: String = "unknown", val port: Int = 8080) : ProxyState()
    object Stopping : ProxyState()
    data class Error(val message: String) : ProxyState()
}
val ProxyState.isRunning: Boolean get() = this is ProxyState.Active
val ProxyState.displayLabel: String get() = when (this) {
    is ProxyState.Idle     -> "Idle — Listening for Cloud Hooks"
    is ProxyState.Starting -> "Starting proxy engine…"
    is ProxyState.Active   -> "Active — Tunnelling Job: $jobId"
    is ProxyState.Stopping -> "Stopping…"
    is ProxyState.Error    -> "Error: $message"
}
