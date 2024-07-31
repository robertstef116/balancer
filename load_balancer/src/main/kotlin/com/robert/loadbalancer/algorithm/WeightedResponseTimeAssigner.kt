package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.BalancingAlgorithmData
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
            val target = targets.find { it.workflowDeploymentData == workflowDeploymentData }
                ?: WeightedResponseTimeWorkflowDeploymentData()
            target.workflowDeploymentData = workflowDeploymentData
            newTargets.add(target)
        }
        val overallResponseTimeAverage = newTargets.sumOf { it.average }
        newTargets.forEach { it.weight = it.average / overallResponseTimeAverage }
        targets = newTargets
    }

    override fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair {
        var rand = Random.nextDouble()
        val allTargets = BalancingAlgorithm.getAvailableTargetsData(targets, blacklistedTargets)
        if (allTargets.isEmpty()) {
            throw NotFoundException()
        }
        for (target in allTargets) {
            val weight = target.weight
            if (rand < weight) {
                return target.getHostInfo()
            }
            rand -= weight
        }

        LOG.warn("Unable to pick a target by weight, selecting first if exists")
        return allTargets[0].getHostInfo()
    }

    override fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {
        if (responseType == LoadBalancerResponseType.OK) {
            targets.find { it.workflowDeploymentData.host == target.host && it.workflowDeploymentData.port == it.workflowDeploymentData.port }
                ?.addResponseTime(responseTime)
        }
    }

    private inner class WeightedResponseTimeWorkflowDeploymentData : BalancingAlgorithmData() {
        @Volatile
        lateinit var workflowDeploymentData: WorkflowDeploymentData
        var average = 0.0
        var count = 0
        var weight = 0.0

        @Synchronized
        fun addResponseTime(responseTime: Long) {
            average = (average * count + responseTime) / (count + 1)
            count = (count + 1) % RESPONSE_TIMES_USED_COUNT
        }

        override fun getDeploymentData(): WorkflowDeploymentData {
            return workflowDeploymentData
        }
    }
}