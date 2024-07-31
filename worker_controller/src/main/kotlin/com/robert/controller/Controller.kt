package com.robert.controller

import com.robert.Env
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.logger
import com.robert.scaling.client.ScalingClient
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.service.DockerService
import com.robert.service.ResourceService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.InetAddress
import java.util.*

@Scheduler
class Controller : KoinComponent {
    companion object {
        val LOG by logger()
    }

    private val resourceService: ResourceService by inject()
    private val dockerService: DockerService by inject()
    private val scalingClient: ScalingClient by inject()

    private val workerIdPath = Env.get("WORKER_ID_PATH", "/worker_id")
    private val id = loadWorkerId()
    private val alias = Env.get("WORKER_ALIAS", InetAddress.getLocalHost().hostName)
    private val host = InetAddress.getLocalHost().hostAddress

    init {
        LOG.info("Starting worker {} ({})", id, alias)
    }

    @SchedulerConsumer(name = "WorkerStatus", interval = "\${WORKER_STATUS_INTERVAL_SECONDS:30s}")
    fun runWorkerStatus() {
        val status = resourceService.getResources()
        val managedContainers = dockerService.getManagedContainers()
        scalingClient.updateWorkerStatus(id, alias, host, status.cpuLoad, 1.0 - status.availableMemory / status.totalMemory.toDouble(), status.availableMemory, managedContainers)
            .forEach { scalingRequest ->
                try {
                    when (scalingRequest.type) {
                        DeploymentScalingRequest.Type.UP -> processScaleUpRequest(
                            scalingRequest.workflowId?.toString() ?: throw RuntimeException("workflow id was not specified"),
                            scalingRequest.image ?: throw RuntimeException("image was not specified"),
                            scalingRequest.memoryLimit ?: throw RuntimeException("memory limit was not specified"),
                            scalingRequest.cpuLimit ?: throw RuntimeException("cpu limit was not specified"),
                            scalingRequest.exposedPorts ?: throw RuntimeException("exposed ports was not specified"),
                        )

                        DeploymentScalingRequest.Type.DOWN -> processScaleDownRequest(
                            scalingRequest.containerId ?: throw RuntimeException("container id was not specified")
                        )
                    }
                } catch (e: Exception) {
                    LOG.error("unable to process scaling request: $scalingRequest", e)
                }
            }
    }

    private fun processScaleUpRequest(workflowId: String, image: String, memoryLimit: Long, cpuLimit: Long, ports: List<Int>) {
        try {
            dockerService.startContainer(workflowId, image, memoryLimit, cpuLimit, ports)
            LOG.info("Workflow {} scaled up with image {} and ports {}", workflowId, image, ports)
        } catch (e: Exception) {
            LOG.error("Unable to fulfill scale up request for workflow {}", workflowId, e)
        }
    }

    private fun processScaleDownRequest(containerId: String) {
        try {
            dockerService.removeContainer(containerId)
            LOG.info("Container $containerId was removed")
        } catch (e: Exception) {
            LOG.error("Unable to fulfill down request to remove container {}", containerId, e)
        }
    }

    private fun loadWorkerId(): String {
        try {
            return File(workerIdPath).readText(Charsets.UTF_8)
        } catch (e: Exception) {
            val id = UUID.randomUUID()
            LOG.warn("Failed to load worker id, generating new id - {} : {}", id, e.message)
            File(workerIdPath).writeText(id.toString(), Charsets.UTF_8)
            return id.toString()
        }
    }
}
