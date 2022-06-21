package com.robert.resources.performance

import java.util.concurrent.ConcurrentLinkedQueue

data class DeploymentPerformanceData(
    val deploymentId: String,
    val containerId: String,
) {
    val latestAvailableCpus = ConcurrentLinkedQueue<Double>()
    val latestAvailableMemories = ConcurrentLinkedQueue<Long>()
}
