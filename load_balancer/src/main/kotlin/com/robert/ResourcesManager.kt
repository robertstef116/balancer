package com.robert

import com.robert.algorithms.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class ResourcesManager {
    private val storage = Storage()
    private var workers: List<WorkerNode>
    private var workflows: List<Workflow>
    private val deployments: List<Deployment>
    private val lock = ReentrantReadWriteLock()

    var pathsMapping: Map<WorkflowPath, List<PathTargetResource>>
        get() = lock.read { pathsMapping }
        private set

    init {
        DynamicConfigProperties.setPropertiesData(storage.getConfigs())
        workers = storage.getWorkers()
        workflows = storage.getWorkflows()
        deployments = storage.getDeployments()

        ensureResourcesConsistency()

        pathsMapping = storage.getPathsMapping()
        // algorithms need to be reinitialized on deployment changes
        // weighted response time need to be reinitialized also on dynamic property change
        loadPathsAlgorithms(pathsMapping)
    }

    companion object {
        private fun loadPathsAlgorithms(pathsMapping: Map<WorkflowPath, List<PathTargetResource>>) {
            for (pathMapping in pathsMapping.entries) {
                val workflowPath = pathMapping.key
                when (workflowPath.algorithm) {
                    LBAlgorithms.RANDOM -> workflowPath.deploymentSelectionAlgorithm =
                        RandomAlgorithm(pathMapping.value)
                    LBAlgorithms.ROUND_ROBIN -> workflowPath.deploymentSelectionAlgorithm =
                        RoundRobinAlgorithm(pathMapping.value)
                    LBAlgorithms.LEAST_CONNECTION -> workflowPath.deploymentSelectionAlgorithm =
                        LeastConnectionAlgorithm(pathMapping.value)
                    LBAlgorithms.WEIGHTED_RESPONSE_TIME -> workflowPath.deploymentSelectionAlgorithm =
                        WeightedResponseTimeAlgorithm(pathMapping.value)
                }
            }
        }
    }

    // digest auth: https://stackoverflow.com/questions/53028003/apache-httpclient-digestauth-doesnt-forward-opaque-value-from-challenge/53028597#53028597
    private fun ensureResourcesConsistency() {
        // TO DO
    }
}
