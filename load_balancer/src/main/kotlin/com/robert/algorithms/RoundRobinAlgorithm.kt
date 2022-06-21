package com.robert.algorithms

import com.robert.enums.LBAlgorithms
import com.robert.balancing.RequestTargetData
import com.robert.balancing.RequestData
import java.util.concurrent.atomic.AtomicInteger

class RoundRobinAlgorithm(private var availableTargets: List<RequestTargetData>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.ROUND_ROBIN
    private val currentIdx = AtomicInteger(0)

    override fun updateTargets(newTargets: List<RequestTargetData>) {
        availableTargets = newTargets
    }

    override fun selectTargetDeployment(): RequestData {
        val targetIdx = currentIdx.getAndIncrement() % availableTargets.size
        val target = availableTargets[targetIdx]

        return RequestData(
            "", // no need for a reference
            target
        )
    }

    override fun registerProcessingFinished(deploymentInfo: RequestData) {
        // no processing required
    }
}
