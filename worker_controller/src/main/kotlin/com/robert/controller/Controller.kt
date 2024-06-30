package com.robert.controller

import com.robert.Constants
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.logger
import com.robert.scaling.client.ScalingClient
import com.robert.scaling.client.model.DeploymentScalingRequest
import com.robert.service.DockerService
import com.robert.service.ResourceService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.InetAddress

@Scheduler
class Controller : KoinComponent {
    companion object {
        val LOG by logger()
    }

    private val resourceService: ResourceService by inject()
    private val dockerService: DockerService by inject()
    private val scalingClient: ScalingClient by inject()
    private val id = "c2351bbf-57d8-487c-8511-a9756b9bae7d" // TODO: better id mechanism
    private val alias = "boo" // TODO: better alias mechanism
    private val host = InetAddress.getLocalHost().hostAddress

    @SchedulerConsumer(name = "WorkerStatus", interval = "\${${Constants.WORKER_STATUS_INTERVAL}:30s}")
    fun runWorkerStatus() {
        val status = resourceService.getResources()
        val managedContainers = dockerService.getManagedContainers()
        scalingClient.updateWorkerStatus(id, alias, host, status.cpuLoad, 1.0 - status.availableMemory / status.totalMemory, status.availableMemory, managedContainers)
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
            LOG.info("Workflow $workflowId scaled up")
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
}
