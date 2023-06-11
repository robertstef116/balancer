package com.robert.controller

import com.robert.Constants
import com.robert.Env
import com.robert.docker.DockerContainerStats
import com.robert.resources.performance.DeploymentPerformanceData
import com.robert.resources.performance.PerformanceData
import com.robert.resources.performance.WorkerPerformanceData
import io.ktor.util.logging.*
import java.util.UUID

class Health(val workerId: UUID, private val onHealthStatusChange: (HealthStatus) -> Unit) : PerformanceData() {
    companion object {
        private val LOG =  KtorSimpleLogger(this::class.java.name)

        private val maxFailures = Env.getInt(Constants.HEALTH_CHECK_MAX_FAILURES, 3)
    }

    private var failures = 0

    var deploymentsPerformance = listOf<DeploymentPerformanceData>()
        private set

    var status = HealthStatus.UNINITIALIZED
        private set

    fun increaseFailures(): Boolean {
        failures++
        if (failures > maxFailures) {
            onHealthStatusChange(HealthStatus.UNHEALTHY)
            return true
        }
        return false
    }

    fun addWorkerPerformanceData(data: WorkerPerformanceData) {
        addCpuLoadData(data.cpuLoad)
        addMemoryData(data.availableMemory)
    }

    fun addContainersPerformanceData(data: List<DockerContainerStats>) {
        deploymentsPerformance = data.map { containerStat ->
            deploymentsPerformance.find {
                containerStat.containerId == it.containerId
            } ?: DeploymentPerformanceData(containerStat.deploymentId, containerStat.containerId).apply {
                addCpuLoadData(containerStat.cpuLoad)
                addMemoryData(containerStat.availableMemory)
            }
        }
    }

    fun cleanRemovedContainersData(data: List<DockerContainerStats>) {
        deploymentsPerformance = deploymentsPerformance.filter { deploymentPerformanceData ->
            data.any { it.deploymentId == deploymentPerformanceData.deploymentId }
        }
    }

    fun resetFailures() {
        failures = 0
        if (status == HealthStatus.UNHEALTHY) {
            status = HealthStatus.HEALTHY
            onHealthStatusChange(HealthStatus.HEALTHY)
        }
    }
}

enum class HealthStatus {
    HEALTHY,
    UNHEALTHY,
    UNINITIALIZED
}
