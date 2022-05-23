package com.robert

import com.robert.algorithms.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write
import kotlin.properties.Delegates

// digest auth: https://stackoverflow.com/questions/53028003/apache-httpclient-digestauth-doesnt-forward-opaque-value-from-challenge/53028597#53028597
// or: https://mkyong.com/java/apache-httpclient-examples/

class ResourcesManager(private val storage: Storage) : UpdateAwareManager(Constants.WORKER_SERVICE_KEY) {
    companion object {
        private val log = LoggerFactory.getLogger(ResourcesManager::class.java)
    }

    private var numberOfRelevantPerformanceMetrics by Delegates.notNull<Int>()
    private var healthCheckTimeout by Delegates.notNull<Long>()
    private var healthCheckInterval by Delegates.notNull<Long>()
    private var maxNumberOfHealthFailures by Delegates.notNull<Int>()
    private var recomputeWeightInterval = Constants.DEFAULT_COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL

    private var initialized = false

    private val initializationLock = ReentrantLock()
    private val pathsMappingLock = ReentrantReadWriteLock()
    private val workersConfigLock = ReentrantLock()
    private val algorithmConfigLock = ReentrantLock()
    private val initializationWaiters = ConcurrentLinkedQueue<() -> Unit>()

    var pathsMapping: Map<WorkflowPath, List<PathTargetResource>> = emptyMap()
    var workers: List<WorkerNode> = emptyList()
    var healthChecks: List<HealthChecker> = emptyList()

    fun getWorkflows() = storage.getWorkflows()
    fun getDeployments() = storage.getDeployments()

    fun reloadDynamicConfigs() {
        log.debug("reload dynamic configs")
        workersConfigLock.withLock {
            numberOfRelevantPerformanceMetrics =
                DynamicConfigProperties.getIntPropertyOrDefault(Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS, 3)
            healthCheckTimeout =
                DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_TIMEOUT, 10000L)
            healthCheckInterval =
                DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_INTERVAL, 10000L)
            maxNumberOfHealthFailures =
                DynamicConfigProperties.getIntPropertyOrDefault(Constants.HEALTH_CHECK_MAX_FAILURES, 3)

            healthChecks.forEach {
                it.updateConfigs(healthCheckInterval, healthCheckTimeout, maxNumberOfHealthFailures, numberOfRelevantPerformanceMetrics)
            }
        }

        algorithmConfigLock.withLock {
            val newRecomputeWeightInterval = DynamicConfigProperties.getIntPropertyOrDefault(
                Constants.COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL,
                Constants.DEFAULT_COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL
            )

            if (newRecomputeWeightInterval != recomputeWeightInterval) {
                pathsMappingLock.write {
                    recomputeWeightInterval = newRecomputeWeightInterval
                    for (workflowPath in pathsMapping.keys) {
                        if (workflowPath.deploymentSelectionAlgorithm is WeightedResponseTimeAlgorithm) {
                            (workflowPath.deploymentSelectionAlgorithm as WeightedResponseTimeAlgorithm).updateConfigs(
                                recomputeWeightInterval
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getWorkflowPathAlgorithms(algorithm: LBAlgorithms, targetResources: List<PathTargetResource>): LoadBalancingAlgorithm {
        return when (algorithm) {
            LBAlgorithms.RANDOM -> RandomAlgorithm(targetResources)
            LBAlgorithms.ROUND_ROBIN -> RoundRobinAlgorithm(targetResources)
            LBAlgorithms.LEAST_CONNECTION -> LeastConnectionAlgorithm(targetResources)
            LBAlgorithms.WEIGHTED_RESPONSE_TIME -> WeightedResponseTimeAlgorithm(targetResources, recomputeWeightInterval)
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
        }
    }

    private fun initWorkerHealthChecks(worker: WorkerNode): HealthChecker {
        // TO DO: recover worker mechanism
        val healthCheck = HealthChecker(
            worker,
            healthCheckInterval,
            healthCheckTimeout,
            numberOfRelevantPerformanceMetrics,
            maxNumberOfHealthFailures,
            this::onInitialized
        ) { healthCheck ->
            log.debug("worker check failed {}", worker)
            storage.disableWorker(worker.id)
            healthCheck.stop()
            workersChanged()
        }
        healthCheck.start()
        return healthCheck
    }

    @Synchronized
    private fun workersChanged() {
        val newWorkers = storage.getWorkers()
        workers.filterNot { newWorkers.contains(it) }.forEach { worker ->
            healthChecks.find { it.worker == worker }?.stop()
        }
        newWorkers.filterNot { workers.contains(it) }.forEach { worker ->
            healthChecks = healthChecks + initWorkerHealthChecks(worker)
        }
        workers = newWorkers
    }

    fun pathsMappingChanged() {
        algorithmConfigLock.withLock {
            val newPathsMapping = storage.getPathsMapping()

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

    override fun refresh() {
        log.debug("resource manager refresh")
        workersChanged()
        pathsMappingChanged()
    }
}
