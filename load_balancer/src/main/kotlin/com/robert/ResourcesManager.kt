package com.robert

import com.robert.algorithms.*
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// digest auth: https://stackoverflow.com/questions/53028003/apache-httpclient-digestauth-doesnt-forward-opaque-value-from-challenge/53028597#53028597
// or: https://mkyong.com/java/apache-httpclient-examples/

class ResourcesManager(private val storage: Storage) {
    companion object {
        private val log = LoggerFactory.getLogger(ResourcesManager::class.java)

        private fun getWorkflowPathAlgorithms(
            algorithm: LBAlgorithms,
            targetResources: List<PathTargetResource>
        ): LoadBalancingAlgorithm {
            return when (algorithm) {
                LBAlgorithms.RANDOM -> RandomAlgorithm(targetResources)
                LBAlgorithms.ROUND_ROBIN -> RoundRobinAlgorithm(targetResources)
                LBAlgorithms.LEAST_CONNECTION -> LeastConnectionAlgorithm(targetResources)
                LBAlgorithms.WEIGHTED_RESPONSE_TIME -> WeightedResponseTimeAlgorithm(targetResources)
            }
        }
    }

    private val numberOfRelevantPerformanceMetrics =
        DynamicConfigProperties.getIntPropertyOrDefault(Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS, 3)
    private val healthCheckTimeout =
        DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_TIMEOUT, 10000L)
    private val healthCheckInterval =
        DynamicConfigProperties.getLongPropertyOrDefault(Constants.HEALTH_CHECK_INTERVAL, 10000L)
    private val maxNumberOfHealthFailures =
        DynamicConfigProperties.getIntPropertyOrDefault(Constants.HEALTH_CHECK_MAX_FAILURES, 3)

    private val pathsMappingLock = ReentrantReadWriteLock()
    private val workersLock = ReentrantReadWriteLock()

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

    init {
        // should be moved into MasterChangesManager which calls the other managers on change
        DynamicConfigProperties.setPropertiesData(storage.getConfigs())

        workersChanged() // should be moved into a start function
        pathsMappingChanged()

        log.debug("resources initialized")
    }

    fun getWorkflows() = storage.getWorkflows()
    fun getDeployments() = storage.getDeployments()

    private fun initWorkerHealthChecks(worker: WorkerNode): HealthChecker {
        // TO DO: recover worker mechanism
        val healthCheck = HealthChecker(
            worker,
            healthCheckInterval,
            healthCheckTimeout,
            numberOfRelevantPerformanceMetrics,
            maxNumberOfHealthFailures
        ) { healthCheck ->
            log.debug("worker failed {}", worker)
            workersLock.write {
                storage.disableWorker(worker.id)
                healthCheck.stop()
                workersChanged()

//                healthChecks = healthChecks.filter { it.worker != worker }
//                deployments
//                    .filter { it.workerId == worker.id }
//                    .forEach { d ->
//                        val workflow = workflows.find { w -> d.workflowId == w.id }!!
//                        deployWorkflow(workflow)
//                    }
            }
        }
        healthCheck.start()
        return healthCheck
    }

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

    private fun pathsMappingChanged() {
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
}
