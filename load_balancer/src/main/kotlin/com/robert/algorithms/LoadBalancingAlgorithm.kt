package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource

interface LoadBalancingAlgorithm {
    val algorithm: LBAlgorithms
    fun selectTargetDeployment(availableTargets: List<PathTargetResource>): PathTargetResource
}
