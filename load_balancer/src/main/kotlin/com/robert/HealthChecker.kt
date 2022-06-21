package com.robert

import com.robert.api.response.WorkerResourceResponse
import com.robert.resources.performance.DeploymentPerformanceData
import com.robert.resources.Worker
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.properties.Delegates

class HealthChecker(
    val worker: Worker,
    private val onInitialize: () -> Unit,
    val onFailure: () -> Unit
) {
    companion object {
        private val log = LoggerFactory.getLogger(HealthChecker::class.java)
        private val context = Dispatchers.IO.limitedParallelism(1)

        private var checkInterval by Delegates.notNull<Long>()
        private var checkTimeout by Delegates.notNull<Long>()
        private var maxNumberOfFailures by Delegates.notNull<Int>()
        private var relevantPerformanceMetricsNumber by Delegates.notNull<Int>()

        private fun <T> addMetric(latestMetrics: ConcurrentLinkedQueue<T>, newValue: T, limit: Int) {
            while (latestMetrics.size > limit) {
                latestMetrics.remove()
            }
            latestMetrics.add(newValue)
        }

        suspend fun doHealthCheck(worker: Worker): WorkerResourceResponse {
            log.debug("Health check {}:{}", worker.host, worker.port)
            val url = "http://${worker.host}:${worker.port}/resource"
            return HttpClient.get(url, checkTimeout)
        }

        fun reloadDynamicConfigs() {
            log.debug("reload health checker dynamic configs")
            relevantPerformanceMetricsNumber = DynamicConfigProperties.getIntPropertyOrDefault(Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS, 3)
            checkTimeout =  DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_TIMEOUT, 10000L)
            checkInterval = DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_INTERVAL, 10000L)
            maxNumberOfFailures = DynamicConfigProperties.getIntPropertyOrDefault(Constants.HEALTH_CHECK_MAX_FAILURES, 3)
        }
    }

    private var nrOfFailures = 0
    private var runHealthCheck = true

    private val resourcesLock = ReentrantReadWriteLock()

    private val latestAvailableCpus = ConcurrentLinkedQueue<Double>()
    private val latestAvailableMemories = ConcurrentLinkedQueue<Long>()

    val initialized = AtomicBoolean(false)
    var lastCheckTimestamp = 0L
        private set

    var deploymentsPerformance: List<DeploymentPerformanceData> = listOf()
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
        log.debug("starting health check {} {}", worker.host, worker.port)

        CoroutineScope(context).launch {
            while (runHealthCheck) {
                try {
                    val res = doHealthCheck(worker)

                    addMetric(latestAvailableCpus, 100 - res.performanceData.cpuLoad, relevantPerformanceMetricsNumber)
                    addMetric(latestAvailableMemories, res.performanceData.availableMemory, relevantPerformanceMetricsNumber)

                    // remove performance data of the container which no longer exists
                    val deploymentsPerformanceToRemove = mutableSetOf<DeploymentPerformanceData>()
                    deploymentsPerformance.forEach { containerStats ->
                        if (res.containersStats.find { it.containerId == containerStats.containerId } == null) {
                            deploymentsPerformanceToRemove.add(containerStats)
                        }
                    }
                    deploymentsPerformance = deploymentsPerformance - deploymentsPerformanceToRemove

                    // add new deployed containers
                    val deploymentsPerformanceToAdd = mutableListOf<DeploymentPerformanceData>()

                    for (containerStats in res.containersStats) {
                        var deploymentPerformance = deploymentsPerformance.find {
                            it.containerId == containerStats.containerId
                        }

                        // create deploymentPerformance data for new containers
                        if (deploymentPerformance == null) {
                            deploymentPerformance = DeploymentPerformanceData(containerStats.deploymentId, containerStats.containerId)
                            deploymentsPerformanceToAdd.add(deploymentPerformance)
                        }

                        // update deploymentPerformance data
                        addMetric(
                            deploymentPerformance.latestAvailableCpus,
                            100 - containerStats.cpuLoad,
                            relevantPerformanceMetricsNumber
                        )
                        addMetric(
                            deploymentPerformance.latestAvailableMemories,
                            containerStats.availableMemory,
                            relevantPerformanceMetricsNumber
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
                        terminate()
                        onFailure()
                    }
                }
                log.debug("Health check {}:{} done, next check in {} ms", worker.host, worker.port, checkInterval)
                delay(checkInterval)
            }
        }
    }

    fun terminate() {
        runHealthCheck = false
        if (initialized.compareAndSet(false, true)) {
            onInitialize()
        }
    }
}
