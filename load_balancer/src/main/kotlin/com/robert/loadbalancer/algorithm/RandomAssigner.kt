package com.robert.loadbalancer.algorithm

import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import kotlin.random.Random

class RandomAssigner : BalancingAlgorithm {
    @Volatile
    private var targets = listOf<WorkflowDeploymentData>()

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.RANDOM
    }

    override fun updateData(data: List<WorkflowDeploymentData>) {
        targets = data
    }

    override fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair {
        BalancingAlgorithm.getAvailableTargets(targets, blacklistedTargets).also { targets ->
            if (targets.isEmpty()) {
                throw NotFoundException()
            }
            return targets[Random.nextInt(targets.size)].let { HostPortPair(it.workflowId, it.host, it.port) }
        }
    }
}