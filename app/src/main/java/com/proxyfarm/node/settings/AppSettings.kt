package com.proxyfarm.node.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent settings store backed by SharedPreferences.
 *
 * Stores all user-configurable values:
 *  • VM/Dashboard IP address  (e.g. "34.71.36.248")
 *  • Dashboard port           (e.g. 80)
 *  • Dashboard token          (e.g. "listingbot123")
 *  • Proxy port               (e.g. 8080)
 *
 * Exposes a [StateFlow] of [SettingsState] so the UI and
 * repository can reactively observe any changes the user makes.
 */
class AppSettings(context: Context) {

    companion object {
        private const val PREFS_NAME        = "fleet_proxy_settings"

        // ── Preference keys ───────────────────────────────────────
        const val KEY_VM_IP                 = "vm_ip"
        const val KEY_DASHBOARD_PORT        = "dashboard_port"
        const val KEY_DASHBOARD_TOKEN       = "dashboard_token"
        const val KEY_PROXY_PORT            = "proxy_port"

        // ── Defaults ──────────────────────────────────────────────
        const val DEFAULT_VM_IP             = "34.71.36.248"
        const val DEFAULT_DASHBOARD_PORT    = 80
        const val DEFAULT_DASHBOARD_TOKEN   = "listingbot123"
        const val DEFAULT_PROXY_PORT        = 8080
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Internal mutable state ────────────────────────────────────
    private val _settings = MutableStateFlow(loadFromPrefs())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    // ── Current values (convenience accessors) ────────────────────
    val currentVmIp: String
        get() = prefs.getString(KEY_VM_IP, DEFAULT_VM_IP) ?: DEFAULT_VM_IP

    val currentDashboardPort: Int
        get() = prefs.getInt(KEY_DASHBOARD_PORT, DEFAULT_DASHBOARD_PORT)

    val currentDashboardToken: String
        get() = prefs.getString(KEY_DASHBOARD_TOKEN, DEFAULT_DASHBOARD_TOKEN) ?: DEFAULT_DASHBOARD_TOKEN

    val currentProxyPort: Int
        get() = prefs.getInt(KEY_PROXY_PORT, DEFAULT_PROXY_PORT)

    /** Full dashboard URL built from current settings. */
    val dashboardUrl: String
        get() {
            val ip    = currentVmIp
            val port  = currentDashboardPort
            val token = currentDashboardToken
            val base  = if (port == 80 || port == 443) ip else "$ip:$port"
            return "http://$base/dashboard?token=$token"
        }

    // ─────────────────────────────────────────────────────────────
    // Save Methods
    // ─────────────────────────────────────────────────────────────

    fun saveVmIp(ip: String) {
        prefs.edit().putString(KEY_VM_IP, ip.trim()).apply()
        _settings.value = loadFromPrefs()
    }

    fun saveDashboardPort(port: Int) {
        prefs.edit().putInt(KEY_DASHBOARD_PORT, port).apply()
        _settings.value = loadFromPrefs()
    }

    fun saveDashboardToken(token: String) {
        prefs.edit().putString(KEY_DASHBOARD_TOKEN, token.trim()).apply()
        _settings.value = loadFromPrefs()
    }

    fun saveProxyPort(port: Int) {
        prefs.edit().putInt(KEY_PROXY_PORT, port).apply()
        _settings.value = loadFromPrefs()
    }

    /** Save all settings at once (called from Settings screen Save button). */
    fun saveAll(
        vmIp: String,
        dashboardPort: Int,
        dashboardToken: String,
        proxyPort: Int
    ) {
        prefs.edit()
            .putString(KEY_VM_IP,           vmIp.trim())
            .putInt(KEY_DASHBOARD_PORT,     dashboardPort)
            .putString(KEY_DASHBOARD_TOKEN, dashboardToken.trim())
            .putInt(KEY_PROXY_PORT,         proxyPort)
            .apply()
        _settings.value = loadFromPrefs()
    }

    /** Reset all settings to factory defaults. */
    fun resetToDefaults() {
        prefs.edit()
            .putString(KEY_VM_IP,           DEFAULT_VM_IP)
            .putInt(KEY_DASHBOARD_PORT,     DEFAULT_DASHBOARD_PORT)
            .putString(KEY_DASHBOARD_TOKEN, DEFAULT_DASHBOARD_TOKEN)
            .putInt(KEY_PROXY_PORT,         DEFAULT_PROXY_PORT)
            .apply()
        _settings.value = loadFromPrefs()
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    private fun loadFromPrefs() = SettingsState(
        vmIp           = prefs.getString(KEY_VM_IP,           DEFAULT_VM_IP)           ?: DEFAULT_VM_IP,
        dashboardPort  = prefs.getInt(KEY_DASHBOARD_PORT,     DEFAULT_DASHBOARD_PORT),
        dashboardToken = prefs.getString(KEY_DASHBOARD_TOKEN, DEFAULT_DASHBOARD_TOKEN) ?: DEFAULT_DASHBOARD_TOKEN,
        proxyPort      = prefs.getInt(KEY_PROXY_PORT,         DEFAULT_PROXY_PORT)
    )
}

// ─────────────────────────────────────────────────────────────────
// Settings State Data Class
// ─────────────────────────────────────────────────────────────────

/**
 * Immutable snapshot of all user-configurable settings.
 * Emitted by [AppSettings.settings] StateFlow.
 */
data class SettingsState(
    val vmIp: String           = AppSettings.DEFAULT_VM_IP,
    val dashboardPort: Int     = AppSettings.DEFAULT_DASHBOARD_PORT,
    val dashboardToken: String = AppSettings.DEFAULT_DASHBOARD_TOKEN,
    val proxyPort: Int         = AppSettings.DEFAULT_PROXY_PORT
) {
    /** Full dashboard URL built from this snapshot. */
    val dashboardUrl: String
        get() {
            val base = if (dashboardPort == 80 || dashboardPort == 443) vmIp
                       else "$vmIp:$dashboardPort"
            return "http://$base/dashboard?token=$dashboardToken"
        }

    /** Validation: IP must not be blank and port must be in valid range. */
    val isValid: Boolean
        get() = vmIp.isNotBlank() &&
                dashboardPort in 1..65535 &&
                dashboardToken.isNotBlank() &&
                proxyPort in 1..65535

    val validationError: String?
        get() = when {
            vmIp.isBlank()                  -> "VM IP address cannot be empty"
            dashboardPort !in 1..65535      -> "Dashboard port must be between 1 and 65535"
            dashboardToken.isBlank()        -> "Dashboard token cannot be empty"
            proxyPort !in 1..65535          -> "Proxy port must be between 1 and 65535"
            else                            -> null
        }
}