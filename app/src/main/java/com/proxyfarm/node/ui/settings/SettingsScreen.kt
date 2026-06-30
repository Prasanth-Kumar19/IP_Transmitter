package com.proxyfarm.node.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: SettingsState,
    onSave: (
        vmIp: String, dashboardPort: Int, dashboardToken: String,
        proxyPort: Int, sshUser: String, sshPassword: String,
        sshPort: Int, remotePort: Int, sshPrivateKey: String
    ) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    // ── Local state ───────────────────────────────────────────────
    var vmIp           by rememberSaveable { mutableStateOf(currentSettings.vmIp) }
    var dashboardPort  by rememberSaveable { mutableStateOf(currentSettings.dashboardPort.toString()) }
    var dashboardToken by rememberSaveable { mutableStateOf(currentSettings.dashboardToken) }
    var proxyPort      by rememberSaveable { mutableStateOf(currentSettings.proxyPort.toString()) }
    var sshUser        by rememberSaveable { mutableStateOf(currentSettings.sshUser) }
    var sshPassword    by rememberSaveable { mutableStateOf(currentSettings.sshPassword) }
    var sshPort        by rememberSaveable { mutableStateOf(currentSettings.sshPort.toString()) }
    var remotePort     by rememberSaveable { mutableStateOf(currentSettings.remotePort.toString()) }
    var sshPrivateKey  by rememberSaveable { mutableStateOf(currentSettings.sshPrivateKey) }
    var showToken      by remember { mutableStateOf(false) }
    var showPassword   by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // ── Validation ────────────────────────────────────────────────
    val vmIpError       = vmIp.isBlank()
    val dashPortError   = dashboardPort.toIntOrNull()?.let { it !in 1..65535 } ?: true
    val tokenError      = dashboardToken.isBlank()
    val proxyPortError  = proxyPort.toIntOrNull()?.let { it !in 1..65535 } ?: true
    val sshPortError    = sshPort.toIntOrNull()?.let { it !in 1..65535 } ?: true
    val remotePortError = remotePort.toIntOrNull()?.let { it !in 1..65535 } ?: true
    val canSave         = !vmIpError && !dashPortError && !tokenError &&
                          !proxyPortError && !sshPortError && !remotePortError

    // ── Preview URL ───────────────────────────────────────────────
    val previewUrl = run {
        val port = dashboardPort.toIntOrNull() ?: 80
        val base = if (port == 80 || port == 443) vmIp else "$vmIp:$port"
        "http://$base/api/state?token=$dashboardToken"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Settings",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Configure VM connection",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
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

            // ── Section 1: Orchestration Server ───────────────────
            SettingsCard(title = "Orchestration Server", icon = Icons.Rounded.Cloud) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedTextField(
                        value         = vmIp,
                        onValueChange = { vmIp = it; hasUnsavedChanges = true },
                        label         = { Text("VM IP Address") },
                        placeholder   = { Text("e.g. 34.71.36.248", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (vmIpError) Text("IP cannot be empty", color = ProxyRed)
                            else Text("Your orchestration server public IP", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError         = vmIpError,
                        leadingIcon     = { Icon(Icons.Rounded.Cloud, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        textStyle       = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    OutlinedTextField(
                        value         = dashboardPort,
                        onValueChange = { if (it.length <= 5) { dashboardPort = it; hasUnsavedChanges = true } },
                        label         = { Text("Dashboard Port") },
                        placeholder   = { Text("80", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (dashPortError) Text("Must be 1–65535", color = ProxyRed)
                            else Text("Port your dashboard listens on", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError         = dashPortError,
                        leadingIcon     = { Icon(Icons.Rounded.NetworkCheck, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        textStyle       = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

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
                                    null, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (showToken) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        textStyle       = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            // ── Section 2: Proxy Engine ───────────────────────────
            SettingsCard(title = "Proxy Engine", icon = Icons.Rounded.Router) {
                OutlinedTextField(
                    value         = proxyPort,
                    onValueChange = { if (it.length <= 5) { proxyPort = it; hasUnsavedChanges = true } },
                    label         = { Text("Local Proxy Port") },
                    placeholder   = { Text("8080", fontFamily = FontFamily.Monospace) },
                    supportingText = {
                        if (proxyPortError) Text("Must be 1–65535", color = ProxyRed)
                        else Text("Port proxy listens on this device", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    },
                    isError         = proxyPortError,
                    leadingIcon     = { Icon(Icons.Rounded.Router, null, modifier = Modifier.size(20.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(12.dp),
                    textStyle       = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }

            // ── Section 3: SSH Tunnel ─────────────────────────────
            SettingsCard(title = "SSH Reverse Tunnel", icon = Icons.Rounded.Cloud) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(
                        text  = "Phone connects OUT to your VM via SSH.\nScraper uses http://127.0.0.1:remotePort",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value         = sshUser,
                        onValueChange = { sshUser = it; hasUnsavedChanges = true },
                        label         = { Text("SSH Username") },
                        placeholder   = { Text("hello", fontFamily = FontFamily.Monospace) },
                        supportingText = { Text("VM SSH username", color = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                        leadingIcon   = { Icon(Icons.Rounded.Key, null, modifier = Modifier.size(20.dp)) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    OutlinedTextField(
                        value         = sshPassword,
                        onValueChange = { sshPassword = it; hasUnsavedChanges = true },
                        label         = { Text("SSH Password (optional)") },
                        supportingText = { Text("Leave empty if using private key below", color = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                        leadingIcon   = { Icon(Icons.Rounded.Key, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon  = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.Check else Icons.Rounded.Key,
                                    null, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value         = sshPort,
                        onValueChange = { if (it.length <= 5) { sshPort = it; hasUnsavedChanges = true } },
                        label         = { Text("SSH Port") },
                        placeholder   = { Text("22", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (sshPortError) Text("Must be 1–65535", color = ProxyRed)
                            else Text("Usually 22", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError         = sshPortError,
                        leadingIcon     = { Icon(Icons.Rounded.NetworkCheck, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        textStyle       = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    OutlinedTextField(
                        value         = remotePort,
                        onValueChange = { if (it.length <= 5) { remotePort = it; hasUnsavedChanges = true } },
                        label         = { Text("Remote Tunnel Port") },
                        placeholder   = { Text("9090", fontFamily = FontFamily.Monospace) },
                        supportingText = {
                            if (remotePortError) Text("Must be 1–65535", color = ProxyRed)
                            else Text("Port on VM your scraper connects to", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        },
                        isError         = remotePortError,
                        leadingIcon     = { Icon(Icons.Rounded.Router, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        textStyle       = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )

                    // ── SSH Private Key ────────────────────────────
                    OutlinedTextField(
                        value         = sshPrivateKey,
                        onValueChange = { sshPrivateKey = it; hasUnsavedChanges = true },
                        label         = { Text("SSH Private Key") },
                        placeholder   = {
                            Text(
                                "-----BEGIN OPENSSH PRIVATE KEY-----\n...\n-----END OPENSSH PRIVATE KEY-----",
                                fontFamily = FontFamily.Monospace,
                                style      = MaterialTheme.typography.labelSmall
                            )
                        },
                        supportingText = {
                            Text(
                                "Paste private key from: cat ~/proxy_key",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Key, null, modifier = Modifier.size(20.dp)) },
                        minLines    = 5,
                        maxLines    = 10,
                        modifier    = Modifier.fillMaxWidth(),
                        shape       = RoundedCornerShape(12.dp),
                        textStyle   = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // ── Section 4: URL Preview ────────────────────────────
            SettingsCard(title = "Dashboard URL Preview", icon = Icons.Rounded.NetworkCheck) {
                Text(
                    text       = previewUrl,
                    style      = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color      = if (canSave) ProxyGreen
                                 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // ── Unsaved warning ───────────────────────────────────
            AnimatedVisibility(visible = hasUnsavedChanges, enter = fadeIn(), exit = fadeOut()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Warning, null,
                        tint     = ProxyRed.copy(0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Unsaved changes — tap Save to apply",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = ProxyRed.copy(0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ── Buttons ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Button(
                    onClick = {
                        if (canSave) {
                            onSave(
                                vmIp,
                                dashboardPort.toInt(),
                                dashboardToken,
                                proxyPort.toInt(),
                                sshUser,
                                sshPassword,
                                sshPort.toIntOrNull() ?: 22,
                                remotePort.toIntOrNull() ?: 9090,
                                sshPrivateKey
                            )
                            hasUnsavedChanges = false
                            scope.launch {
                                snackbarHostState.showSnackbar("✅ Settings saved!")
                            }
                        }
                    },
                    enabled  = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProxyGreen,
                        contentColor   = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = {
                        onReset()
                        vmIp           = AppSettings.DEFAULT_VM_IP
                        dashboardPort  = AppSettings.DEFAULT_DASHBOARD_PORT.toString()
                        dashboardToken = AppSettings.DEFAULT_DASHBOARD_TOKEN
                        proxyPort      = AppSettings.DEFAULT_PROXY_PORT.toString()
                        sshUser        = AppSettings.DEFAULT_SSH_USER
                        sshPassword    = AppSettings.DEFAULT_SSH_PASSWORD
                        sshPort        = AppSettings.DEFAULT_SSH_PORT.toString()
                        remotePort     = AppSettings.DEFAULT_REMOTE_PORT.toString()
                        sshPrivateKey  = AppSettings.DEFAULT_SSH_PRIVATE_KEY
                        hasUnsavedChanges = false
                        scope.launch {
                            snackbarHostState.showSnackbar("↺ Reset to defaults")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
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
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
