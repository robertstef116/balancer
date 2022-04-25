package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo
import java.util.concurrent.atomic.AtomicLong


class LeastConnectionAlgorithm(availableTargets: List<PathTargetResource>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.LEAST_CONNECTION
    private val targets = HashMap<PathTargetResource, AtomicLong>()

    init {
        for (target in availableTargets) {
            targets[target] = AtomicLong(0)
        }
    }

    override fun selectTargetDeployment(): SelectedDeploymentInfo {
        val minTarget = targets.entries.minByOrNull { it.value.get() }
        minTarget!!.value.incrementAndGet()
        return SelectedDeploymentInfo(
            minTarget.key.host,
            minTarget.key.port,
            ""
        )
    }

    override fun registerProcessingFinished(deploymentInfo: SelectedDeploymentInfo) {
        targets[PathTargetResource(deploymentInfo.host, deploymentInfo.port)]!!.decrementAndGet()
    }
}
