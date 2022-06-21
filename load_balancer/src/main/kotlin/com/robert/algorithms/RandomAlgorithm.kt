package com.robert.algorithms

import com.robert.enums.LBAlgorithms
import com.robert.balancing.RequestTargetData
import com.robert.balancing.RequestData

class RandomAlgorithm(private var availableTargets: List<RequestTargetData>): LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.RANDOM

    override fun updateTargets(newTargets: List<RequestTargetData>) {
        availableTargets = newTargets
    }

    override fun selectTargetDeployment(): RequestData {
        val targetIdx = (availableTargets.indices).random()
        val target = availableTargets[targetIdx]

        return RequestData(
            "", // no need for a reference,
            target
        )
    }

    override fun registerProcessingFinished(deploymentInfo: RequestData) {
        // no processing required
    }
}
