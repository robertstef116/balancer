package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.BalancingAlgorithmData
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import java.util.concurrent.atomic.AtomicLong

// Z-Score Standardization for avg response time
// https://www.aampe.com/blog/how-to-normalize-data-in-excel
// compute the mean and standard deviation in one loop using Welford's algorithm
class AdaptiveAssigner : BalancingAlgorithm {
    companion object {
        private const val RESPONSE_TIMES_USED_COUNT = 1000
        private const val SCORE_USED_COUNT = 100
    }

    @Volatile
    private var targets = listOf<AdaptiveWorkflowDeploymentData>()

    private var maxResponseTime = 0.0
    private var minResponseTime = 0.0

    private var maxScore = 0.0
    private var minScore = 0.0

    private val overallActiveRequestCounter = AtomicLong(0)

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.ADAPTIVE
    }

    override fun updateData(data: List<WorkflowDeploymentData>) {
        val newTargets = mutableListOf<AdaptiveWorkflowDeploymentData>()

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
            newTargets.add(target)
        }
        targets = newTargets
    }

    override fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair {
        return (BalancingAlgorithm.getAvailableTargetsData(targets, blacklistedTargets)
            .minByOrNull {
                1.5 * ((it.averageResponseTime - minResponseTime) / (maxResponseTime - minResponseTime)) +
                        (it.averageScore - minScore) / (maxScore - minScore) +
                        2 * (it.activeRequestsCounter.get() / overallActiveRequestCounter.get())
            }
            ?: throw NotFoundException())
            .let {
                overallActiveRequestCounter.incrementAndGet()
                it.activeRequestsCounter.incrementAndGet()
                it.getHostInfo()
            }
    }

    override fun addResponseTimeData(target: HostPortPair, responseTime: Long, responseType: LoadBalancerResponseType) {
        overallActiveRequestCounter.decrementAndGet()
        if (responseType == LoadBalancerResponseType.OK) {
            targets.find { it.workflowDeploymentData.host == target.host && it.workflowDeploymentData.port == it.workflowDeploymentData.port }
                ?.also {
                    it.activeRequestsCounter.decrementAndGet()
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