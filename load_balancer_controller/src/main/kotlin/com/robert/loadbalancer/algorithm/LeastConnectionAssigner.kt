package com.robert.loadbalancer.algorithm

import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import java.util.concurrent.atomic.AtomicLong

class LeastConnectionAssigner : BalancingAlgorithm {
    @Volatile
    private var targets = listOf<LeastConnectionWorkflowDeploymentData>()

    override fun updateData(data: List<WorkflowDeploymentData>) {
        targets.also { currentTargets ->
            data.map {
                LeastConnectionWorkflowDeploymentData(it, currentTargets
                    .find { t -> t.workflowDeploymentData == it }
                    ?.activeRequestsCounter
                    ?: AtomicLong(0))
            }
        }
    }

    override fun getTarget(): HostPortPair {
        targets.also { targets ->
            if (targets.isEmpty()) {
                throw NotFoundException()
            }
            val minTarget = targets.minBy { it.activeRequestsCounter.get() }
            minTarget.activeRequestsCounter.incrementAndGet()
            return HostPortPair(minTarget.workflowDeploymentData.workflowId, minTarget.workflowDeploymentData.host, minTarget.workflowDeploymentData.port)
        }
    }

    override fun addResponseTimeData(responseTime: Long) {
        // NOP
    }

    private inner class LeastConnectionWorkflowDeploymentData(val workflowDeploymentData: WorkflowDeploymentData, val activeRequestsCounter: AtomicLong)
}

