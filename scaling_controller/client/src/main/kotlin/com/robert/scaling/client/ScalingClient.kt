package com.robert.scaling.client

import com.robert.Env
import com.robert.docker.DockerContainer
import com.robert.enums.LBAlgorithms
import com.robert.logger
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.scaling.grpc.DeploymentRequestTypeGrpc
import com.robert.scaling.grpc.ScalingServiceGrpcKt
import com.robert.scaller.WorkerState
import com.robert.scaller.Workflow
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

    fun updateWorkerStatus(id: String, alias: String, cpuLoad: Double, memoryLoad: Double, availableMemory: Long, managedContainers: List<DockerContainer>): List<DeploymentScalingRequest> =
        runBlocking {
            return@runBlocking client.updateWorkerStatus(EntityBuilder.newWorkerStatus(id, alias, cpuLoad, memoryLoad, availableMemory, managedContainers))
                .requestsList
                .mapNotNull {
                    val type = when (it.type ?: DeploymentRequestTypeGrpc.UNRECOGNIZED) {
                        DeploymentRequestTypeGrpc.UP -> DeploymentScalingRequest.Type.UP
                        DeploymentRequestTypeGrpc.DOWN -> DeploymentScalingRequest.Type.DOWN
                        DeploymentRequestTypeGrpc.UNRECOGNIZED -> null
                    }
                    // TODO: validate request: eg. needed field are not null
                    if (type == null) {
                        LOG.warn("Not able to process deployment request of unknown type")
                        null
                    } else {
                        DeploymentScalingRequest(it.id, UUID.fromString(it.workflowId), it.image, it.portsList, it.cpuLimit, it.memoryLimit, type)
                    }
                }
        }

    fun updateWorker(id: UUID, status: WorkerState) = runBlocking {
        client.updateWorker(EntityBuilder.newWorkerData(id, status))
    }

    fun deleteWorker(id: UUID) = runBlocking {
        client.removeWorker(EntityBuilder.newIdData(id))
    }

    fun addWorkflow(workflow: Workflow) = runBlocking {
        client.addWorkflow(EntityBuilder.newWorkflowData(workflow))
    }

    fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms) = runBlocking {
        client.updateWorkflow(EntityBuilder.newWorkflowUpdateData(id, minDeployments, maxDeployments, algorithm))
    }

    fun deleteWorkflow(id: UUID) = runBlocking {
        client.removeWorkflow(EntityBuilder.newIdData(id))
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