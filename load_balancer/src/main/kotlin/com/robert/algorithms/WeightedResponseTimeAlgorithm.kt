package com.robert.algorithms

import com.robert.*
import com.robert.balancing.ProcessedHeader
import com.robert.balancing.RequestData
import com.robert.balancing.TargetData
import com.robert.balancing.TargetWeightData
import com.robert.enums.LBAlgorithms
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class WeightedResponseTimeAlgorithm(availableTargets: List<TargetData>) : LoadBalancingAlgorithm {
    companion object {
        private val log = LoggerFactory.getLogger(WeightedResponseTimeAlgorithm::class.java)
        private var recomputeWeightInterval by Delegates.notNull<Int>()

        fun reloadDynamicConfigs() {
            log.debug("reload weighted response time algorithm dynamic configs")
            recomputeWeightInterval = DynamicConfigProperties.getIntPropertyOrDefault(
                Constants.COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL,
                Constants.DEFAULT_COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL
            )
        }
    }

    override val algorithm = LBAlgorithms.WEIGHTED_RESPONSE_TIME

    private val activeRequests = ConcurrentHashMap<String, Pair<Long, TargetWeightData>>()
    private var targets: List<TargetWeightData> = emptyList()
    private val computedRequestsSinceLastReWeighting = AtomicInteger(0)

    init {
        updateTargets(availableTargets)
    }

    @Synchronized
    override fun updateTargets(newTargets: List<TargetData>) {
        // reinitialize all the weights, on targets change
        targets = newTargets.map { TargetWeightData(it, 1.0 / newTargets.size) }
    }

    override fun selectTargetDeployment(header: ProcessedHeader): RequestData {
        val referenceId = UUID.randomUUID().toString()
        val targetInfo = TargetWeightData.selectRandomTarget(targets)
        activeRequests[referenceId] = Pair(Instant.now().epochSecond, targetInfo)

        return RequestData(referenceId, targetInfo.target)
    }

    override fun registerProcessingFinished(deploymentInfo: RequestData) {
        activeRequests.remove(deploymentInfo.referenceId)?.let {
            val (requestTime, targetInfo) = it
            if (requestTime > 0) {
                val delta = Instant.now().epochSecond - requestTime
                targetInfo.addResponseTime(delta)
                computedRequestsSinceLastReWeighting.incrementAndGet()
                if (computedRequestsSinceLastReWeighting.updateAndGet { value ->
                        if (value > recomputeWeightInterval) {
                            0
                        } else {
                            value
                        }
                    } == 0) {
                    recomputeWeights()
                }
            }
        }
    }


    @Synchronized
    private fun recomputeWeights() {
        val avgResponseTimeMap = HashMap<TargetWeightData, Double>()

        val targetsRef = targets
        val sumComputeTime = targetsRef.sumOf {
            val averageResponseTime = it.averageResponseTime
            avgResponseTimeMap[it] = averageResponseTime
            it.averageResponseTime
        }
        targets = targetsRef.map {
            val weight = (avgResponseTimeMap[it]!! / sumComputeTime)
            TargetWeightData(it.target, weight)
        }
    }
}
