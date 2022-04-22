package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource

class RandomAlgorithm: LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.RANDOM

    override fun selectTargetDeployment(availableTargets: List<PathTargetResource>): PathTargetResource {
        val targetIdx = (0 ..availableTargets.size).random()

        return availableTargets[targetIdx]
    }
}
