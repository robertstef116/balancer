package com.robert.loadbalancer.algorithm

import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData

class RandomAssigner : BalancingAlgorithm {
    private var targets = listOf<WorkflowDeploymentData>()

    override fun updateData(data: List<WorkflowDeploymentData>) {
        targets = data
    }

    override fun getTarget(): HostPortPair {
        if (targets.isEmpty()) {
            throw NotFoundException()
        }
        return targets[(targets.indices).random()].let { HostPortPair(it.host, it.port) }
    }
}