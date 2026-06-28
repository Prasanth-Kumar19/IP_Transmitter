package com.proxyfarm.node.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proxyfarm.node.settings.AppSettings
import com.proxyfarm.node.settings.SettingsState
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed
import kotlinx.coroutines.launch

/**
 * Settings screen where the operator can manually configure:
 *  • VM / Orchestration server IP address
 *  • Dashboard port
 *  • Dashboard auth token
 *  • Local proxy port
 *
 * All values are persisted to SharedPreferences via [AppSettings]
 * and take effect immediately on the next dashboard poll or
 * proxy engine restart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: SettingsState,
    onSave: (vmIp: String, dashboardPort: Int, dashboardToken: String, proxyPort: Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    // ── Local editable state ──────────────────────────────────────
    var vmIp           by rememberSaveable { mutableStateOf(currentSettings.vmIp) }
    var dashboardPort  by rememberSaveable { mutableStateOf(currentSettings.dashboardPort.toString()) }
    var dashboardToken by rememberSaveable { mutableStateOf(currentSettings.dashboardToken) }
    var proxyPort      by rememberSaveable { mutableStateOf(currentSettings.proxyPort.toString()) }
    var showToken      by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // ── Derived validation ────────────────────────────────────────
    val vmIpError          = vmIp.isBlank()
    val dashboardPortError = dashboardPort.toIntOrNull()?.let { it !in 1..65535 } ?: true
    val tokenError         = dashboardToken.isBlank()
    val proxyPortError     = proxyPort.toIntOrNull()?.let { it !in 1..65535 } ?: true
    val canSave            = !vmIpError && !dashboardPortError && !tokenError && !proxyPortError

    // ── Preview URL ───────────────────────────────────────────────
    val previewUrl = run {
        val port = dashboardPort.toIntOrNull() ?: 80
        val base = if (port == 80 || port == 443) vmIp else "$vmIp:$port"
        "http://$base/dashboard?token=$dashboardToken"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Configure VM connection", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontFamily = FontFamily.Monospace)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Section 1: VM / Server IP ─────────────────────────
            SettingsCard(title = "Orchestration Server", icon = Icons.Rounded.Cloud) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // VM IP Address
                    OutlinedTextField(
                        value         = vmIp,
                        onValueChange = { vmIp = it; hasUnsavedChanges = true },
                        label         = { Text("VM IP Address") },
                        placeholder   = { Text("e.g. 34.71.36.248", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (vmIpError) Text("IP address cannot be empty", color = ProxyRed)
                            else Text("Your orchestration server's public IP", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError       = vmIpError,
                        leadingIcon   = { Icon(Icons.Rounded.Cloud, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    // Dashboard Port
                    OutlinedTextField(
                        value         = dashboardPort,
                        onValueChange = { if (it.length <= 5) { dashboardPort = it; hasUnsavedChanges = true } },
                        label         = { Text("Dashboard Port") },
                        placeholder   = { Text("80", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (dashboardPortError) Text("Must be between 1 and 65535", color = ProxyRed)
                            else Text("Port your dashboard API listens on", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError       = dashboardPortError,
                        leadingIcon   = { Icon(Icons.Rounded.NetworkCheck, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    // Dashboard Token
                    OutlinedTextField(
                        value         = dashboardToken,
                        onValueChange = { dashboardToken = it; hasUnsavedChanges = true },
                        label         = { Text("Auth Token") },
                        placeholder   = { Text("listingbot123", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (tokenError) Text("Token cannot be empty", color = ProxyRed)
                            else Text("?token= query parameter", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError       = tokenError,
                        leadingIcon   = { Icon(Icons.Rounded.Key, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon  = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    if (showToken) Icons.Rounded.Check else Icons.Rounded.Key,
                                    contentDescription = if (showToken) "Hide token" else "Show token",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (showToken) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            // ── Section 2: Proxy Port ─────────────────────────────
            SettingsCard(title = "Proxy Engine", icon = Icons.Rounded.Router) {
                OutlinedTextField(
                    value         = proxyPort,
                    onValueChange = { if (it.length <= 5) { proxyPort = it; hasUnsavedChanges = true } },
                    label         = { Text("Local Proxy Port") },
                    placeholder   = { Text("8080", fontFamily = FontFamily.Monospace) },
                    supportingText = {
                        if (proxyPortError) Text("Must be between 1 and 65535", color = ProxyRed)
                        else Text("Port your scraper connects to on this device", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    },
                    isError       = proxyPortError,
                    leadingIcon   = { Icon(Icons.Rounded.Router, null, modifier = Modifier.size(20.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }

            // ── Section 3: URL Preview ────────────────────────────
            SettingsCard(title = "Dashboard URL Preview", icon = Icons.Rounded.NetworkCheck) {
                Column {
                    Text(
                        text       = previewUrl,
                        style      = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color      = if (canSave) ProxyGreen
                                     else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "This URL will be used to fetch pipeline status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }

            // ── Unsaved changes warning ───────────────────────────
            AnimatedVisibility(visible = hasUnsavedChanges, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Rounded.Warning, null,
                        tint     = ProxyRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Unsaved changes — tap Save to apply",
                        style = MaterialTheme.typography.labelSmall,
                        color = ProxyRed.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ── Action Buttons ────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Save button
                Button(
                    onClick = {
                        if (canSave) {
                            onSave(
                                vmIp,
                                dashboardPort.toInt(),
                                dashboardToken,
                                proxyPort.toInt()
                            )
                            hasUnsavedChanges = false
                            scope.launch {
                                snackbarHostState.showSnackbar("✅ Settings saved successfully")
                            }
                        }
                    },
                    enabled  = canSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = ProxyGreen,
                        contentColor   = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Reset to defaults button
                OutlinedButton(
                    onClick  = {
                        onReset()
                        vmIp           = AppSettings.DEFAULT_VM_IP
                        dashboardPort  = AppSettings.DEFAULT_DASHBOARD_PORT.toString()
                        dashboardToken = AppSettings.DEFAULT_DASHBOARD_TOKEN
                        proxyPort      = AppSettings.DEFAULT_PROXY_PORT.toString()
                        hasUnsavedChanges = false
                        scope.launch {
                            snackbarHostState.showSnackbar("↺ Settings reset to defaults")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Settings Card wrapper
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}