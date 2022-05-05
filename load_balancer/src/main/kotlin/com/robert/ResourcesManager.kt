package com.robert

import com.robert.algorithms.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
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

    private val pathsMappingLock = ReentrantReadWriteLock()
    private val workersLock = ReentrantReadWriteLock()
    private val initialized = AtomicBoolean(false)
    private val initializationWaiters = ConcurrentLinkedQueue<() -> Unit>()
    private val initializationLock = ReentrantLock()

    var pathsMapping: Map<WorkflowPath, List<PathTargetResource>> = emptyMap()
        get() = pathsMappingLock.read { field }
        private set

    var workers: List<WorkerNode> = emptyList()
        get() = workersLock.read { field }
        private set

    // use same lock as the workers
    var healthChecks: List<HealthChecker> = emptyList()
        get() = workersLock.read { field }
        private set

    fun getWorkflows() = storage.getWorkflows()
    fun getDeployments() = storage.getDeployments()

    fun loadConfigs() {
        log.debug("load configs")
        workersLock.write {
            numberOfRelevantPerformanceMetrics =
                DynamicConfigProperties.getIntPropertyOrDefault(Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS, 3)
            healthCheckTimeout =
                DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_TIMEOUT, 10000L)
            healthCheckInterval =
                DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_INTERVAL, 10000L)
            maxNumberOfHealthFailures =
                DynamicConfigProperties.getIntPropertyOrDefault(Constants.HEALTH_CHECK_MAX_FAILURES, 3)

            healthChecks.forEach {
                it.updateConfigs(
                    healthCheckInterval,
                    healthCheckTimeout,
                    maxNumberOfHealthFailures,
                    numberOfRelevantPerformanceMetrics
                )
            }

            val newRecomputeWeightInterval =
                DynamicConfigProperties.getIntPropertyOrDefault(
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

    private fun getWorkflowPathAlgorithms(
        algorithm: LBAlgorithms,
        targetResources: List<PathTargetResource>
    ): LoadBalancingAlgorithm {
        return when (algorithm) {
            LBAlgorithms.RANDOM -> RandomAlgorithm(targetResources)
            LBAlgorithms.ROUND_ROBIN -> RoundRobinAlgorithm(targetResources)
            LBAlgorithms.LEAST_CONNECTION -> LeastConnectionAlgorithm(targetResources)
            LBAlgorithms.WEIGHTED_RESPONSE_TIME -> WeightedResponseTimeAlgorithm(
                targetResources,
                recomputeWeightInterval
            )
        }
    }

    fun registerInitializationWaiter(cb: () -> Unit) {
        initializationLock.withLock {
            if (initialized.get()) {
                cb()
            } else {
                initializationWaiters.add(cb)
            }
        }
    }

    private fun onInitialized() {
        initializationLock.withLock {
            workersLock.read {
                for (healthCheck in healthChecks) {
                    if (!healthCheck.initialized.get()) {
                        return@withLock
                    }
                }
            }
            log.debug("resources initialized")
            initializationWaiters.forEach { it() }
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
            log.debug("worker failed {}", worker)
            workersLock.write {
                storage.disableWorker(worker.id)
                healthCheck.stop()
                workersChanged()
            }
        }
        healthCheck.start()
        return healthCheck
    }

    fun workersChanged() {
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
        val newPathsMapping = storage.getPathsMapping()

        pathsMappingLock.write {
            for (workflowPathPair in newPathsMapping.entries) {
                val oldWorkflowPath = pathsMapping.keys.find { it.path == workflowPathPair.key.path }
                if (oldWorkflowPath != null) {
                    workflowPathPair.key.deploymentSelectionAlgorithm = oldWorkflowPath.deploymentSelectionAlgorithm
                    workflowPathPair.key.deploymentSelectionAlgorithm.updateTargets(workflowPathPair.value)
                } else {
                    workflowPathPair.key.deploymentSelectionAlgorithm = getWorkflowPathAlgorithms(
                        workflowPathPair.key.algorithm,
                        workflowPathPair.value
                    )
                }
            }

            pathsMapping = newPathsMapping
        }
    }

    override fun refresh() {
        log.debug("resource manager changed")
        workersChanged()
        pathsMappingChanged()
    }
}
