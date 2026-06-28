package com.proxyfarm.node.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proxyfarm.node.data.model.PipelineStatus
import com.proxyfarm.node.data.model.TaskInfo
import com.proxyfarm.node.ui.theme.ProxyAmber
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed
import com.proxyfarm.node.ui.theme.ProxyBlue

@Composable
fun PipelineMonitorCard(status: PipelineStatus, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pipeline Monitor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text       = "listing-agent-dashboard / api/state",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onRefresh, enabled = !status.isLoading) {
                    if (status.isLoading)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else
                        Icon(Icons.Rounded.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState   = status,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label         = "pipeline"
            ) { s ->
                when {
                    s.isLoading -> Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                    s.hasError -> Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Error, null, tint = ProxyRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.errorMessage ?: "Error", style = MaterialTheme.typography.bodySmall, color = ProxyRed)
                    }
                    else -> Column {
                        // ── Pipeline status summary ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text       = "Status: ${s.pipelineStatus.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp,
                                    color      = statusColor(s.pipelineStatus),
                                    fontFamily = FontFamily.Monospace
                                )
                                if (s.currentTask != "—" && s.currentTask.isNotBlank()) {
                                    Text(
                                        text  = "Current: ${s.currentTask}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            // Completed count badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ProxyGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text     = "${s.completedToday}/5 done",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = ProxyGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        Spacer(Modifier.height(8.dp))

                        // ── Task list ──────────────────────────────
                        if (s.tasks.isNotEmpty()) {
                            s.tasks.forEach { task ->
                                TaskRow(task = task)
                            }
                        } else {
                            Text(
                                "No task data available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Last refreshed: ${s.lastRefreshed}",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskInfo) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot
        Surface(
            modifier = Modifier.size(8.dp),
            shape    = RoundedCornerShape(50),
            color    = statusColor(task.status)
        ) {}
        Spacer(Modifier.width(10.dp))

        // Task label
        Text(
            text     = task.label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Runtime (if available)
        if (task.runtimeSeconds > 0) {
            Text(
                text       = "${task.runtimeSeconds}s",
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
        }

        // Status badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = statusBgColor(task.status)
        ) {
            Text(
                text     = task.status.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style    = MaterialTheme.typography.labelSmall,
                color    = statusColor(task.status),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun statusColor(status: String): Color = when (status.lowercase()) {
    "running"   -> ProxyBlue
    "completed" -> ProxyGreen
    "failed"    -> ProxyRed
    "retrying"  -> ProxyAmber
    "skipped"   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
}

@Composable
private fun statusBgColor(status: String): Color = when (status.lowercase()) {
    "running"   -> ProxyBlue.copy(alpha = 0.15f)
    "completed" -> ProxyGreen.copy(alpha = 0.15f)
    "failed"    -> ProxyRed.copy(alpha = 0.15f)
    "retrying"  -> ProxyAmber.copy(alpha = 0.15f)
    else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
}
