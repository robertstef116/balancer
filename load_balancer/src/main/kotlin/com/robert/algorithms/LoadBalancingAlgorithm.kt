package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

interface LoadBalancingAlgorithm {
    val algorithm: LBAlgorithms
    fun updateTargets(newTargets: List<PathTargetResource>)
    fun selectTargetDeployment(): SelectedDeploymentInfo
    fun registerProcessingFinished(deploymentInfo: SelectedDeploymentInfo)
}
