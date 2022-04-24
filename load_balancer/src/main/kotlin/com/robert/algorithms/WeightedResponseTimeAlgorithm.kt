package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

class WeightedResponseTimeAlgorithm : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.WEIGHTED_RESPONSE_TIME

    override fun selectTargetDeployment(availableTargets: List<PathTargetResource>): SelectedDeploymentInfo {
        TODO("Not yet implemented")
    }

    override fun registerProcessingFinished(requestReference: String) {
        TODO("Not yet implemented")
    }
}
