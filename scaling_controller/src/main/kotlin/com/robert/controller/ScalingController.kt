package com.robert.controller

import com.robert.Env
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.logger
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.scaling.client.model.WorkflowDeploymentData
import com.robert.model.InternalWorkerStatus
import com.robert.analytics.ScalingAnalytic
import com.robert.enums.WorkerState
import com.robert.resources.Workflow
import com.robert.storage.repository.ScalingAnalyticRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*
import kotlin.math.abs

@Scheduler
class ScalingController : KoinComponent {
    companion object {
        val LOG by logger()

        private val CPU_WEIGHT = Env.getDouble("CPU_WEIGHT", 0.3)
        private val MEMORY_WEIGHT = Env.getDouble("CPU_WEIGHT", 0.7)
        private val SCALE_DOWN_THRESHOLD = Env.getDouble("SCALE_DOWN_THRESHOLD", 0.3)
        private val SCALE_UP_THRESHOLD = Env.getDouble("SCALE_UP_THRESHOLD", 0.8)
        private val SCORE_BREACH_COUNT = Env.getInt("SCORE_BREACH_COUNT", 3)

        private val rnd = Random()

        data class WorkflowMetadata(
            val workerId: UUID,
            val containerId: String,
            val score: Double,
            val cpuUsage: Double,
            val memoryUsage: Double,
        )
    }

    private val workerController: WorkerController by inject()
    private val workflowController: WorkflowController by inject()
    private val scalingAnalyticRepository: ScalingAnalyticRepository by inject()

    private val workflowScalingThresholdBreachCounts = mutableMapOf<UUID, Int>()

    private val scalingRequestsQueue = mutableMapOf<UUID, MutableList<DeploymentScalingRequest>>()

    @Synchronized
    fun getScalingRequestForWorker(workerId: UUID): List<DeploymentScalingRequest> {
        val registeredScalingRequests = mutableListOf<DeploymentScalingRequest>()
        val unregisteredScalingRequests = mutableListOf<DeploymentScalingRequest>()
        scalingRequestsQueue[workerId]?.forEach {
            if (it.registered) {
                registeredScalingRequests.add(it)
            } else {
                unregisteredScalingRequests.add(it)
            }
        }
        if (unregisteredScalingRequests.isEmpty()) {
            scalingRequestsQueue.remove(workerId)
        } else {
            scalingRequestsQueue[workerId] = unregisteredScalingRequests
        }
        LOG.debug("{} scaling requests pending and {} unregistered requests for worker {}", registeredScalingRequests.size, unregisteredScalingRequests.size, workerId)
        return registeredScalingRequests
    }

    @Synchronized
    fun updateWorkerState(id: UUID, state: WorkerState): Boolean {
        if (state == WorkerState.DISABLED || state == WorkerState.OFFLINE) {
            val scalingRequests = scalingRequestsQueue.remove(id)
            LOG.debug("Removing {} scaling requests pending for worker {} since worker changed state to {}", scalingRequests?.size ?: 0, id, state.toString())
        }
        return workerController.updateWorkerState(id, state)
    }

    @Synchronized
    fun getAvailableWorkflowDeploymentsData(): List<WorkflowDeploymentData> {
        val workflowCache = mutableMapOf<UUID, Workflow?>()
        val data = mutableListOf<WorkflowDeploymentData>()
        workerController.getOnlineWorkersStatus().forEach { workerData ->
            workerData.activeDeployments
                .filter { deploymentData ->
                    scalingRequestsQueue[workerData.id]?.find {
                        it.containerId == deploymentData.containerID && it.type == DeploymentScalingRequest.Type.DOWN
                    }?.also {
                        it.registered = true // marking the request as registered meaning it can be processed by the worker and will no longer be used by the load balancer
                    } == null
                }
                .forEach { deploymentData ->
                    workflowCache.computeIfAbsent(deploymentData.workflowId) {
                        workflowController.getWorkflow(deploymentData.workflowId)
                    }?.let { workflowData ->
                        workflowData.pathsMapping.forEach { (path, privatePort) ->
                            deploymentData.ports
                                .find { it.privatePort == privatePort }
                                ?.let { (publicPort, _) ->
                                    data.add(
                                        WorkflowDeploymentData(
                                            workflowData.id,
                                            path,
                                            workerData.host,
                                            publicPort,
                                            workflowData.algorithm,
                                            computeScore(deploymentData.cpuUsage, deploymentData.memoryUsage)
                                        )
                                    )
                                }
                                ?: LOG.warn("Port mapping of {} not found for workflow {} on worker {}", privatePort, workflowData.id, workerData.host)
                        }
                    } ?: LOG.warn("Workflow {} not found", deploymentData.workflowId)
                }
        }
        return data
    }

