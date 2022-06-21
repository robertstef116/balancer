package com.robert.algorithms

import com.robert.enums.LBAlgorithms
import com.robert.balancing.RequestTargetData
import com.robert.balancing.RequestData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


class LeastConnectionAlgorithm(private var availableTargets: List<RequestTargetData>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.LEAST_CONNECTION
    private val targets = ConcurrentHashMap<RequestTargetData, AtomicLong>()

    init {
        updateTargets(availableTargets)
    }

    override fun updateTargets(newTargets: List<RequestTargetData>) {
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

    override fun selectTargetDeployment(): RequestData {
        val minTarget = targets.entries.minByOrNull { it.value.get() }
        minTarget!!.value.incrementAndGet()
        return RequestData(
            "",
            minTarget.key
        )
    }

    override fun registerProcessingFinished(deploymentInfo: RequestData) {
        targets[deploymentInfo.targetResource]?.decrementAndGet()
    }
}
