package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.HostPortPair
import com.robert.logger
import com.robert.scaling.client.model.WorkflowDeploymentData
import kotlin.random.Random

class WeightedResponseTimeAssigner : BalancingAlgorithm {
    companion object {
        val LOG by logger()
        private const val RESPONSE_TIMES_USED_COUNT = 1000
    }

    @Volatile
    private var targets = listOf<WeightedResponseTimeWorkflowDeploymentData>()

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.WEIGHTED_RESPONSE_TIME
    }

    @Synchronized
    override fun updateData(data: List<WorkflowDeploymentData>) {
        val newTargets = mutableListOf<WeightedResponseTimeWorkflowDeploymentData>()
        data.forEach { workflowDeploymentData ->
            targets.find { it.workflowDeploymentData == workflowDeploymentData }
                ?.let {
                    it.workflowDeploymentData = workflowDeploymentData
                    newTargets.add(it)
                }
                ?: also {
                    newTargets.add(WeightedResponseTimeWorkflowDeploymentData(workflowDeploymentData, 0.0, 0, 0.0))
                }
        }
        val overallResponseTimeAverage = newTargets.sumOf { it.average }
        newTargets.forEach { it.weight = it.average / overallResponseTimeAverage }
        targets = newTargets
    }

    override fun getTarget(): HostPortPair {
        val rand = Random.nextDouble()
        for (target in targets) {
            val weight = target.weight
            if (rand < weight) {
                return HostPortPair(target.workflowDeploymentData.workflowId, target.workflowDeploymentData.host, target.workflowDeploymentData.port)
            }
        }

        LOG.warn("Unable to pick a target by weight, selecting first if exists")
        return targets.let {
            if (it.isEmpty()) {
                throw NotFoundException()
            }
            val target = it[0]
            HostPortPair(target.workflowDeploymentData.workflowId, target.workflowDeploymentData.host, target.workflowDeploymentData.port)
        }
    }

    override fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {
        if (responseType == LoadBalancerResponseType.OK) {
            targets.find { it.workflowDeploymentData.host == target.host && it.workflowDeploymentData.port == it.workflowDeploymentData.port }
                ?.addResponseTime(responseTime)
        }
    }

    private inner class WeightedResponseTimeWorkflowDeploymentData(
        @Volatile
        var workflowDeploymentData: WorkflowDeploymentData,
        var average: Double,
        var count: Int,
        var weight: Double
    ) {
        @Synchronized
        fun addResponseTime(responseTime: Long) {
            average = (average * count + responseTime) / (count + 1)
            count = (count + 1) % RESPONSE_TIMES_USED_COUNT
        }
    }
}