    @Synchronized
    @SchedulerConsumer(name = "ScalingManager", interval = "\${RESCALING_INTERVAL_SECONDS:30s}")
    fun scale() {
        workerController.pruneWorkersByAge()

        val workflowDeploymentsMetadata = mutableMapOf<UUID, MutableList<WorkflowMetadata>>()
        val onlineWorkersStatus = workerController.getOnlineWorkersStatus()
        onlineWorkersStatus.forEach {
            val workerScore = computeScore(it.cpuLoad, it.memoryLoad)
            it.activeDeployments.forEach { deployment ->
                workflowDeploymentsMetadata.getOrPut(deployment.workflowId) { mutableListOf() }
                    .add(
                        WorkflowMetadata(
                            it.id,
                            deployment.containerID,
                            0.2 * workerScore + 0.8 * computeScore(deployment.cpuUsage, deployment.memoryUsage),
                            deployment.cpuUsage,
                            deployment.memoryUsage
                        )
                    )
            }
        }

        workflowController.getWorkflows().forEach { workflow ->
            val metadata = workflowDeploymentsMetadata.remove(workflow.id)?.let { workflowMetadata ->
                saveWorkflowScalingAnalyticsData(workflow.id, workflowMetadata)
                val averageScore = workflowMetadata
                    .map { it.score }
                    .average()
                val containersCount = workflowMetadata.size

                val min = workflow.minDeployments ?: 1
                val max = workflow.maxDeployments ?: Int.MAX_VALUE
                if ((averageScore < SCALE_DOWN_THRESHOLD && containersCount - 1 in min..max) || containersCount > max) {
                    scaleDown(workflow.id, onlineWorkersStatus)
                } else if ((averageScore > SCALE_UP_THRESHOLD && containersCount + 1 in min..max) || containersCount < min) {
                    scaleUp(workflow, onlineWorkersStatus)
                } else if (workflowScalingThresholdBreachCounts.remove(workflow.id) != null) {
                    LOG.info("Cleaning workflow breaches count")
                }
            }
            if (metadata == null) {
                scaleUp(workflow, onlineWorkersStatus, true)
            }
        }

        workflowDeploymentsMetadata.forEach { workflowMetadata ->
            workflowMetadata.value.forEach {
                LOG.debug("Discovered dandling container on worker {}, removing it", it.workerId)
                scaleDownDeployment(it.workerId, workflowMetadata.key, it.containerId)
            }
        }
    }

    private fun scaleUp(workflow: Workflow, onlineWorkersStatus: List<InternalWorkerStatus>, force: Boolean = false) {
        val thresholdBreachCount = workflowScalingThresholdBreachCounts.getOrDefault(workflow.id, 0)
        workflowScalingThresholdBreachCounts[workflow.id] = thresholdBreachCount + 1
        var earlyScaleUp = false
        if (force && thresholdBreachCount == 0) {
            LOG.info("Early scaling up required for workflow {}", workflow.id)
            earlyScaleUp = true
        } else if (thresholdBreachCount <= SCORE_BREACH_COUNT) {
            LOG.info("Scaling up workflow {}, but threshold is {}", workflow.id, thresholdBreachCount)
            return
        }

        val existingScalingRequest = getScalingRequestsForWorkflow(workflow.id)
        if (existingScalingRequest != null) {
            if (existingScalingRequest.type == DeploymentScalingRequest.Type.DOWN) {
                removeScalingRequestForWorkflow(workflow.id)
                workflowScalingThresholdBreachCounts.remove(workflow.id)
            } else {
                LOG.info("Scaling up request pending for workflow {}", workflow.id)
                return
            }
        }

        val availableWorkerStatuses = onlineWorkersStatus.filter { it.availableMemory > workflow.memoryLimit }
        val weightSum = availableWorkerStatuses.sumOf { 5 - computeScore(it.cpuLoad, it.memoryLoad) }
        var rndWeight = rnd.nextDouble() * weightSum
        availableWorkerStatuses.forEach {
            val workerWeight = 5 - computeScore(it.cpuLoad, it.memoryLoad)
            if (rndWeight < workerWeight) {
                scaleUpDeployment(it.id, workflow)
                if (!earlyScaleUp) {
                    workflowScalingThresholdBreachCounts.remove(workflow.id)
                }
                return
            }
            rndWeight -= workerWeight
        }
        LOG.warn("Unable to find worker to scale up workflow {}, {} workers available", workflow.id, availableWorkerStatuses.size)
        workflowScalingThresholdBreachCounts[workflow.id] = thresholdBreachCount
    }

