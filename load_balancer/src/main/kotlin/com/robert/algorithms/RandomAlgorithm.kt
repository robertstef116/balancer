package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

class RandomAlgorithm: LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.RANDOM

    override fun selectTargetDeployment(availableTargets: List<PathTargetResource>): SelectedDeploymentInfo {
        val targetIdx = (availableTargets.indices).random()
        val target = availableTargets[targetIdx]

        return SelectedDeploymentInfo(
            target.host,
            target.port,
            "" // no need for a reference
        )
    }

    override fun registerProcessingFinished(requestReference: String) {
        // no processing required
    }
}
