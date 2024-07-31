package com.robert.scaling.client

import com.google.protobuf.Empty
import com.robert.Env
import com.robert.resources.DockerContainer
import com.robert.enums.LBAlgorithms
import com.robert.logger
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.scaling.client.model.WorkflowDeploymentData
import com.robert.scaling.grpc.DeploymentRequestTypeGrpc
import com.robert.scaling.grpc.ScalingServiceGrpcKt
import com.robert.enums.WorkerState
import com.robert.resources.Workflow
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.*
import java.util.concurrent.TimeUnit

class ScalingClient : Closeable {
    companion object {
        val LOG by logger()
    }

    private lateinit var channel: ManagedChannel
    private lateinit var client: ScalingServiceGrpcKt.ScalingServiceCoroutineStub

    @Synchronized
    fun connect(): ScalingClient {
        val host = Env.get("SCALING_CONTROLLER_HOST")
        val port = Env.getInt("SCALING_CONTROLLER_PORT")

        LOG.info("Connecting to scaling controller at $host:$port")
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build()

        client = ScalingServiceGrpcKt.ScalingServiceCoroutineStub(channel)
        return this
    }

    fun updateWorkerStatus(
        id: String,
        alias: String,
        host: String,
        cpuLoad: Double,
        memoryLoad: Double,
        availableMemory: Long,
        managedContainers: List<DockerContainer>
    ): List<DeploymentScalingRequest> =
        runBlocking {
            LOG.debug("Updating worker status with: cpuLoad = {}, memoryLoad = {} and {} managed containers", cpuLoad, memoryLoad, managedContainers.size)
            if (LOG.isTraceEnabled) {
                LOG.trace("Managed containers: [{}]", managedContainers.joinToString(", ") { "{${it.containerID.take(6)} - cpuUsage: ${it.cpuUsage}, memoryUsage: ${it.memoryUsage}}" })
            }

            return@runBlocking client.updateWorkerStatus(EntityBuilder.newWorkerStatus(id, alias, host, cpuLoad, memoryLoad, availableMemory, managedContainers))
                .requestsList
                .mapNotNull {
                    val type = when (it.type ?: DeploymentRequestTypeGrpc.UNRECOGNIZED) {
                        DeploymentRequestTypeGrpc.UP -> DeploymentScalingRequest.Type.UP
                        DeploymentRequestTypeGrpc.DOWN -> DeploymentScalingRequest.Type.DOWN
                        DeploymentRequestTypeGrpc.UNRECOGNIZED -> null
                    }
                    if (type == null) {
                        LOG.warn("Not able to process deployment request of unknown type")
                        null
                    } else {
                        DeploymentScalingRequest(it.id, UUID.fromString(it.workflowId), it.image, it.portsList, it.cpuLimit, it.memoryLimit, type, it.registered)
                    }
                }
        }

    fun getAvailableWorkflowDeploymentsData(): List<WorkflowDeploymentData> = runBlocking {
        return@runBlocking client.getAvailableWorkflowDeploymentsData(Empty.getDefaultInstance())
            .requestsList
            .map {
                WorkflowDeploymentData(UUID.fromString(it.workflowId), it.path, it.host, it.port, LBAlgorithms.valueOf(it.algorithm.toString()), it.score)
            }
    }

    fun updateWorker(id: UUID, status: WorkerState): Boolean = runBlocking {
        return@runBlocking client.updateWorker(EntityBuilder.newWorkerData(id, status)).ok
    }

    fun deleteWorker(id: UUID): Boolean = runBlocking {
        return@runBlocking client.removeWorker(EntityBuilder.newIdData(id)).ok
    }

    fun addWorkflow(workflow: Workflow): Boolean = runBlocking {
        return@runBlocking client.addWorkflow(EntityBuilder.newWorkflowData(workflow)).ok
    }

    fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms): Boolean = runBlocking {
        return@runBlocking client.updateWorkflow(EntityBuilder.newWorkflowUpdateData(id, minDeployments, maxDeployments, algorithm)).ok
    }

    fun deleteWorkflow(id: UUID): Boolean = runBlocking {
        return@runBlocking client.removeWorkflow(EntityBuilder.newIdData(id)).ok
    }

    @Synchronized
    override fun close() {
        if (::channel.isInitialized) {
            channel.shutdown()
                .awaitTermination(5, TimeUnit.SECONDS)
            LOG.info("Disconnected from scaling controller")
        }
    }
}