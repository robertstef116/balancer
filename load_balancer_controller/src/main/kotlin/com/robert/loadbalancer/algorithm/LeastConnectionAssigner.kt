package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import java.util.concurrent.atomic.AtomicLong

class LeastConnectionAssigner : BalancingAlgorithm {
    @Volatile
    private var targets = listOf<LeastConnectionWorkflowDeploymentData>()

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.LEAST_CONNECTION
    }

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

    override fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {
        targets.find { it.workflowDeploymentData.host == target.host && it.workflowDeploymentData.port == it.workflowDeploymentData.port }
            ?.activeRequestsCounter
            ?.decrementAndGet()
    }

    private inner class LeastConnectionWorkflowDeploymentData(
        val workflowDeploymentData: WorkflowDeploymentData,
        val activeRequestsCounter: AtomicLong
    )
}

