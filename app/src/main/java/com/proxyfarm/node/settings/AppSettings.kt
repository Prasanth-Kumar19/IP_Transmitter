package com.proxyfarm.node.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
    companion object {
        private const val PREFS_NAME              = "fleet_proxy_settings"
        const val KEY_VM_IP                       = "vm_ip"
        const val KEY_DASHBOARD_PORT              = "dashboard_port"
        const val KEY_DASHBOARD_TOKEN             = "dashboard_token"
        const val KEY_PROXY_PORT                  = "proxy_port"
        const val KEY_SSH_USER                    = "ssh_user"
        const val KEY_SSH_PASSWORD                = "ssh_password"
        const val KEY_SSH_PORT                    = "ssh_port"
        const val KEY_REMOTE_PORT                 = "remote_port"
        const val KEY_SSH_PRIVATE_KEY             = "ssh_private_key"

        const val DEFAULT_VM_IP                   = "34.71.36.248"
        const val DEFAULT_DASHBOARD_PORT          = 80
        const val DEFAULT_DASHBOARD_TOKEN         = "listingbot123"
        const val DEFAULT_PROXY_PORT              = 8080
        const val DEFAULT_SSH_USER                = "hello"
        const val DEFAULT_SSH_PASSWORD            = ""
        const val DEFAULT_SSH_PORT                = 22
        const val DEFAULT_REMOTE_PORT             = 9090
        const val DEFAULT_SSH_PRIVATE_KEY         = ""
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadFromPrefs())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    val currentVmIp: String
        get() = prefs.getString(KEY_VM_IP, DEFAULT_VM_IP) ?: DEFAULT_VM_IP

    val currentDashboardPort: Int
        get() = prefs.getInt(KEY_DASHBOARD_PORT, DEFAULT_DASHBOARD_PORT)

    val currentDashboardToken: String
        get() = prefs.getString(KEY_DASHBOARD_TOKEN, DEFAULT_DASHBOARD_TOKEN) ?: DEFAULT_DASHBOARD_TOKEN

    val currentProxyPort: Int
        get() = prefs.getInt(KEY_PROXY_PORT, DEFAULT_PROXY_PORT)

    val currentSshUser: String
        get() = prefs.getString(KEY_SSH_USER, DEFAULT_SSH_USER) ?: DEFAULT_SSH_USER

    val currentSshPassword: String
        get() = prefs.getString(KEY_SSH_PASSWORD, DEFAULT_SSH_PASSWORD) ?: DEFAULT_SSH_PASSWORD

    val currentSshPort: Int
        get() = prefs.getInt(KEY_SSH_PORT, DEFAULT_SSH_PORT)

    val currentRemotePort: Int
        get() = prefs.getInt(KEY_REMOTE_PORT, DEFAULT_REMOTE_PORT)

    val currentSshPrivateKey: String
        get() = prefs.getString(KEY_SSH_PRIVATE_KEY, DEFAULT_SSH_PRIVATE_KEY) ?: DEFAULT_SSH_PRIVATE_KEY

    val dashboardUrl: String
        get() {
            val p = currentDashboardPort
            val b = if (p == 80 || p == 443) currentVmIp else "$currentVmIp:$p"
            return "http://$b/api/state?token=$currentDashboardToken"
        }

    fun saveAll(
        vmIp:          String,
        dashboardPort: Int,
        dashboardToken: String,
        proxyPort:     Int,
        sshUser:       String,
        sshPassword:   String,
        sshPort:       Int,
        remotePort:    Int,
        sshPrivateKey: String
    ) {
        prefs.edit()
            .putString(KEY_VM_IP,            vmIp.trim())
            .putInt(KEY_DASHBOARD_PORT,      dashboardPort)
            .putString(KEY_DASHBOARD_TOKEN,  dashboardToken.trim())
            .putInt(KEY_PROXY_PORT,          proxyPort)
            .putString(KEY_SSH_USER,         sshUser.trim())
            .putString(KEY_SSH_PASSWORD,     sshPassword)
            .putInt(KEY_SSH_PORT,            sshPort)
            .putInt(KEY_REMOTE_PORT,         remotePort)
            .putString(KEY_SSH_PRIVATE_KEY,  sshPrivateKey.trim())
            .apply()
        _settings.value = loadFromPrefs()
    }

    fun resetToDefaults() {
        prefs.edit()
            .putString(KEY_VM_IP,            DEFAULT_VM_IP)
            .putInt(KEY_DASHBOARD_PORT,      DEFAULT_DASHBOARD_PORT)
            .putString(KEY_DASHBOARD_TOKEN,  DEFAULT_DASHBOARD_TOKEN)
            .putInt(KEY_PROXY_PORT,          DEFAULT_PROXY_PORT)
            .putString(KEY_SSH_USER,         DEFAULT_SSH_USER)
            .putString(KEY_SSH_PASSWORD,     DEFAULT_SSH_PASSWORD)
            .putInt(KEY_SSH_PORT,            DEFAULT_SSH_PORT)
            .putInt(KEY_REMOTE_PORT,         DEFAULT_REMOTE_PORT)
            .putString(KEY_SSH_PRIVATE_KEY,  DEFAULT_SSH_PRIVATE_KEY)
            .apply()
        _settings.value = loadFromPrefs()
    }

    private fun loadFromPrefs() = SettingsState(
        vmIp           = currentVmIp,
        dashboardPort  = currentDashboardPort,
        dashboardToken = currentDashboardToken,
        proxyPort      = currentProxyPort,
        sshUser        = currentSshUser,
        sshPassword    = currentSshPassword,
        sshPort        = currentSshPort,
        remotePort     = currentRemotePort,
        sshPrivateKey  = currentSshPrivateKey
    )
}

data class SettingsState(
    val vmIp:           String = AppSettings.DEFAULT_VM_IP,
    val dashboardPort:  Int    = AppSettings.DEFAULT_DASHBOARD_PORT,
    val dashboardToken: String = AppSettings.DEFAULT_DASHBOARD_TOKEN,
    val proxyPort:      Int    = AppSettings.DEFAULT_PROXY_PORT,
    val sshUser:        String = AppSettings.DEFAULT_SSH_USER,
    val sshPassword:    String = AppSettings.DEFAULT_SSH_PASSWORD,
    val sshPort:        Int    = AppSettings.DEFAULT_SSH_PORT,
    val remotePort:     Int    = AppSettings.DEFAULT_REMOTE_PORT,
    val sshPrivateKey:  String = AppSettings.DEFAULT_SSH_PRIVATE_KEY
) {
    val dashboardUrl: String
        get() {
            val b = if (dashboardPort == 80 || dashboardPort == 443) vmIp
                    else "$vmIp:$dashboardPort"
            return "http://$b/api/state?token=$dashboardToken"
        }

    val isValid: Boolean
        get() = vmIp.isNotBlank() && dashboardPort in 1..65535 &&
                dashboardToken.isNotBlank() && proxyPort in 1..65535
}
