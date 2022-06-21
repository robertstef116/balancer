package com.robert.api.response

import com.robert.docker.DockerContainerStats
import com.robert.resources.performance.WorkerPerformanceData

data class WorkerResourceResponse(
    val performanceData: WorkerPerformanceData,
    val containersStats: List<DockerContainerStats>
)
