package com.robert.algorithms

import com.robert.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.concurrent.read
import kotlin.concurrent.write

private data class TargetWeightedResponseInfo(
    val target: PathTargetResource,
    var weight: Float
) {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    private var numberOfElements: Int = 0

    var averageResponseTime: Float = 0f
        get() = lock.read { averageResponseTime }
        private set

    fun addResponseTime(responseTime: Long) {
        lock.write {
            // tale into consideration for the average only the last Int.MAX_VALUE requests
            if (numberOfElements != Int.MAX_VALUE) {
                numberOfElements++
            }

            averageResponseTime = ((numberOfElements - 1f) / numberOfElements) * averageResponseTime +
                    (1 / numberOfElements) * responseTime
        }
    }
}

class WeightedResponseTimeAlgorithm(private var availableTargets: List<PathTargetResource>) : LoadBalancingAlgorithm {
    companion object {
        private val log = LoggerFactory.getLogger(WeightedResponseTimeAlgorithm::class.java)
    }

    override val algorithm = LBAlgorithms.WEIGHTED_RESPONSE_TIME
    private val lock = ReentrantReadWriteLock()
    private val requests = ConcurrentHashMap<String, Pair<Long, TargetWeightedResponseInfo>>()
    private var targets: List<TargetWeightedResponseInfo> = emptyList()
    private val computedRequestsSinceLastReWeighting = AtomicInteger(0)
    private val recomputeWeightInterval =
        DynamicConfigProperties.getIntProperty(Constants.COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL) ?: 10

    init {
        updateTargets(availableTargets)
    }

    override fun updateTargets(newTargets: List<PathTargetResource>) {
        // reinitialize all the weights, on targets change
        lock.write {
            targets = newTargets.map { TargetWeightedResponseInfo(it, 1f / availableTargets.size) }
        }
        availableTargets = newTargets
    }

    override fun selectTargetDeployment(): SelectedDeploymentInfo {
        val referenceId = UUID.randomUUID().toString()
        val targetInfo = selectTarget()
        requests[referenceId] = Pair(Instant.now().epochSecond, targetInfo)

        return SelectedDeploymentInfo(
            targetInfo.target.host,
            targetInfo.target.port,
            referenceId,
        )
    }

    override fun registerProcessingFinished(deploymentInfo: SelectedDeploymentInfo) {
        val (requestTime, targetInfo) = requests.remove(deploymentInfo.referenceId) ?: Pair(-1L, null)
        if (requestTime > 0) {
            val delta = Instant.now().epochSecond - requestTime
            targetInfo!!.addResponseTime(delta)
            if (computedRequestsSinceLastReWeighting.compareAndSet(recomputeWeightInterval, 0)) {
                recomputeWeights()
            }
        }
    }

    private fun selectTarget(): TargetWeightedResponseInfo {
        lock.read {
            var rand = kotlin.random.Random.nextFloat()
            for (target in targets) {
                val weight = target.averageResponseTime
                if (rand < weight) {
                    return target
                }
                rand -= weight
            }
            log.error("Unable to randomly select the target")
            return targets.first()
        }
    }

    private fun recomputeWeights() {
        lock.write {
            val avgResponseTimeMap = HashMap<TargetWeightedResponseInfo, Float>()
            val sumComputeTime = targets.sumOf {
                val averageResponseTime = it.averageResponseTime
                avgResponseTimeMap[it] = averageResponseTime
                it.averageResponseTime.toDouble()
            }
            targets.forEach {
                it.weight = (avgResponseTimeMap[it]!! / sumComputeTime).toFloat()
            }
        }
    }
}
