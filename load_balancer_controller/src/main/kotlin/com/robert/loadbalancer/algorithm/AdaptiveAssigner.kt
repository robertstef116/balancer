package com.robert.loadbalancer.algorithm

import com.robert.balancing.LoadBalancerResponseType
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.model.BalancingAlgorithmData
import com.robert.loadbalancer.model.HostPortPair
import com.robert.scaling.client.model.WorkflowDeploymentData
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

// Z-Score Standardization for avg response time
// https://www.aampe.com/blog/how-to-normalize-data-in-excel
// compute the mean and standard deviation in one loop using Welford's algorithm
class AdaptiveAssigner : BalancingAlgorithm {
    companion object {
        private const val RESPONSE_TIMES_USED_COUNT = 1000
    }

    @Volatile
    private var targets = listOf<AdaptiveWorkflowDeploymentData>()
    private var overallAverageResponseTime = 0.0
    private var overallDeviationResponseTime = 0.0
    private val overallActiveRequestCounter = AtomicLong(0)

    override fun getAlgorithmType(): LBAlgorithms {
        return LBAlgorithms.ADAPTIVE
    }

    override fun updateData(data: List<WorkflowDeploymentData>) {
        val newTargets = mutableListOf<AdaptiveWorkflowDeploymentData>()

        var validTargetsForLearningAverage = 0.0
        var validTargetsForLearningCount = 0
        var m2 = 0.0
        data.forEach { workflowDeploymentData ->
            val target = targets.find { it.workflowDeploymentData == workflowDeploymentData }
                ?: AdaptiveWorkflowDeploymentData()
            target.workflowDeploymentData = workflowDeploymentData
            if (target.count > 100) {
                validTargetsForLearningCount++
                val average = target.average
                val delta = average - validTargetsForLearningAverage
                validTargetsForLearningAverage += delta / validTargetsForLearningCount
                val delta2 = average - validTargetsForLearningAverage
                m2 = delta * delta2
            }
            newTargets.add(target)
        }
        overallDeviationResponseTime = sqrt(if (validTargetsForLearningCount > 0) m2 / (validTargetsForLearningCount - 1) else 0.0)
        overallAverageResponseTime = validTargetsForLearningAverage
        targets = newTargets
    }

    override fun getTarget(blacklistedTargets: Set<HostPortPair>): HostPortPair {
        return (BalancingAlgorithm.getAvailableTargets(targets, blacklistedTargets)
            .minByOrNull { (it.average - overallAverageResponseTime) / overallDeviationResponseTime + it.activeRequestsCounter.get() / overallActiveRequestCounter.get() }
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

    private inner class AdaptiveWorkflowDeploymentData: BalancingAlgorithmData() {
        @Volatile
        lateinit var workflowDeploymentData: WorkflowDeploymentData
        val activeRequestsCounter = AtomicLong()
        var average = 0.0
        var count = 0

        @Synchronized
        fun addResponseTime(responseTime: Long) {
            average = (average * count + responseTime) / (count + 1)
            count = (count + 1) % RESPONSE_TIMES_USED_COUNT
        }

        override fun getWorkflowDeploymentData(): WorkflowDeploymentData {
            return workflowDeploymentData
        }
    }
}