    // Scaling down the workflow with smaller score (smaller load)
    private fun scaleDown(workflowId: UUID, onlineWorkersStatus: List<InternalWorkerStatus>) {
        val thresholdBreachCount = workflowScalingThresholdBreachCounts.getOrDefault(workflowId, 0) - 1
        if (abs(thresholdBreachCount) <= SCORE_BREACH_COUNT) {
            LOG.info("Scaling down workflow {}, but threshold is {}", workflowId, thresholdBreachCount)
            workflowScalingThresholdBreachCounts[workflowId] = thresholdBreachCount
            return
        }

        val existingScalingRequest = getScalingRequestsForWorkflow(workflowId)
        if (existingScalingRequest != null) {
            if (existingScalingRequest.type == DeploymentScalingRequest.Type.UP) {
                removeScalingRequestForWorkflow(workflowId)
                workflowScalingThresholdBreachCounts.remove(workflowId)
            } else {
                LOG.info("Scaling down request pending for workflow {}", workflowId)
                return
            }
        }

        val availableWorkerStatuses = onlineWorkersStatus.filter {
            it.activeDeployments.firstOrNull { activeDeployment ->
                activeDeployment.workflowId == workflowId
            } != null
        }
        val weightSum = availableWorkerStatuses.sumOf { 5 - computeScore(it.cpuLoad, it.memoryLoad) }
        var rndWeight = rnd.nextDouble() * weightSum
        availableWorkerStatuses.forEach {
            val workerWeight = 5 - computeScore(it.cpuLoad, it.memoryLoad)
            if (rndWeight < workerWeight) {
                scaleDownDeployment(it.id, workflowId, it.activeDeployments.first { activeDeployment ->
                    activeDeployment.workflowId == workflowId
                }.containerID)
                workflowScalingThresholdBreachCounts.remove(workflowId)
                return
            }
            rndWeight -= workerWeight
        }
        LOG.warn("Unable to find worker to scale down workflow {}", workflowId)
    }

    private fun scaleUpDeployment(workerId: UUID, workflow: Workflow) {
        LOG.info("Scaling up workflow {} on worker {}", workflow.id, workerId)
        scalingRequestsQueue.getOrPut(workerId) { mutableListOf() }
            .add(
                DeploymentScalingRequest(
                    null,
                    workflow.id,
                    workflow.image,
                    workflow.pathsMapping.map { it.value },
                    workflow.cpuLimit,
                    workflow.memoryLimit,
                    DeploymentScalingRequest.Type.UP,
                    true
                )
            )
    }

    private fun scaleDownDeployment(workerId: UUID, workflowId: UUID, containerId: String) {
        val workerQueue = scalingRequestsQueue.getOrPut(workerId) { mutableListOf() }
        if (workerQueue.find { it.containerId == containerId } == null) {
            LOG.info("Register scaling deployment down request on worker {}", workerId)
            workerQueue.add(DeploymentScalingRequest(containerId, workflowId, null, null, null, null, DeploymentScalingRequest.Type.DOWN, false))
        }
    }

    private fun getScalingRequestsForWorkflow(workflowId: UUID): DeploymentScalingRequest? {
        scalingRequestsQueue.values.forEach { workerRequests ->
            workerRequests.forEach { request ->
                if (request.workflowId == workflowId) {
                    return request
                }
            }
        }
        return null
    }

    private fun removeScalingRequestForWorkflow(workflowId: UUID) {
        LOG.info("Removing scaling requests for workflow {} from queue", workflowId)
        scalingRequestsQueue.values.forEach { workerRequests ->
            workerRequests.removeIf { request ->
                request.workflowId == workflowId
            }
        }
    }

    private fun saveWorkflowScalingAnalyticsData(workflowId: UUID, data: List<WorkflowMetadata>) {
        try {
            scalingAnalyticRepository.create(
                ScalingAnalytic(
                    workflowId,
                    data.size,
                    data.sumOf { it.cpuUsage } / data.size,
                    data.sumOf { it.memoryUsage } / data.size,
                    Instant.now().toEpochMilli()
                )
            )
        } catch (e: Exception) {
            LOG.warn("Unable to save scaling analytics for workflow {}: {}", workflowId, e.message)
        }
    }

    private fun computeScore(cpuUsage: Double, memoryUsage: Double): Double {
        return cpuUsage * CPU_WEIGHT + memoryUsage * MEMORY_WEIGHT
    }
}