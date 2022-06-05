package com.robert

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class HealthChecker(
    val worker: WorkerNode,
    private var checkInterval: Long,
    private var checkTimeout: Long,
    private var maxNumberOfFailures: Int,
    private var numberOfRelevantPerformanceMetrics: Int,
    private val onInitialize: () -> Unit,
    val onFailure: (HealthChecker) -> Unit
) {
    companion object {
        private val log = LoggerFactory.getLogger(HealthChecker::class.java)
        private val context = Dispatchers.IO.limitedParallelism(1)

        private fun <T> addMetric(latestMetrics: ConcurrentLinkedQueue<T>, newValue: T, limit: Int) {
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

    private val latestAvailableCpus = ConcurrentLinkedQueue<Double>()
    private val latestAvailableMemories = ConcurrentLinkedQueue<Long>()

    val initialized = AtomicBoolean(false)
    var lastCheckTimestamp = 0L
        private set

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
                    val timestamp = Instant.now().epochSecond
                    val res = HttpClient.get<WorkerResourceResponse>(url, checkTimeout)
                    addMetric(latestAvailableCpus, 100 - res.resourcesInfo.cpuLoad, numberOfRelevantPerformanceMetrics)
                    addMetric(latestAvailableMemories, res.resourcesInfo.availableMemory, numberOfRelevantPerformanceMetrics)

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
                            deploymentPerformance = DeploymentPerformance(containerStats.deploymentId, containerStats.containerId)
                            deploymentsPerformanceToAdd.add(deploymentPerformance)
                        }

                        // update deploymentPerformance data
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

                    deploymentsPerformance = deploymentsPerformance + deploymentsPerformanceToAdd
                    nrOfFailures = 0
                    lastCheckTimestamp = Instant.now().epochSecond
                    if (initialized.compareAndSet(false, true)) {
                        onInitialize()
                    }
                } catch (e: Exception) {
                    log.error("failed health check on host {}", worker.host)
                    nrOfFailures++
                    if (nrOfFailures >= maxNumberOfFailures) {
                        if (initialized.compareAndSet(false, true)) {
                            onInitialize()
                        }
                        onFailure(currentHealthChecker)
                    }
                }
                log.debug("Health check {}:{} done, next check in {} ms", worker.host, worker.port, checkInterval)
                delay(checkInterval)
            }
        }
    }

    fun stop() {
        healthCheckerFn.cancel()
    }

    fun updateConfigs(checkInterval: Long, checkTimeout: Long, maxNumberOfFailures: Int, numberOfRelevantPerformanceMetrics: Int) {
        log.debug("update configs")
        this.checkInterval = checkInterval
        this.checkTimeout = checkTimeout
        this.maxNumberOfFailures = maxNumberOfFailures
        this.numberOfRelevantPerformanceMetrics = numberOfRelevantPerformanceMetrics
    }
}
