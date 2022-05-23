package com.robert

import java.util.concurrent.ConcurrentLinkedQueue

data class DeploymentPerformance(
    val deploymentId: String,
    val containerId: String,
) {
    val latestAvailableCpus = ConcurrentLinkedQueue<Double>()
    val latestAvailableMemories = ConcurrentLinkedQueue<Long>()
}
