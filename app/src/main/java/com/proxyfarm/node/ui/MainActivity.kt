package com.proxyfarm.node.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proxyfarm.node.data.model.ProxyState
import com.proxyfarm.node.ui.components.*
import com.proxyfarm.node.ui.settings.SettingsScreen
import com.proxyfarm.node.ui.theme.FleetProxyTheme
import com.proxyfarm.node.ui.theme.ProxyBlue
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { FleetProxyTheme { FleetProxyApp(viewModel) } }
    }
}

// ═════════════════════════════════════════════════════════════════
// App Navigation
// ═════════════════════════════════════════════════════════════════

@Composable
private fun FleetProxyApp(viewModel: MainViewModel) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    if (showSettings) {
        SettingsScreen(
            currentSettings = settingsState,
            onSave = { ip, port, token, proxyPort, sshUser, sshPassword,
                       sshPort, remotePort, sshPrivateKey ->
                viewModel.onSaveSettings(
                    ip, port, token, proxyPort,
                    sshUser, sshPassword, sshPort,
                    remotePort, sshPrivateKey
                )
            },
            onReset = { viewModel.onResetSettings() },
            onBack  = { showSettings = false }
        )
    } else {
        FleetProxyDashboard(
            viewModel      = viewModel,
            onOpenSettings = { showSettings = true }
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// Dashboard Screen
// ═════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FleetProxyDashboard(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val proxyState     by viewModel.proxyState.collectAsStateWithLifecycle()
    val networkInfo    by viewModel.networkInfo.collectAsStateWithLifecycle()
    val pipelineStatus by viewModel.pipelineStatus.collectAsStateWithLifecycle()
    val statsSnapshot  by viewModel.statsSnapshot.collectAsStateWithLifecycle()
    val settingsState  by viewModel.settingsState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Router, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text       = "IP Transmitter",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text       = "→ ${settingsState.vmIp}",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, "Settings")
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
        ) {
            // ── Hero Header ───────────────────────────────────────
            val gradientColors = when (proxyState) {
                is ProxyState.Active ->
                    listOf(ProxyBlue.copy(alpha = 0.18f), MaterialTheme.colorScheme.background)
                is ProxyState.Idle   ->
                    listOf(ProxyGreen.copy(alpha = 0.12f), MaterialTheme.colorScheme.background)
                else ->
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(gradientColors))
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text       = "Node Status",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusChip(proxyState = proxyState)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = "FCM-triggered  •  Cellular-only routing",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 10.sp
                    )
                }
            }

            // ── Dashboard Cards ───────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                PipelineMonitorCard(
                    status    = pipelineStatus,
                    onRefresh = { viewModel.onRefreshDashboard() }
                )

                NetworkMetricsCard(networkInfo = networkInfo)

                ProxyStatsCard(
                    snapshot   = statsSnapshot,
                    proxyState = proxyState
                )

                DebugToggleCard(
                    proxyState = proxyState,
                    onToggle   = { enabled ->
                        viewModel.onToggleProxy(enabled)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (enabled)
                                    "▶ Proxy starting on port ${settingsState.proxyPort}…"
                                else
                                    "■ Proxy stopping…"
                            )
                        }
                    }
                )

                AnimatedVisibility(
                    visible = proxyState is ProxyState.Error,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    if (proxyState is ProxyState.Error) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text  = "⚠ Engine Error: ${(proxyState as ProxyState.Error).message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
