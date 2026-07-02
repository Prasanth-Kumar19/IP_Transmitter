📄 3. PipelineMonitorCard.kt
Path: app/src/main/java/com/proxyfarm/node/ui/components/PipelineMonitorCard.kt

package com.proxyfarm.node.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.proxyfarm.node.ui.theme.ProxyBlue
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed

@Composable
fun PipelineMonitorCard(
    status: PipelineStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pipeline Monitor",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text       = "listing-agent-dashboard / api/state",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onRefresh, enabled = !status.isLoading) {
                    if (status.isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Refresh,
                            "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Spacer(Modifier.height(12.dp))

            // ── Content ───────────────────────────────────────────
            AnimatedContent(
                targetState   = status,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label         = "pipeline"
            ) { s ->
                when {
                    s.isLoading -> LoadingContent()
                    s.hasError  -> ErrorContent(s.errorMessage ?: "Error")
                    else        -> PipelineContent(status = s, onRefresh = onRefresh)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier         = Modifier.fillMaxWidth().height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
    }
}

// ─────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String) {
    Row(
        modifier          = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Error, null,
            tint     = ProxyRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = ProxyRed
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Main Pipeline Content
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PipelineContent(
    status: PipelineStatus,
    onRefresh: () -> Unit
) {
    Column {

        // ── Pipeline status summary ───────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "Status: ${status.pipelineStatus.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = pipelineStatusColor(status.pipelineStatus),
                    fontFamily = FontFamily.Monospace
                )
                if (status.currentTask != "—" && status.currentTask.isNotBlank()) {
                    Text(
                        text  = "Current: ${status.currentTask}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ProxyGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text     = "${status.completedToday}/5 done",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = ProxyGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        )
        Spacer(Modifier.height(8.dp))

        // ── Task list ─────────────────────────────────────────────
        if (status.tasks.isNotEmpty()) {
            status.tasks.forEach { task -> TaskRow(task = task) }
        } else {
            Text(
                "No task data available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        )
        Spacer(Modifier.height(6.dp))

        // ── Last refreshed ────────────────────────────────────────
        Text(
            "Last refreshed: ${status.lastRefreshed}",
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontFamily = FontFamily.Monospace
        )

        // ── Live Logs ─────────────────────────────────────────────
        if (status.logs.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Article, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Live Logs (last ${status.logs.size} lines)",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable log box — matches log_dashboard.py style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                val scrollState = rememberScrollState(Int.MAX_VALUE)

                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    status.logs.forEach { line ->
                        val color = logLineColor(line)
                        Text(
                            text       = line,
                            style      = MaterialTheme.typography.labelSmall,
                            color      = color,
                            fontFamily = FontFamily.Monospace,
                            modifier   = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Task Row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TaskRow(task: TaskInfo) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape    = RoundedCornerShape(50),
            color    = taskStatusColor(task.status)
        ) {}
        Spacer(Modifier.width(10.dp))
        Text(
            text     = task.label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (task.runtimeSeconds > 0) {
            Text(
                text       = "${task.runtimeSeconds}s",
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = taskStatusBgColor(task.status)
        ) {
            Text(
                text       = task.status.uppercase(),
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style      = MaterialTheme.typography.labelSmall,
                color      = taskStatusColor(task.status),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Color Helpers
// ─────────────────────────────────────────────────────────────────

@Composable
private fun pipelineStatusColor(status: String): Color = when (status.lowercase()) {
    "running"   -> ProxyBlue
    "completed" -> ProxyGreen
    "failed"    -> ProxyRed
    else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
}

@Composable
private fun taskStatusColor(status: String): Color = when (status.lowercase()) {
    "running"   -> ProxyBlue
    "completed" -> ProxyGreen
    "failed"    -> ProxyRed
    "retrying"  -> ProxyAmber
    else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
}

@Composable
private fun taskStatusBgColor(status: String): Color = when (status.lowercase()) {
    "running"   -> ProxyBlue.copy(alpha = 0.15f)
    "completed" -> ProxyGreen.copy(alpha = 0.15f)
    "failed"    -> ProxyRed.copy(alpha = 0.15f)
    "retrying"  -> ProxyAmber.copy(alpha = 0.15f)
    else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
}

@Composable
private fun logLineColor(line: String): Color = when {
    line.contains("ERROR") || line.contains("FAILED")     -> ProxyRed
    line.contains("COMPLETED") || line.contains("SUCCESS") -> ProxyGreen
    line.contains("STARTING") || line.contains("PIPELINE") -> ProxyBlue
    line.contains("WARN") || line.contains("RETRY")        -> ProxyAmber
    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
}
