package com.robert.scaling.client

import com.robert.resources.DockerContainer
import com.robert.enums.LBAlgorithms
import com.robert.scaling.grpc.*
import com.robert.enums.WorkerState
import com.robert.resources.Workflow
import java.util.*

internal object EntityBuilder {
    fun newWorkerStatus(id: String, alias: String, host: String, cpuLoad: Double, memoryLoad: Double, availableMemory: Long, managedContainers: List<DockerContainer>): WorkerStatus {
        val workerStatusBuilder = WorkerStatus.newBuilder()
            .setId(id)
            .setAlias(alias)
            .setHost(host)
            .setCpuLoad(cpuLoad)
            .setAvailableMemory(availableMemory)
            .setMemoryLoad(memoryLoad)
        managedContainers.forEach { container ->
            workerStatusBuilder.addDeployments(newDeploymentStatus(container))
        }
        return workerStatusBuilder.build()
    }

    fun newWorkerData(id: UUID, state: WorkerState): WorkerData {
        return WorkerData.newBuilder()
            .setId(id.toString())
            .setState(WorkerStateGrpc.valueOf(state.toString()))
            .build()
    }

    fun newWorkflowData(workflow: Workflow): WorkflowData {
        val builder = WorkflowData.newBuilder()
            .setId(workflow.id.toString())
            .setImage(workflow.image)
            .setMemoryLimit(workflow.memoryLimit)
            .setCpuLimit(workflow.cpuLimit)
            .setAlgorithm(WorkflowAlgorithm.valueOf(workflow.algorithm.toString()))
            .putAllPathsMapping(workflow.pathsMapping)

        workflow.minDeployments?.let { builder.setMinDeployments(it) }
        workflow.maxDeployments?.let { builder.setMaxDeployments(it) }

        return builder.build()
    }

    fun newWorkflowUpdateData(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms): WorkflowUpdateData {
        val builder = WorkflowUpdateData.newBuilder()
            .setId(id.toString())
            .setAlgorithm(WorkflowAlgorithm.valueOf(algorithm.toString()))

        minDeployments?.let { builder.setMinDeployments(it) }
        maxDeployments?.let { builder.setMaxDeployments(it) }

        return builder.build()
    }

    fun newIdData(id: UUID): IdData {
        return IdData.newBuilder()
            .setId(id.toString())
            .build()
    }

    private fun newDeploymentStatus(container: DockerContainer): DeploymentStatus {
        val deploymentStatus = DeploymentStatus.newBuilder()
            .setContainerId(container.containerID)
            .setWorkflowId(container.workflowId.toString())
            .setCpuUsage(container.cpuUsage)
            .setMemoryUsage(container.memoryUsage)
        container.ports.forEach { mapping ->
            deploymentStatus.addPortsMapping(
                PortMapping.newBuilder()
                    .setPublicPort(mapping.publicPort)
                    .setPrivatePort(mapping.privatePort)
                    .build()
            )
        }
        return deploymentStatus.build()
    }
}