package com.proxyfarm.node.data.model

/**
 * Individual task info from the Flask /api/state response.
 */
data class TaskInfo(
    val key:            String,
    val label:          String,
    val status:         String,
    val runtimeSeconds: Int = 0
)

/**
 * Full pipeline status parsed from /api/state + /api/logs endpoints.
 */
data class PipelineStatus(
    val isLoading:       Boolean        = true,
    val activePipelines: Int            = 0,
    val queuedJobs:      Int            = 0,
    val completedToday:  Int            = 0,
    val errorCount:      Int            = 0,
    val lastRefreshed:   String         = "—",
    val rawJson:         String         = "",
    val errorMessage:    String?        = null,
    val pipelineStatus:  String         = "unknown",
    val currentTask:     String         = "—",
    val tasks:           List<TaskInfo> = emptyList(),
    val logs:            List<String>   = emptyList()
) {
    companion object {
        val LOADING = PipelineStatus(isLoading = true)
        fun error(message: String) = PipelineStatus(
            isLoading    = false,
            errorMessage = message
        )
    }
    val hasError: Boolean  get() = errorMessage != null
    val isHealthy: Boolean get() = !hasError && errorCount == 0
}
