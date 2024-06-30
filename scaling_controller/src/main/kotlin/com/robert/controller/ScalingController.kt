package com.robert.controller

import com.robert.Constants
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.docker.DockerContainer
import com.robert.logger
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.scaling.client.model.WorkflowDeploymentData
import com.robert.scaller.InternalWorkerStatus
import com.robert.scaller.Worker
import com.robert.scaller.WorkerState
import com.robert.scaller.Workflow
import com.robert.storage.repository.WorkerRepository
import com.robert.storage.repository.WorkflowRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*
import kotlin.math.abs

@Scheduler
class ScalingController : KoinComponent {
    companion object {
        val LOG by logger()

        const val SCALING_EXCEEDS_COUNT = 3// TODO: parametrize this
        const val WORKER_STATUS_MAX_AGE_SECONDS = 120;

        private val rnd = Random()

        data class WorkflowMetadata(
            val workerId: UUID,
            val containerId: String,
            val score: Double,
        )
    }

    private val workerRepository: WorkerRepository by inject()
    private val workflowRepository: WorkflowRepository by inject()

    private val workflows = workflowRepository.getAll()

    private val workersStatus = mutableMapOf<UUID, InternalWorkerStatus>()
    private val workersStates = mutableMapOf<UUID, WorkerState>()

    private val workflowScalingThresholdBreachCounts = mutableMapOf<UUID, Int>()

    private val scalingRequestsQueue = mutableMapOf<UUID, MutableList<DeploymentScalingRequest>>()

    init {
        workerRepository.getAll()
            .forEach {
                initWorkerState(it.id, it.state)
            }

        LOG.info("Loaded {} workflow(s)", workflows.size)
    }

    @Synchronized
    fun updateWorkerStatus(id: UUID, alias: String, host: String, cpuLoad: Double, memoryLoad: Double, availableMemory: Long, activeDeployments: List<DockerContainer>) {
        when (workersStates[id]) {
            null -> {
                LOG.info("Discovered new worker {}", id)
                workerRepository.create(Worker(id, alias.take(50), WorkerState.ONLINE))
                workersStates[id] = WorkerState.ONLINE
            }

            WorkerState.OFFLINE -> {
                LOG.info("Updating status of worker {} to ONLINE", id)
                workerRepository.update(id, alias.take(50), WorkerState.ONLINE)
                workersStates[id] = WorkerState.ONLINE
            }

            WorkerState.DISABLED -> {
                LOG.info("Worker {} is disable, ignoring the status..", id)
                return
            }

            WorkerState.ONLINE -> {}
        }

        workersStatus[id] = InternalWorkerStatus(id, host, cpuLoad, memoryLoad, availableMemory, now(), activeDeployments)
    }

    @Synchronized
    fun getScalingRequestForWorker(workerId: UUID): List<DeploymentScalingRequest>? {
        val scalingRequests = scalingRequestsQueue.remove(workerId)
        LOG.info("{} scaling requests pending for worker {}", scalingRequests?.size ?: 0, workerId)
        return scalingRequests
    }

    @Synchronized
    fun updateWorkerState(id: UUID, state: WorkerState) {
        initWorkerState(id, state)
        if (state == WorkerState.DISABLED) {
            workersStatus.remove(id)
        }
        if (state == WorkerState.DISABLED || state == WorkerState.OFFLINE) {
            val scalingRequests = scalingRequestsQueue.remove(id)
            LOG.info("Removing {} scaling requests pending for worker {} since worker changed state to {}", scalingRequests?.size ?: 0, id, state.toString())
        }
    }

    @Synchronized
    fun getAvailableWorkflowDeploymentsData(): List<WorkflowDeploymentData> {
        val workflowCache = mutableMapOf<UUID, Workflow?>()
        val data = mutableListOf<WorkflowDeploymentData>()
        workersStatus.values.forEach { workerData ->
            workerData.activeDeployments.forEach { deploymentData ->
                workflowCache.computeIfAbsent(deploymentData.workflowId) {
                    workflows.find { it.id == deploymentData.workflowId }
                }?.let { workflowData ->
                    workflowData.pathsMapping.forEach { (path, privatePort) ->
                        deploymentData.ports
                            .find { it.privatePort == privatePort }
                            ?.let { (publicPort, _) -> data.add(WorkflowDeploymentData(path, workerData.host, publicPort, workflowData.algorithm, computeScore(deploymentData.cpuUsage, deploymentData.memoryUsage))) }
                            ?: LOG.warn("Port mapping of {} not found for workflow {} on worker {}", privatePort, workflowData.id, workerData.host)
                    }
                } ?: LOG.warn("Workflow {} not found", deploymentData.workflowId)
            }
        }
        return data
    }

    @Synchronized
    fun removeWorker(id: UUID) {
        workerRepository.delete(id)
        workersStatus.remove(id)
        workersStates.remove(id)
    }

