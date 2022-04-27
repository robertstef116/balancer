package com.robert

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class HealthChecker(
    val worker: WorkerNode,
    private val checkInterval: Long,
    private val checkTimeout: Long,
    private val maxNumberOfFailures: Int,
    private var numberOfRelevantPerformanceMetrics: Int,
    val onFailure: (HealthChecker) -> Unit
) {
    companion object {
        private val log = LoggerFactory.getLogger(HealthChecker::class.java)
        private val context = newSingleThreadContext("HEALTH_CHECK")

        private fun <T> addMetric(latestMetrics: LinkedList<T>, newValue: T, limit: Int) {
            while (latestMetrics.size > limit) {
                latestMetrics.remove()
            }
            latestMetrics.add(newValue)
        }
    }

    private lateinit var healthCheckerFn: Job
    private val url = "http://${worker.host}:${worker.port}/resource"
    private var nrOfFailures = 0

    private val resourcesLock = ReentrantReadWriteLock()

    private val latestAvailableCpus = LinkedList<Double>()
    private val latestAvailableMemories = LinkedList<Long>()

    var deploymentsPerformance: List<DeploymentPerformance> = listOf()
        get() = resourcesLock.read { field }

    fun getAverageAvailableCpu(): Double {
        resourcesLock.read {
            return latestAvailableCpus.average()
        }
    }

    fun getAverageAvailableMemory(): Double {
        resourcesLock.read {
            return latestAvailableMemories.average()
        }
    }

    fun start() {
        val currentHealthChecker = this

        healthCheckerFn = CoroutineScope(context).launch {
            while (true) {
                try {
                    log.debug("Health check {}:{}", worker.host, worker.port)
                    val res = HttpClient.get<WorkerResourceResponse>(url, checkTimeout)
                    resourcesLock.write {
                        val availableCpu = 100 - res.resourcesInfo.cpuLoad
                        val availableMemory = res.resourcesInfo.availableMemory
                        addMetric(latestAvailableCpus, availableCpu, numberOfRelevantPerformanceMetrics)
                        addMetric(latestAvailableMemories, availableMemory, numberOfRelevantPerformanceMetrics)

                        // remove performance data of the container which no longer exists
                        val deploymentsPerformanceToRemove = mutableSetOf<DeploymentPerformance>()
                        deploymentsPerformance.forEach { containerStats ->
                            if (res.containersStats.find { it.containerId == containerStats.containerId } == null) {
                                deploymentsPerformanceToRemove.add(containerStats)
                            }
                        }
                        deploymentsPerformance = deploymentsPerformance - deploymentsPerformanceToRemove

                        // add new deployed containers
                        val deploymentsPerformanceToAdd = mutableListOf<DeploymentPerformance>()

                        for (containerStats in res.containersStats) {
                            var deploymentPerformance = deploymentsPerformance.find {
                                it.containerId == containerStats.containerId
                            }

                            // create deploymentPerformance data for new containers
                            if (deploymentPerformance == null) {
                                deploymentPerformance = DeploymentPerformance(containerStats.containerId)
                                deploymentsPerformanceToAdd.add(deploymentPerformance)
                            }

                            // update deploymentPerformance data
                            deploymentPerformance.resourcesLock.write {
                                addMetric(
                                    deploymentPerformance.latestAvailableCpus,
                                    100 - containerStats.cpuLoad,
                                    numberOfRelevantPerformanceMetrics
                                )
                                addMetric(
                                    deploymentPerformance.latestAvailableMemories,
                                    containerStats.availableMemory,
                                    numberOfRelevantPerformanceMetrics
                                )
                            }
                        }

                        deploymentsPerformance = deploymentsPerformance + deploymentsPerformanceToAdd
                    }
                    nrOfFailures = 0
                } catch (e: Exception) {
                    log.error("failed health check on host {}", worker.host)
                    nrOfFailures++
                    if (nrOfFailures >= maxNumberOfFailures) {
                        onFailure(currentHealthChecker)
                    }
                }
                delay(checkInterval)
            }
        }
    }

    fun stop() {
        healthCheckerFn.cancel()
    }

    fun updateNumberOfRelevantPerformanceMetrics(newNumberOfRelevantPerformanceMetrics: Int) {
        resourcesLock.write {
            numberOfRelevantPerformanceMetrics = newNumberOfRelevantPerformanceMetrics
        }
    }
}
