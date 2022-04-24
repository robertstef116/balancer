package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

interface LoadBalancingAlgorithm {
    val algorithm: LBAlgorithms
    fun selectTargetDeployment(availableTargets: List<PathTargetResource>): SelectedDeploymentInfo
    fun registerProcessingFinished(requestReference: String)
}
