package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

class RoundRobinAlgorithm : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.ROUND_ROBIN

    override fun selectTargetDeployment(availableTargets: List<PathTargetResource>): SelectedDeploymentInfo {
        TODO("Not yet implemented")
    }

    override fun registerProcessingFinished(requestReference: String) {
        TODO("Not yet implemented")
    }
}
