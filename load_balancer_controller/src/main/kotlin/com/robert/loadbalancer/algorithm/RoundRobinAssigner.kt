package com.robert.loadbalancer.algorithm

import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import java.util.concurrent.atomic.AtomicInteger

class RoundRobinAssigner : BalancingAlgorithm {
    @Volatile
    private var targets = listOf<WorkflowDeploymentData>()
    private val currentIdx = AtomicInteger(0)

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.ROUND_ROBIN
    }

    override fun updateData(data: List<WorkflowDeploymentData>) {
        targets = data
    }

    override fun getTarget(): HostPortPair {
        targets.also { targets ->
            if (targets.isEmpty()) {
                throw NotFoundException()
            }
            val targetIdx = currentIdx.getAndIncrement() % targets.size
            return targets[targetIdx].let {
                HostPortPair(it.workflowId, it.host, it.port)
            }
        }
    }
}