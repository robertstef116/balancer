package com.robert

import com.robert.algorithms.*
import com.robert.balancing.TargetData
import com.robert.enums.LBAlgorithms
import com.robert.enums.WorkerStatusDepr
import com.robert.resources.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

// digest auth: https://stackoverflow.com/questions/53028003/apache-httpclient-digestauth-doesnt-forward-opaque-value-from-challenge/53028597#53028597
// or: https://mkyong.com/java/apache-httpclient-examples/

class ResourcesManager(private val storage: Storage) : UpdateAwareManager(Constants.WORKER_SERVICE_KEY), BackgroundService {
    companion object {
        private val log = LoggerFactory.getLogger(ResourcesManager::class.java)
        private val context = Dispatchers.IO.limitedParallelism(1)
    }

    private var workersCheckInterval by Delegates.notNull<Long>()

    private var initialized = false
    private var started = false

    private val initializationLock = ReentrantLock()
    private val algorithmConfigLock = ReentrantLock()
    private val initializationWaiters = ConcurrentLinkedQueue<() -> Unit>()

    var pathsMapping: Map<WorkflowPath, List<TargetData>> = emptyMap()
    var workers: List<Worker> = emptyList()
    var healthChecks: List<HealthChecker> = emptyList()

    fun getWorkflows() = storage.getWorkflows()
    fun getDeployments() = storage.getDeployments()

    fun reloadDynamicConfigs() {
        log.debug("reload dynamic configs")
        workersCheckInterval = DynamicConfigProperties.getLongPropertyOrDefault(Constants.WORKERS_CHECK_INTERVAL, 60000L)
    }

    private fun getWorkflowPathAlgorithms(algorithm: LBAlgorithms, targetResources: List<TargetData>): LoadBalancingAlgorithm {
        return when (algorithm) {
            LBAlgorithms.RANDOM -> RandomAlgorithm(targetResources)
            LBAlgorithms.ROUND_ROBIN -> RoundRobinAlgorithm(targetResources)
            LBAlgorithms.LEAST_CONNECTION -> LeastConnectionAlgorithm(targetResources)
            LBAlgorithms.WEIGHTED_RESPONSE_TIME -> WeightedResponseTimeAlgorithm(targetResources)
            LBAlgorithms.ADAPTIVE -> AdaptiveAlgorithmController(targetResources)
        }
    }

    fun registerInitializationWaiter(cb: () -> Unit) {
        initializationLock.withLock {
            if (initialized) {
                cb()
            } else {
                initializationWaiters.add(cb)
            }
        }
    }

    private fun onInitialized() {
        initializationLock.withLock {
            healthChecks.forEach {
                if (!it.initialized.get()) {
                    return@withLock
                }
            }
            log.debug("resources initialized")
            initializationWaiters.forEach { it() }
            initialized = true
            run()
        }
    }

    private fun initWorkerHealthChecks(worker: Worker): HealthChecker {
        log.debug("init workers health checks")
        val healthCheck = HealthChecker(worker, this::onInitialized) {
            log.debug("worker check failed {}", worker)
            storage.disableWorker(worker.id, WorkerStatusDepr.STARTING)
            workersChanged()
            pathsMappingChanged()
        }
        healthCheck.start()
        return healthCheck
    }

    @Synchronized
    private fun workersChanged() {
        val newWorkers = storage.getWorkers()
        log.debug("workers changed, {} active workers", newWorkers.size)
        workers.filterNot { newWorkers.contains(it) }.forEach { worker ->
            val workerHealthCheck = healthChecks.find { it.worker.id == worker.id }
            if (workerHealthCheck != null) {
                workerHealthCheck.terminate()
                healthChecks = healthChecks - workerHealthCheck
            }
        }
        newWorkers.filterNot { workers.contains(it) }.forEach { worker ->
            healthChecks = healthChecks + initWorkerHealthChecks(worker)
        }
        workers = newWorkers

        // initialize when no worker configured
        if (!initialized) {
            onInitialized()
        }
    }

    fun pathsMappingChanged() {
        algorithmConfigLock.withLock {
            val newPathsMapping = storage.getPathsMapping()
            log.debug("paths mapping changed, new path mappings are {}", newPathsMapping)

            for ((workflowPath, targets) in newPathsMapping.entries) {
                val oldWorkflowPath = pathsMapping.keys.find { it.path == workflowPath.path }
                if (oldWorkflowPath != null) {
                    workflowPath.deploymentSelectionAlgorithm = oldWorkflowPath.deploymentSelectionAlgorithm
                    workflowPath.deploymentSelectionAlgorithm.updateTargets(targets)
                } else {
                    workflowPath.deploymentSelectionAlgorithm = getWorkflowPathAlgorithms(workflowPath.algorithm, targets)
                }
            }

            pathsMapping = newPathsMapping
        }
    }

    override fun start() {
        if (!started) {
            registerInitializationWaiter {
                run()
            }
        } else {
            log.warn("workers manager already started")
        }
    }

    private fun run() {
        log.debug("workers manager started")
        CoroutineScope(context).launch {
            while (true) {
                log.debug("processing workers")
                try {
                    val workersStarted = handleStaringWorkers()
                    val workersStopped = handleStoppingWorkers()

                    if (workersStarted > 0 || workersStopped > 0) {
                        log.debug("{} workers started and {} workers stopped, refreshing", workersStarted, workersStopped)
                        workersChanged()
                        if (workersStopped > 0) {
                            pathsMappingChanged()
                        }
                    }
                } catch (e: Exception) {
                    log.error("error processing workers, err = {}", e.message)
                }
                log.debug("processing workers done, next check in {} ms", workersCheckInterval)
                delay(workersCheckInterval)
            }
        }
    }

    private fun handleStoppingWorkers(): Int {
        val stoppingWorkers = storage.getWorkers(WorkerStatusDepr.STOPPING)
        log.trace("{} workers in stopping state", stoppingWorkers.size)
        var workersStopped = 0

        for (worker in stoppingWorkers) {
            storage.disableWorker(worker.id, WorkerStatusDepr.STOPPED)
            workersStopped++
        }

        return workersStopped
    }

    private suspend fun handleStaringWorkers(): Int {
        val startingWorkers = storage.getWorkers(WorkerStatusDepr.STARTING)
        log.trace("{} workers in starting state", startingWorkers.size)
        var workersStarted = 0

        for (worker in startingWorkers) {
            try {
                HealthChecker.doHealthCheck(worker)
                storage.enableWorker(worker.id)
                workersStarted++
            } catch (e: Exception) {
                log.debug("unable to connect to worker {}", worker)
            }
        }

        return workersStarted
    }

    override fun refresh() {
        log.debug("resource manager refresh")
        workersChanged()
        pathsMappingChanged()
    }
}
