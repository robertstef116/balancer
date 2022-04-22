package com.robert

import com.robert.algorithms.LoadBalancingAlgorithm

data class WorkflowPath(
    val path: String,
    val algorithm: LBAlgorithms
) {
    lateinit var deploymentSelectionAlgorithm: LoadBalancingAlgorithm
}
