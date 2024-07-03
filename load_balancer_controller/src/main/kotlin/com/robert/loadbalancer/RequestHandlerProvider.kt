package com.robert.loadbalancer

import com.robert.Constants
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.loadbalancer.algorithm.*
import com.robert.logger
import com.robert.scaling.client.ScalingClient
import com.robert.scaling.client.model.WorkflowDeploymentData
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

@Scheduler
class RequestHandlerProvider : KoinComponent {
    companion object {
        private val LOG by logger()
    }

    private val scalingClient by inject<ScalingClient>()

    private val assigner = ConcurrentHashMap<String, BalancingAlgorithm>()

    fun getAssigner(path: String): BalancingAlgorithm {
        return assigner[path] ?: throw NotFoundException()
    }

    @SchedulerConsumer(name = "ScalingManager", interval = "\${${Constants.HEALTH_CHECK_INTERVAL}:30s}") // TODO: wrong constant
    fun reloadWorkflowStatusData() {
        val mappedDeploymentData = mutableMapOf<String, MutableList<WorkflowDeploymentData>>()
        scalingClient.getAvailableWorkflowDeploymentsData().forEach {
            mappedDeploymentData.computeIfAbsent(it.path) { mutableListOf() }
                .add(it)
        }

        assigner.entries.removeIf {
            !mappedDeploymentData.containsKey(it.key) || mappedDeploymentData[it.key]?.first()?.algorithm != it.value.getAlgorithmType()
        }
        mappedDeploymentData.forEach { (path, deploymentDataList) ->
            assigner.computeIfAbsent(path) {
                val algorithm = deploymentDataList.first().algorithm
                LOG.debug("Initializing algorithm of type {} for path {}", algorithm, path)
                getAlgorithmAssigner(algorithm)
            }
                .updateData(deploymentDataList)
        }
        LOG.debug("{} active paths", assigner.size)
    }

    private fun getAlgorithmAssigner(algorithm: LBAlgorithms): BalancingAlgorithm {
        return when (algorithm) {
            LBAlgorithms.RANDOM -> RandomAssigner()
            LBAlgorithms.LEAST_CONNECTION -> LeastConnectionAssigner()
            LBAlgorithms.ROUND_ROBIN -> RoundRobinAssigner()
            LBAlgorithms.WEIGHTED_RESPONSE_TIME -> WeightedResponseTimeAssigner()
            LBAlgorithms.ADAPTIVE -> RandomAssigner()
        }
    }
}