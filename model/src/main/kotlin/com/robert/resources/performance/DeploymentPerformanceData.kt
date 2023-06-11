package com.robert.resources.performance

import java.util.UUID

data class DeploymentPerformanceData(
    val deploymentId: UUID,
    val containerId: String,
): PerformanceData() {
    private var totalMemory = 0L

    fun setTotalMemory(value: Long) {
        totalMemory = value
    }

    fun getUtilization(): Double {
        return ((averageMemory * 100 / totalMemory) * memoryWeight + averageCpu * cpuWeight) / 2
    }
}
