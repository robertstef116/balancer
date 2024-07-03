package com.robert.loadbalancer.algorithm

import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData

class RandomAssigner : BalancingAlgorithm {
    @Volatile
    private var targets = listOf<WorkflowDeploymentData>()

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.RANDOM
    }

    override fun updateData(data: List<WorkflowDeploymentData>) {
        targets = data
    }

    override fun getTarget(): HostPortPair {
        targets.also { targets ->
            if (targets.isEmpty()) {
                throw NotFoundException()
            }
            return targets[(targets.indices).random()].let { HostPortPair(it.workflowId, it.host, it.port) }
        }
    }
}