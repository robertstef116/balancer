package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo

class RandomAlgorithm(private val availableTargets: List<PathTargetResource>): LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.RANDOM

    override fun selectTargetDeployment(): SelectedDeploymentInfo {
        val targetIdx = (availableTargets.indices).random()
        val target = availableTargets[targetIdx]

        return SelectedDeploymentInfo(
            target.host,
            target.port,
            "" // no need for a reference
        )
    }

    override fun registerProcessingFinished(deploymentInfo: SelectedDeploymentInfo) {
        // no processing required
    }
}
