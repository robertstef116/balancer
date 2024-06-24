package com.robert.service

import com.google.protobuf.Empty
import com.robert.controller.ScalingController
import com.robert.docker.DockerContainer
import com.robert.docker.DockerPortMapping
import com.robert.logger
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.scaling.grpc.*
import com.robert.scaller.WorkerState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

//https://github.com/grpc/grpc-kotlin/blob/master/examples/server/src/main/kotlin/io/grpc/examples/routeguide/RouteGuideServer.kt
class ScalingService(coroutineContext: CoroutineContext = EmptyCoroutineContext) :
    ScalingServiceGrpcKt.ScalingServiceCoroutineImplBase(coroutineContext), KoinComponent {
    companion object {
        val LOG by logger()
    }

    private val scalingController: ScalingController by inject()

//    override fun registerWorker(requests: Flow<WorkerStatus>): Flow<DeploymentRequest> {
//        var id: UUID? = null
//        var terminate = false
//        val deploymentScalingRequestFlow = flow<DeploymentRequest> {
//            while (!terminate) {
//                delay(scalingRequestsProcessingIntervalMillis)
//                id?.let {
//
//                }
//            }
//        }
//
//        CoroutineScope(Dispatchers.IO).launch {
//            requests
//                .conflate()
//                .collect { workerStatus ->
//                    if (id == null) {
//                        UUID.fromString(workerStatus.id).let {
//                            id = it
//                            scalingController.registerWorker(it)
//                        }
//                    }
//                    id?.let { id ->
//                        scalingController.updateWorkerStatus(id, workerStatus.cpuLoad, workerStatus.memoryLoad, workerStatus.availableMemory, workerStatus.deploymentsList.map {
//                            DockerContainer(
//                                it.workflowId,
//                                it.cpuUsage,
//                                it.memoryUsage,
//                                it.portsMappingList.map { mapping -> DockerPortMapping(mapping.publicPort, mapping.privatePort) }
//                            )
//                        })
//                    }
//                }
//            terminate = true
//            id?.let { scalingController.unregisterWorker(it) }
//        }
//
//        return deploymentScalingRequestFlow
//    }

    override suspend fun updateWorkerStatus(request: WorkerStatus): DeploymentRequestList {
        LOG.info("Received status update from worker {} ({})", request.id, request.alias)
        try {
            val id = UUID.fromString(request.id)
            scalingController.updateWorkerStatus(id, request.alias, request.cpuLoad, request.memoryLoad, request.availableMemory, request.deploymentsList.map {
                DockerContainer(
                    it.containerId,
                    UUID.fromString(it.workflowId),
                    it.cpuUsage,
                    it.memoryUsage,
                    it.portsMappingList.map { mapping -> DockerPortMapping(mapping.publicPort, mapping.privatePort) }
                )
            })
            return newWorkflowScalingRequest(scalingController.getScalingRequestForWorker(id))

        } catch (e: Exception) {
            LOG.error("Unable to update worker status", e)
        }
        return DeploymentRequestList.getDefaultInstance()
    }

    override suspend fun updateWorker(request: WorkerData): Empty {
        LOG.info("got updateWorker $request")
        scalingController.updateWorkerState(UUID.fromString(request.id), WorkerState.valueOf(request.state.toString()))
        return Empty.getDefaultInstance()
    }

    override suspend fun removeWorker(request: IdData): Empty {
        LOG.info("got removeWorker $request")
        scalingController.removeWorker(UUID.fromString(request.id))
        return Empty.getDefaultInstance()
    }

    override suspend fun addWorkflow(request: WorkflowData): Empty {
        LOG.info("got addWorkflow $request")
        return Empty.getDefaultInstance()
    }

    override suspend fun updateWorkflow(request: WorkflowUpdateData): Empty {
        LOG.info("got updateWorkflow $request")
        return Empty.getDefaultInstance()
    }

    override suspend fun removeWorkflow(request: IdData): Empty {
        LOG.info("got removeWorkflow $request")
        return Empty.getDefaultInstance()
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

    //    override suspend fun connectWorkerStatusController(requests: Flow<WorkerStatus>): Empty {
//        flow<WorkerStatus> {
//            requests.collect { ws ->
//                val workerStatus = InternalWorkerStatus(UUID.fromString(ws.id), ws.cpuLoad, ws.availableMemory)
//                val deploymentsStatus = ws.deploymentsList.map { ds ->
//                    InternalDeploymentStatus(
//                        UUID.fromString(ds.id),
//                        UUID.fromString(ds.workflowId),
//                        UUID.fromString(ws.id),
//                        scalingController.computeScore(ds.cpuUsage, ds.memoryUsage),
//                        ds.port
//                    )
//                }
//                scalingControllerMtx.withLock {
//                    scalingController.updateWorkerStatus(workerStatus, deploymentsStatus)
//                }
//            }
//        }
//        return Empty.getDefaultInstance()
//    }
//
//    override fun connectWorkerController(requests: Flow<DeploymentUpdateResponse>): Flow<DeploymentRequest> =
//        flow {
//            requests.collect { dr ->
//                if (dr.type == DeploymentUpdateResponseType.CONNECTED) {
//                    scalingControllerMtx.withLock {
//                        scalingController.connectWorkerDeploymentCreator(UUID.fromString(dr.workerId)) { id, image, cpuLimit, memoryLimit ->
//                            CoroutineScope(Dispatchers.IO).launch {
//                                emit(
//                                    DeploymentRequest.newBuilder()
//                                        .setId(id.toString())
//                                        .setImage(image)
//                                        .setCpuLimit(cpuLimit)
//                                        .setMemoryLimit(memoryLimit)
//                                        .setType(DeploymentRequestType.CREATE)
//                                        .build()
//                                )
//                            }
//                        }
//                        scalingController.connectWorkerDeploymentDestroyer(UUID.fromString(dr.workerId)) { deploymentId ->
//                            CoroutineScope(Dispatchers.IO).launch {
//                                emit(
//                                    DeploymentRequest.newBuilder()
//                                        .setId(deploymentId.toString())
//                                        .setType(DeploymentRequestType.DESTROY)
//                                        .build()
//                                )
//                            }
//                        }
//                    }
//                } else {
//                    scalingControllerMtx.withLock {
//
//                    }
//                }
//            }
//        }
}