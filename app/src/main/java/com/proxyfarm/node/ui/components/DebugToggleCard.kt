package com.proxyfarm.node.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.proxyfarm.node.data.model.ProxyState
import com.proxyfarm.node.data.model.isRunning
import com.proxyfarm.node.ui.theme.ProxyBlue
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed

@Composable
fun DebugToggleCard(proxyState: ProxyState, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val isRunning = proxyState.isRunning
    val isTransitioning = proxyState is ProxyState.Starting || proxyState is ProxyState.Stopping
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Debug Controls", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)); Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Manual Proxy Toggle", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(2.dp))
                    Text("Start or stop the proxy service manually", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = isRunning, onCheckedChange = { if (!isTransitioning) onToggle(it) }, enabled = !isTransitioning,
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = ProxyGreen,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(0.6f), uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.15f),
                        disabledCheckedTrackColor = ProxyGreen.copy(alpha = 0.4f), disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.08f)))
            }
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)); Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PowerSettingsNew, null, tint = when (proxyState) { is ProxyState.Active -> ProxyGreen; is ProxyState.Starting -> ProxyBlue; is ProxyState.Error -> ProxyRed; else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) }, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(when (proxyState) { is ProxyState.Active -> "ACTIVE  •  Port: ${proxyState.port}  •  Job: ${proxyState.jobId}"; is ProxyState.Starting -> "STARTING…"; is ProxyState.Stopping -> "STOPPING…"; is ProxyState.Error -> "ERROR — ${proxyState.message}"; is ProxyState.Idle -> "IDLE  •  Awaiting FCM command" },
                    style = MaterialTheme.typography.labelSmall, color = when (proxyState) { is ProxyState.Active -> ProxyGreen.copy(alpha = 0.85f); is ProxyState.Error -> ProxyRed.copy(alpha = 0.85f); else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) }, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
