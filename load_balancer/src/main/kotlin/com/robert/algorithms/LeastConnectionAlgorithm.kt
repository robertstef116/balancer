package com.robert.algorithms

import com.robert.balancing.ProcessedHeader
import com.robert.enums.LBAlgorithms
import com.robert.balancing.TargetData
import com.robert.balancing.RequestData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


class LeastConnectionAlgorithm(private var availableTargets: List<TargetData>) : LoadBalancingAlgorithm {
    override val algorithm = LBAlgorithms.LEAST_CONNECTION
    private val targets = ConcurrentHashMap<TargetData, AtomicLong>()

    init {
        updateTargets(availableTargets)
    }

    override fun updateTargets(newTargets: List<TargetData>) {
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

    override fun selectTargetDeployment(header: ProcessedHeader): RequestData {
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
