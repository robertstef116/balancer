package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.algorithm.WeightedScoreAssigner.Companion.LOG
import com.robert.loadbalancer.model.BalancingAlgorithmData
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class AdaptiveAssigner : BalancingAlgorithm {
    companion object {
        private const val RESPONSE_TIMES_USED_COUNT = 1000
        private const val SCORE_USED_COUNT = 100
    }

    @Volatile
    private var targets = listOf<AdaptiveWorkflowDeploymentData>()

    @Volatile
    private var totalWeight = 0.0
    private val overallActiveRequestCounter = AtomicLong(0)

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.ADAPTIVE
    }

    override fun updateData(data: List<WorkflowDeploymentData>) {
        val newTargets = mutableListOf<AdaptiveWorkflowDeploymentData>()
        var maxScore = 0.0
        var minScore = Double.MAX_VALUE
        var maxResponseTime = 0.0
        var minResponseTime = Double.MAX_VALUE

        data.forEach { workflowDeploymentData ->
            val target = targets.find { it.workflowDeploymentData == workflowDeploymentData }
                ?: AdaptiveWorkflowDeploymentData()
            target.workflowDeploymentData = workflowDeploymentData
            target.addScore(workflowDeploymentData.score)
            if (target.averageScore > maxScore) {
                maxScore = target.averageScore
            }
            if (target.averageScore < minScore) {
                minScore = target.averageScore
            }
            if (target.averageResponseTime > maxResponseTime) {
                maxResponseTime = target.averageResponseTime
            }
            if (target.averageResponseTime < minResponseTime) {
                minResponseTime = target.averageResponseTime
            }
            target.algorithmScore = 1.7 * (((target.averageResponseTime - minResponseTime) / ((maxResponseTime - minResponseTime).coerceAtLeast(0.000001))).coerceIn(0.00001, 1.0)) +
                    ((target.averageScore - minScore) / ((maxScore - minScore).coerceAtLeast(0.000001))).coerceIn(0.00001, 1.0)
            newTargets.add(target)
        }

        val overallResponseTimeAverage = newTargets.sumOf { it.algorithmScore }
        newTargets.forEach { it.weight = (it.algorithmScore / overallResponseTimeAverage).coerceAtLeast(0.2 / newTargets.size) }
        totalWeight = newTargets.sumOf { it.weight }

        targets = newTargets
    }

    override fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair {
        val allTargets = BalancingAlgorithm.getAvailableTargetsData(targets, blacklistedTargets)
        if (allTargets.isEmpty()) {
            throw NotFoundException()
        }
        var rand = Random.nextDouble(totalWeight + 0.6)
        overallActiveRequestCounter.incrementAndGet()
        for (target in allTargets) {
            val weight = target.weight + 0.6 * (1 - target.activeRequestsCounter.get() / overallActiveRequestCounter.get().coerceAtLeast(1))
            if (rand < weight) {
                target.activeRequestsCounter.incrementAndGet()
                return target.getHostInfo()
            }
            rand -= weight
        }

        LOG.warn("Unable to pick a target by algorithm score, selecting first if exists")
        allTargets[0].activeRequestsCounter.incrementAndGet()
        return allTargets[0].getHostInfo()
    }

    override fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {
        overallActiveRequestCounter.decrementAndGet()
        targets.find { it.workflowDeploymentData.host == target.host && it.workflowDeploymentData.port == target.port }
            ?.also {
                it.activeRequestsCounter.decrementAndGet()
                if (responseType == LoadBalancerResponseType.OK) {
                    it.addResponseTime(responseTime)
                }
            }
    }

    private inner class AdaptiveWorkflowDeploymentData : BalancingAlgorithmData() {
        @Volatile
        lateinit var workflowDeploymentData: WorkflowDeploymentData
        val activeRequestsCounter = AtomicLong()
        var averageResponseTime = 0.0
        var responseTimeCount = 0
        var averageScore = 0.0
        var scoreCount = 0
        var weight = 0.0
        var algorithmScore = 0.0

        @Synchronized
        fun addResponseTime(responseTime: Long) {
            averageResponseTime = (averageResponseTime * responseTimeCount + responseTime) / (responseTimeCount + 1)
            responseTimeCount = (responseTimeCount + 1) % RESPONSE_TIMES_USED_COUNT
        }

        @Synchronized
        fun addScore(score: Double) {
            averageScore = (averageScore * scoreCount + score) / (scoreCount + 1)
            scoreCount = (scoreCount + 1) % SCORE_USED_COUNT
        }

        override fun getDeploymentData(): WorkflowDeploymentData {
            return workflowDeploymentData
        }
    }
}