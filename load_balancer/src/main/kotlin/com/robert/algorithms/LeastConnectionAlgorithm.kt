package com.robert.algorithms

import com.robert.LBAlgorithms
import com.robert.PathTargetResource
import com.robert.SelectedDeploymentInfo
import java.util.concurrent.atomic.AtomicLong


class LeastConnectionAlgorithm(private var availableTargets: List<PathTargetResource>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.LEAST_CONNECTION
    private val targets = HashMap<PathTargetResource, AtomicLong>()

    init {
        updateTargets(availableTargets)
    }

    override fun updateTargets(newTargets: List<PathTargetResource>) {
        for (target in targets.keys) {
            if (!newTargets.contains(target)) {
                targets.remove(target)
            }
        }

        for (target in newTargets) {
            if (targets[target] == null) {
                targets[target] = AtomicLong(0)
            }
        }

        availableTargets = newTargets
    }

    override fun selectTargetDeployment(): SelectedDeploymentInfo {
        val minTarget = targets.entries.minByOrNull { it.value.get() }
        minTarget!!.value.incrementAndGet()
        return SelectedDeploymentInfo(
            minTarget.key.host,
            minTarget.key.port,
            "",
            minTarget.key
        )
    }

    override fun registerProcessingFinished(deploymentInfo: SelectedDeploymentInfo) {
        targets[deploymentInfo.targetResource]?.decrementAndGet()
    }
}
