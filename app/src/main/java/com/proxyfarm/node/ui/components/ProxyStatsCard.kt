package com.proxyfarm.node.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proxyfarm.node.data.model.ProxyState
import com.proxyfarm.node.data.model.isRunning
import com.proxyfarm.node.proxy.ProxyStats
import com.proxyfarm.node.ui.theme.ProxyBlue
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed

/**
 * Real-time proxy traffic statistics card.
 *
 * Displays live counters from [ProxyStats.Snapshot]:
 *  • Active / total connections
 *  • HTTP vs HTTPS request split
 *  • Bytes in / out / total
 *  • Error count
 *
 * Layout:
 * ┌─────────────────────────────────────────────┐
 * │  ⇅ Traffic Statistics              ● LIVE   │
 * │  ─────────────────────────────────────────  │
 * │  Active      Total Conns    Errors          │
 * │    3             47           0             │
 * │  ─────────────────────────────────────────  │
 * │  ↓ Inbound: 14.2 MB   ↑ Outbound: 2.1 MB  │
 * │  HTTP: 31             HTTPS: 16            │
 * └─────────────────────────────────────────────┘
 */
@Composable
fun ProxyStatsCard(
    snapshot: ProxyStats.Snapshot,
    proxyState: ProxyState,
    modifier: Modifier = Modifier
) {
    val isActive = proxyState.isRunning

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Rounded.SwapVert,
                    contentDescription = null,
                    tint               = if (isActive) ProxyBlue
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = "Traffic Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isActive) {
                    Text(
                        text       = "● LIVE",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ProxyGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState   = snapshot,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label         = "statsContent"
            ) { snap ->
                Column {
                    // ── Row 1: Connection counters ─────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCell(
                            icon     = Icons.Rounded.Hub,
                            tint     = if (snap.activeConnections > 0) ProxyGreen
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            label    = "Active",
                            value    = snap.activeConnections.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            icon     = Icons.Rounded.Hub,
                            tint     = MaterialTheme.colorScheme.primary,
                            label    = "Total Conns",
                            value    = snap.totalConnections.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            icon     = Icons.Rounded.Hub,
                            tint     = if (snap.errorCount > 0) ProxyRed
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            label    = "Errors",
                            value    = snap.errorCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Row 2: Bandwidth ───────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BandwidthCell(
                            icon     = Icons.Rounded.ArrowDownward,
                            tint     = ProxyGreen,
                            label    = "Inbound",
                            value    = snap.bytesInReadable,
                            modifier = Modifier.weight(1f)
                        )
                        BandwidthCell(
                            icon     = Icons.Rounded.ArrowUpward,
                            tint     = ProxyBlue,
                            label    = "Outbound",
                            value    = snap.bytesOutReadable,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Row 3: Protocol split + total ──────────────
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "HTTP: ${snap.httpRequests}   HTTPS: ${snap.httpsRequests}",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontFamily = FontFamily.Monospace,
                            modifier   = Modifier.weight(1f)
                        )
                        Text(
                            text       = "Total: ${snap.totalBytesReadable}",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCell(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = value,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = tint,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun BandwidthCell(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text       = value,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}