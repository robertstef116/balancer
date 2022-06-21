package com.robert.algorithms

import com.robert.enums.LBAlgorithms
import com.robert.balancing.RequestTargetData
import com.robert.balancing.RequestData

interface LoadBalancingAlgorithm {
    val algorithm: LBAlgorithms
    fun updateTargets(newTargets: List<RequestTargetData>)
    fun selectTargetDeployment(): RequestData
    fun registerProcessingFinished(deploymentInfo: RequestData)
}
