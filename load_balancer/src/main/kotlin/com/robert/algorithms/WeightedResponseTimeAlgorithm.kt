package com.robert.algorithms

import com.robert.*
import com.robert.balancing.RequestData
import com.robert.balancing.RequestTargetData
import com.robert.enums.LBAlgorithms
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

private data class TargetWeightedResponseInfo(
    val target: RequestTargetData,
    var weight: Float
) {
    private var numberOfElements: Int = 0

    var averageResponseTime: Float = 0f
        private set

    @Synchronized
    fun addResponseTime(responseTime: Long) {
        // take into consideration for the average only the last Int.MAX_VALUE requests
        if (numberOfElements != Int.MAX_VALUE) {
            numberOfElements++
        }

        averageResponseTime = ((numberOfElements - 1f) / numberOfElements) * averageResponseTime +
                (1f / numberOfElements) * responseTime
    }
}

class WeightedResponseTimeAlgorithm(availableTargets: List<RequestTargetData>) : LoadBalancingAlgorithm {
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

    private val requests = ConcurrentHashMap<String, Pair<Long, TargetWeightedResponseInfo>>()
    private var targets: List<TargetWeightedResponseInfo> = emptyList()
    private val computedRequestsSinceLastReWeighting = AtomicInteger(0)
    private val targetsLock = ReentrantLock()

    init {
        updateTargets(availableTargets)
    }

    override fun updateTargets(newTargets: List<RequestTargetData>) {
        // reinitialize all the weights, on targets change
        targetsLock.withLock {
            targets = newTargets.map { TargetWeightedResponseInfo(it, 1f / newTargets.size) }
        }
    }

    override fun selectTargetDeployment(): RequestData {
        val referenceId = UUID.randomUUID().toString()
        val targetInfo = selectTarget()
        requests[referenceId] = Pair(Instant.now().epochSecond, targetInfo)

        return RequestData(referenceId, targetInfo.target)
    }

    override fun registerProcessingFinished(deploymentInfo: RequestData) {
        requests.remove(deploymentInfo.referenceId)?.let {
            val (requestTime, targetInfo) = it
            if (requestTime > 0) {
                val delta = Instant.now().epochSecond - requestTime
                targetInfo.addResponseTime(delta)
                computedRequestsSinceLastReWeighting.incrementAndGet()
                if (computedRequestsSinceLastReWeighting.compareAndSet(recomputeWeightInterval, 0)) {
                    recomputeWeights()
                }
            }
        }
    }

    private fun selectTarget(): TargetWeightedResponseInfo {
        var rand = kotlin.random.Random.nextFloat()
        for (target in targets) {
            val weight = target.weight
            if (rand < weight) {
                return target
            }
            rand -= weight
        }
        log.error("Unable to randomly select the target")
        return targets.first()
    }

    private fun recomputeWeights() {
        val avgResponseTimeMap = HashMap<TargetWeightedResponseInfo, Float>()

        targetsLock.withLock {
            val targetsRef = targets
            val sumComputeTime = targetsRef.sumOf {
                val averageResponseTime = it.averageResponseTime
                avgResponseTimeMap[it] = averageResponseTime
                it.averageResponseTime.toDouble()
            }
            targets = targetsRef.map {
                val weight = (avgResponseTimeMap[it]!! / sumComputeTime).toFloat()
                TargetWeightedResponseInfo(it.target, weight)
            }
        }
    }
}