    @Synchronized
    @SchedulerConsumer(name = "ScalingManager", interval = "\${${Constants.HEALTH_CHECK_INTERVAL}:30s}") // TODO: Wrong constant
    fun scale() {
        checkWorkersStatusAge()

        val workflowDeploymentsMetadata = mutableMapOf<UUID, MutableList<WorkflowMetadata>>()
        workersStatus.values.forEach {
            val workerScore = computeScore(it.cpuLoad, it.memoryLoad)
            it.activeDeployments.forEach { deployment ->
                workflowDeploymentsMetadata.getOrPut(deployment.workflowId) { mutableListOf() }
                    .add(WorkflowMetadata(it.id, deployment.containerID, 0.2 * workerScore + 0.8 * computeScore(deployment.cpuUsage, deployment.memoryUsage)))
            }
        }

        workflows.forEach { workflow ->
            val metadata = workflowDeploymentsMetadata.remove(workflow.id)?.let { workflowMetadata ->
                val averageScore = workflowMetadata
                    .map { it.score }
                    .average()
                val containersCount = workflowMetadata.size

                val min = workflow.minDeployments ?: 1
                val max = workflow.maxDeployments ?: Int.MAX_VALUE
                if ((averageScore < 0.3 && containersCount - 1 in min..max) || containersCount > max) {// TODO: parametrize this
                    scaleDown(workflow.id)
                } else if ((averageScore > 0.8 && containersCount + 1 in min..max) || containersCount < min) {// TODO: parametrize this
                    scaleUp(workflow)
                } else if (workflowScalingThresholdBreachCounts.remove(workflow.id) != null) {
                    LOG.info("Cleaning workflow breaches count")
                }
            }
            if (metadata == null) {
                scaleUp(workflow, true)
            }
        }

        workflowDeploymentsMetadata.forEach { workflowMetadata ->
            workflowMetadata.value.forEach {
                LOG.info("Discovered dandling container on worker {}, removing it", it.workerId)
                scaleDownDeployment(it.workerId, it.containerId)
            }
        }
    }

    private fun initWorkerState(id: UUID, state: WorkerState) {
        if (state != WorkerState.DISABLED) {
            LOG.info("Initializing state of worker {}", id)
            workersStates[id] = WorkerState.OFFLINE
            workerRepository.update(id, null, WorkerState.OFFLINE)
            workersStatus[id] = InternalWorkerStatus(id, "", -1.0, -1.0, -1, now(), listOf())
        } else {
            workersStates[id] = WorkerState.DISABLED
        }
    }

    private fun checkWorkersStatusAge() {
        val now = now()
        workersStatus.values.filter { now - it.lastUpdate > WORKER_STATUS_MAX_AGE_SECONDS }
            .forEach {
                workersStatus.remove(it.id)
                workerRepository.update(it.id, null, WorkerState.OFFLINE)
                LOG.info("Updated status of worker {} to OFFLINE", it.id)
            }
    }

    private fun scaleUp(workflow: Workflow, force: Boolean = false) {
        val thresholdBreachCount = workflowScalingThresholdBreachCounts.getOrDefault(workflow.id, 0)
        workflowScalingThresholdBreachCounts[workflow.id] = thresholdBreachCount + 1
        var earlyScaleUp = false
        if (force && thresholdBreachCount == 0) {
            LOG.info("Early scaling up required for workflow {}", workflow.id)
            earlyScaleUp = true
        } else if (thresholdBreachCount <= SCALING_EXCEEDS_COUNT) {
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

        val availableWorkerStatuses = workersStatus.values.filter { it.availableMemory > workflow.memoryLimit }
        var rndWeight = rnd.nextDouble() * availableWorkerStatuses.size
        availableWorkerStatuses.forEach {
            val workerWeight = computeScore(it.cpuLoad, it.memoryLoad)
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

    // Scaling down the workflow with higher score (smaller load)
    private fun scaleDown(workflowId: UUID) {
        val thresholdBreachCount = workflowScalingThresholdBreachCounts.getOrDefault(workflowId, 0) - 1
        if (abs(thresholdBreachCount) <= SCALING_EXCEEDS_COUNT) {
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

        val availableWorkerStatuses = workersStatus.values.filter {
            it.activeDeployments.firstOrNull { activeDeployment ->
                activeDeployment.workflowId == workflowId
            } != null
        }
        var rndWeight = rnd.nextDouble() * availableWorkerStatuses.size
        availableWorkerStatuses.forEach {
            val workerWeight = computeScore(it.cpuLoad, it.memoryLoad)
            if (rndWeight < workerWeight) {
                scaleDownDeployment(it.id, it.activeDeployments.first { activeDeployment ->
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
                    DeploymentScalingRequest.Type.UP
                )
            )
    }

    private fun scaleDownDeployment(workerId: UUID, containerId: String) {
        LOG.info("Scaling down worker {}", workerId)
        scalingRequestsQueue.getOrPut(workerId) { mutableListOf() }
            .add(DeploymentScalingRequest(containerId, null, null, null, null, null, DeploymentScalingRequest.Type.DOWN))
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

    private fun now(): Long {
        return Instant.now().epochSecond
    }

    private fun computeScore(cpuUsage: Double, memoryUsage: Double): Double {
        return cpuUsage * 0.3 + memoryUsage * 0.6// TODO: parametrize this
    }
}