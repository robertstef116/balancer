package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo
import java.util.concurrent.atomic.AtomicInteger

class RoundRobinAlgorithm(private var availableTargets: List<PathTargetResource>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.ROUND_ROBIN
    private val currentIdx = AtomicInteger(0)

    override fun updateTargets(newTargets: List<PathTargetResource>) {
        availableTargets = newTargets
    }

    override fun selectTargetDeployment(): SelectedDeploymentInfo {
        val targetIdx = currentIdx.getAndIncrement() % availableTargets.size
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
