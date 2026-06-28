package com.proxyfarm.node.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proxyfarm.node.data.model.NetworkInfo
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed

@Composable
fun NetworkMetricsCard(networkInfo: NetworkInfo, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Network Debug Metrics", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)); Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                NetworkMetricItem(if (networkInfo.connectionType == "Not Connected") Icons.Rounded.WifiOff else if (networkInfo.isCellular) Icons.Rounded.SignalCellularAlt else Icons.Rounded.Wifi,
                    if (networkInfo.isCellular) ProxyGreen else MaterialTheme.colorScheme.primary, "Connection Type", networkInfo.connectionType,
                    if (networkInfo.connectionType == "Not Connected") ProxyRed else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                NetworkMetricItem(Icons.Rounded.Language, MaterialTheme.colorScheme.tertiary, "Public IPv4", networkInfo.publicIpv4 ?: "Resolving…",
                    if (networkInfo.publicIpv4 != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), monospace = true, modifier = Modifier.weight(1f))
            }
            if (networkInfo.isCellular) {
                Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)); Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CellTower, null, tint = ProxyGreen, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp))
                    Text("Routing through cellular — optimal for proxy farm", style = MaterialTheme.typography.labelSmall, color = ProxyGreen.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun NetworkMetricItem(icon: ImageVector, iconTint: Color, label: String, value: String, valueColor: Color, modifier: Modifier = Modifier, monospace: Boolean = false) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor, fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default, maxLines = 1)
    }
}
