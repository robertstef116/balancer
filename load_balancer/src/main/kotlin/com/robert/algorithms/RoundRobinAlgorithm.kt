package com.robert.algorithms

import com.robert.balancing.ProcessedHeader
import com.robert.enums.LBAlgorithms
import com.robert.balancing.TargetData
import com.robert.balancing.RequestData
import java.util.concurrent.atomic.AtomicInteger

class RoundRobinAlgorithm(private var availableTargets: List<TargetData>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.ROUND_ROBIN
    private val currentIdx = AtomicInteger(0)

    override fun updateTargets(newTargets: List<TargetData>) {
        availableTargets = newTargets
    }

    override fun selectTargetDeployment(header: ProcessedHeader): RequestData {
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
