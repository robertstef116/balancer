package com.robert.service

import com.google.protobuf.Empty
import com.robert.controller.ScalingController
import com.robert.controller.WorkerController
import com.robert.controller.WorkflowController
import com.robert.resources.DockerContainer
import com.robert.resources.DockerPortMapping
import com.robert.enums.LBAlgorithms
import com.robert.logger
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.scaling.client.model.WorkflowDeploymentData
import com.robert.scaling.grpc.*
import com.robert.enums.WorkerState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ScalingService(coroutineContext: CoroutineContext = EmptyCoroutineContext) : ScalingServiceGrpcKt.ScalingServiceCoroutineImplBase(coroutineContext), KoinComponent {
    companion object {
        val LOG by logger()
    }

    private val scalingController: ScalingController by inject()
    private val workerController: WorkerController by inject()
    private val workflowController: WorkflowController by inject()

    override suspend fun updateWorkerStatus(request: WorkerStatus): DeploymentRequestList {
        LOG.info("Received status update from worker {} ({})", request.id, request.alias)
        try {
            val id = UUID.fromString(request.id)
            val scalingRequests = scalingController.getScalingRequestForWorker(id)
            workerController.updateWorkerStatus(id, request.alias, request.host, request.cpuLoad, request.memoryLoad, request.availableMemory, request.deploymentsList
                .filter { deploymentStatus -> scalingRequests.find { it.containerId == deploymentStatus.containerId } == null }
                .map {
                    DockerContainer(
                        it.containerId,
                        UUID.fromString(it.workflowId),
                        it.cpuUsage,
                        it.memoryUsage,
                        it.portsMappingList.map { mapping -> DockerPortMapping(mapping.publicPort, mapping.privatePort) }
                    )
                })
            return newWorkflowScalingRequest(scalingRequests)

        } catch (e: Exception) {
            LOG.error("Unable to update worker status", e)
        }
        return DeploymentRequestList.getDefaultInstance()
    }

    override suspend fun getAvailableWorkflowDeploymentsData(request: Empty): WorkflowDeploymentsDataList {
        LOG.info("Requested available workflow deployment data")
        return newWorkflowDeploymentsDataList(scalingController.getAvailableWorkflowDeploymentsData())
    }

    override suspend fun updateWorker(request: WorkerData): OkData {
        LOG.info("Updating state of worker with id {} to {}", request.id, request.state)
        return buildOkData(scalingController.updateWorkerState(UUID.fromString(request.id), WorkerState.valueOf(request.state.toString())))
    }

    override suspend fun removeWorker(request: IdData): OkData {
        LOG.info("Removing worker with id {}", request.id)
        return buildOkData(workerController.removeWorker(UUID.fromString(request.id)))
    }

    override suspend fun addWorkflow(request: WorkflowData): OkData {
        LOG.trace("Received add workflow request: {}", request)
        return buildOkData(
            workflowController.addWorkflow(
                UUID.fromString(request.id),
                request.image,
                request.cpuLimit,
                request.memoryLimit,
                request.minDeployments,
                request.maxDeployments,
                LBAlgorithms.valueOf(request.algorithm.toString()),
                request.pathsMappingMap
            )
        )
    }

    override suspend fun updateWorkflow(request: WorkflowUpdateData): OkData {
        LOG.trace("Received update workflow request: {}", request.id)
        return buildOkData(workflowController.updateWorkflow(UUID.fromString(request.id), request.minDeployments, request.maxDeployments, LBAlgorithms.valueOf(request.algorithm.toString())))
    }

    override suspend fun removeWorkflow(request: IdData): OkData {
        LOG.trace("Received remove workflow request: {}", request.id)
        return buildOkData(workflowController.removeWorkflow(UUID.fromString(request.id)))
    }

    private fun newWorkflowScalingRequest(requests: List<DeploymentScalingRequest>?): DeploymentRequestList {
        if (requests.isNullOrEmpty()) {
            return DeploymentRequestList.getDefaultInstance()
        }

        val workflowScalingRequestsListBuilder = DeploymentRequestList.newBuilder()
        requests.forEach { request ->
            val deploymentRequestBuilder = DeploymentRequest.newBuilder()
                .setType(DeploymentRequestTypeGrpc.valueOf(request.type.toString()))
            request.containerId?.let { deploymentRequestBuilder.setId(it) }
            request.workflowId?.let { deploymentRequestBuilder.setWorkflowId(it.toString()) }
            request.image?.let { deploymentRequestBuilder.setImage(it) }
            request.cpuLimit?.let { deploymentRequestBuilder.setCpuLimit(it) }
            request.memoryLimit?.let { deploymentRequestBuilder.setMemoryLimit(it) }
            request.exposedPorts?.forEach { port -> deploymentRequestBuilder.addPorts(port) }
            workflowScalingRequestsListBuilder.addRequests(deploymentRequestBuilder.build())
        }
        return workflowScalingRequestsListBuilder.build()
    }

    private fun newWorkflowDeploymentsDataList(data: List<WorkflowDeploymentData>): WorkflowDeploymentsDataList {
        if (data.isEmpty()) {
            return WorkflowDeploymentsDataList.getDefaultInstance()
        }

        val workflowDeploymentsDataListBuilder = WorkflowDeploymentsDataList.newBuilder()
        data.forEach {
            workflowDeploymentsDataListBuilder.addRequests(
                WorkflowDeploymentsData.newBuilder()
                    .setWorkflowId(it.workflowId.toString())
                    .setPath(it.path)
                    .setHost(it.host)
                    .setPort(it.port)
                    .setAlgorithm(WorkflowAlgorithm.valueOf(it.algorithm.toString()))
                    .setScore(it.score)
                    .build()
            )
        }
        return workflowDeploymentsDataListBuilder.build()
    }

    private fun buildOkData(ok: Boolean): OkData {
        return OkData.newBuilder()
            .setOk(ok)
            .build()
    }
}
