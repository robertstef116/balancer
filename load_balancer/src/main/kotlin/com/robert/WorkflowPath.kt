package com.robert

import com.robert.algorithms.LoadBalancingAlgorithm
import com.robert.enums.LBAlgorithms

data class WorkflowPath(
    val path: String,
    val algorithm: LBAlgorithms
) {
    lateinit var deploymentSelectionAlgorithm: LoadBalancingAlgorithm
}
