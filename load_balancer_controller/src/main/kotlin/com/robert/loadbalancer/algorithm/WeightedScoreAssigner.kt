package com.robert.loadbalancer.algorithm

import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.BalancingAlgorithmData
import com.robert.loadbalancer.model.HostPortPair
import com.robert.logger
import com.robert.scaling.client.model.WorkflowDeploymentData
import kotlin.random.Random

class WeightedScoreAssigner : BalancingAlgorithm {
    companion object {
        val LOG by logger()
        private const val RESPONSE_TIMES_USED_COUNT = 100
    }

    @Volatile
    private var targets = listOf<WeightedScoreWorkflowDeploymentData>()

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.WEIGHTED_SCORE
    }

    @Synchronized
    override fun updateData(data: List<WorkflowDeploymentData>) {
        val newTargets = mutableListOf<WeightedScoreWorkflowDeploymentData>()
        data.forEach { workflowDeploymentData ->
            val target = targets.find { it.workflowDeploymentData == workflowDeploymentData }
                ?: WeightedScoreWorkflowDeploymentData()
            target.updateWorkflowDeploymentData(workflowDeploymentData)
            newTargets.add(target)
        }
        val overallResponseTimeAverage = newTargets.sumOf { it.average }
        newTargets.forEach { it.weight = it.average / overallResponseTimeAverage }
        targets = newTargets
    }

    override fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair {
        var rand = Random.nextDouble()
        val allTargets = BalancingAlgorithm.getAvailableTargets(targets, blacklistedTargets)
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

    private inner class WeightedScoreWorkflowDeploymentData: BalancingAlgorithmData() {
        @Volatile
        lateinit var workflowDeploymentData: WorkflowDeploymentData

        var average: Double = 0.0
        var count: Int = 0
        var weight: Double = 0.0

        @Synchronized
        fun updateWorkflowDeploymentData(workflowDeploymentData: WorkflowDeploymentData) {
            this.workflowDeploymentData = workflowDeploymentData
            average = (average * count + workflowDeploymentData.score) / (count + 1)
            count = (count + 1) % RESPONSE_TIMES_USED_COUNT
        }

        override fun getWorkflowDeploymentData(): WorkflowDeploymentData {
            return workflowDeploymentData
        }
    }
}