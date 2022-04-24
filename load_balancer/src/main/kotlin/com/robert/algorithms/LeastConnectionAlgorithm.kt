package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

class LeastConnectionAlgorithm : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.LEAST_CONNECTION

    override fun selectTargetDeployment(availableTargets: List<PathTargetResource>): SelectedDeploymentInfo {
        TODO("Not yet implemented")
    }

    override fun registerProcessingFinished(requestReference: String) {
        TODO("Not yet implemented")
    }
}
