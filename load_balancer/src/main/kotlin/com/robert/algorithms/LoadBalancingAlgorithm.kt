package com.robert.algorithms

import com.robert.balancing.ProcessedHeader
import com.robert.enums.LBAlgorithms
import com.robert.balancing.TargetData
import com.robert.balancing.RequestData

interface LoadBalancingAlgorithm {
    val algorithm: LBAlgorithms
    fun updateTargets(newTargets: List<TargetData>)
    fun selectTargetDeployment(header: ProcessedHeader): RequestData
    fun registerProcessingFinished(deploymentInfo: RequestData)
}
