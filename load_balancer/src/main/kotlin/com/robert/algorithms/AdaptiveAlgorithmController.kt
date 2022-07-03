package com.robert.algorithms

import com.robert.Constants
import com.robert.DynamicConfigProperties
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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.Delegates

class AdaptiveAlgorithmController(availableTargets: List<TargetData>) : LoadBalancingAlgorithm {
    companion object {
        private val log = LoggerFactory.getLogger(AdaptiveAlgorithmController::class.java)
        private var recomputeWeightInterval by Delegates.notNull<Int>()

        fun reloadDynamicConfigs() {
            log.debug("reload weighted response time algorithm dynamic configs")
            recomputeWeightInterval = DynamicConfigProperties.getIntPropertyOrDefault(
                Constants.COMPUTE_ADAPTIVE_WEIGHTED_RESPONSE_TIME_INTERVAL,
                Constants.DEFAULT_COMPUTE_ADAPTIVE_WEIGHTED_RESPONSE_TIME_INTERVAL
            )
            AdaptiveAlgorithm.recomputeWeightInterval = recomputeWeightInterval
        }
    }

    override val algorithm = LBAlgorithms.ADAPTIVE

    private lateinit var targets: List<TargetData>
    private var targetsMapping = AdaptiveAlgorithmMapping()
    private val mtx = ReentrantReadWriteLock()

    init {
        updateTargets(availableTargets)
    }

    override fun updateTargets(newTargets: List<TargetData>) {
        mtx.write {
            targetsMapping.getAll().map { it.updateTargets(newTargets) }
            targets = newTargets
        }
    }

    override fun selectTargetDeployment(header: ProcessedHeader): RequestData {
        val adaptiveAlgorithm: AdaptiveAlgorithm
        mtx.read {
            adaptiveAlgorithm = targetsMapping.getOrCreate(header.method, header.route, targets)
        }
        return adaptiveAlgorithm.selectTargetDeployment(header)
    }

    override fun registerProcessingFinished(deploymentInfo: RequestData) {
    }
}

private class AdaptiveAlgorithm(availableTargets: List<TargetData>) : LoadBalancingAlgorithm {
    companion object {
        var recomputeWeightInterval by Delegates.notNull<Int>()
    }

    override val algorithm = LBAlgorithms.ADAPTIVE
    private var targets: List<TargetWeightData> = emptyList()
    private val activeRequests = ConcurrentHashMap<String, Pair<Long, TargetWeightData>>()
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

    @Synchronized
    private fun recomputeWeights() {
        val targetData = HashMap<TargetWeightData, Pair<Int, Double>>()
        val targetsRef = targets

        targetsRef.forEach {
            targetData[it] = it.reset()
        }

        val sumComputeTime = targetData.values.sumOf { it.second }
        val avgComputeTime = sumComputeTime / targetsRef.size
        val requestsCount = targetData.values.sumOf { it.first }

        // compute weights
        val minWeight = (1 / targetsRef.size) * 0.05
        val intermediateTargetsRef = targetsRef.map {
            val (targetRequestsCount, targetAvgComputeTime) = targetData[it] ?: Pair(0, 0.0)
            val computedWeight = (targetAvgComputeTime - avgComputeTime) * (targetRequestsCount / requestsCount) * 0.1

            val weight = computedWeight.coerceAtLeast(minWeight)
            TargetWeightData(it.target, weight)
        }

        // normalize
        val sumWeight = intermediateTargetsRef.sumOf { it.weight }
        intermediateTargetsRef.forEach {
            it.weight = it.weight/sumWeight
        }
        targets = intermediateTargetsRef
    }
}

private class AdaptiveAlgorithmMapping {
    private val mapping = mutableMapOf<String, MutableMap<String, AdaptiveAlgorithm>>() // method - path - algorithm
    private val mtx = ReentrantReadWriteLock()

    fun getOrCreate(method: String, path: String, availableTargets: List<TargetData>): AdaptiveAlgorithm {
        mtx.read {
            return mapping[method]?.get(path) ?: put(method, path, AdaptiveAlgorithm(availableTargets))
        }
    }

    fun getAll(): List<AdaptiveAlgorithm> {
        val algorithmsList = mutableListOf<AdaptiveAlgorithm>()
        mtx.read {
            for (pathsPerMethod in mapping.values) {
                for (algorithms in pathsPerMethod.values) {
                    algorithmsList.addAll(listOf(algorithms))
                }
            }
        }
        return algorithmsList
    }

    fun get(method: String, path: String): AdaptiveAlgorithm? {
        mtx.read {
            return mapping[method]?.get(path)
        }
    }

    fun put(method: String, path: String, adaptiveAlgorithm: AdaptiveAlgorithm): AdaptiveAlgorithm {
        mtx.read {
            var methodMapping = mapping[method]
            var persistMethodMapping = false
            if (methodMapping == null) {
                methodMapping = mutableMapOf()
                persistMethodMapping = true
            }
            mtx.write {
                methodMapping[path] = adaptiveAlgorithm
                if (persistMethodMapping) {
                    mapping[method] = methodMapping
                }
                return adaptiveAlgorithm
            }
        }
    }
}
