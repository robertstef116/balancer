package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

class ResourceBasedAlgorithm : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.RESOURCE_BASED

    override fun selectTargetDeployment(availableTargets: List<PathTargetResource>): SelectedDeploymentInfo {
        TODO("Not yet implemented")
    }

    override fun registerProcessingFinished(requestReference: String) {
        TODO("Not yet implemented")
    }
}
