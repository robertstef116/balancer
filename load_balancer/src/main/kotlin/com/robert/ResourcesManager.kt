package com.robert

import com.robert.algorithms.RandomAlgorithm

class ResourcesManager {
    private val storage = Storage()
    private var workers: List<WorkerNode>
    private var workflows: List<Workflow>
    private val deployments: List<Deployment>
    var pathsMapping: Map<WorkflowPath, List<PathTargetResource>> private set

    init {
        workers = storage.getWorkers()
        workflows = storage.getWorkflows()
        deployments = storage.getDeployments()

        ensureResourcesConsistency()

        pathsMapping = storage.getPathsMapping()
        loadPathsAlgorithms(pathsMapping.keys)
    }

    companion object {
        private fun loadPathsAlgorithms(workflowPaths: Set<WorkflowPath>) {
            for (workflowPath in workflowPaths) {
                when(workflowPath.algorithm) {
                    LBAlgorithms.RANDOM -> workflowPath.deploymentSelectionAlgorithm = RandomAlgorithm()
                }
            }
        }
    }

    private fun ensureResourcesConsistency() {
        // TO DO
    }


}
