package com.robert.api.service

import com.robert.enums.LBAlgorithms
import com.robert.exceptions.ValidationException
import com.robert.scaling.client.ScalingClient
import com.robert.scaller.Workflow
import com.robert.storage.repository.WorkflowRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class WorkflowService : KoinComponent {
    companion object {
        fun validateImage(image: String) = Regex("[a-z][a-z0-9]+:[a-z0-9.-]+").matches(image)
        fun validatePath(path: String) = Regex("/[a-zA-Z0-9_-]+").matches(path)
        fun validatePort(port: Int) = port in 1..65535
        fun validateMemory(memory: Long) = memory >= 6291456
        fun validateCpu(cpu: Long) = cpu >= 100 // TODO: better validation
        fun validatePathMapping(pathMapping: Map<String, Int>) = pathMapping.isNotEmpty()
        fun validateDeploymentLimits(minDeployments: Int?, maxDeployments: Int?): Boolean {
            val minD = minDeployments ?: 1
            val maxD = maxDeployments ?: Int.MAX_VALUE
            return minD in 1..maxD
        }
    }

    private val scalingClient by inject<ScalingClient>()
    private val workflowRepository by inject<WorkflowRepository>()

    fun get(id: UUID): Workflow? {
        return workflowRepository.find(id)
    }

    fun getAll(): Collection<Workflow> {
        return workflowRepository.getAll()
    }

    fun add(
        image: String,
        memoryLimit: Long,
        cpuLimit: Long,
        minDeployments: Int?,
        maxDeployments: Int?,
        algorithm: LBAlgorithms,
        pathMapping: Map<String, Int>
    ): Workflow {
        if (!validateDeploymentLimits(minDeployments, maxDeployments)) {
            throw ValidationException("Invalid deployment limits")
        }

        if (!validateImage(image)) {
            throw ValidationException("Invalid workflow image")
        }

        if (!validateMemory(memoryLimit)) {
            throw ValidationException("Invalid workflow memory limit")
        }

        if (!validateCpu(cpuLimit)) {
            throw ValidationException("Invalid workflow cpu limit")
        }

        if (!validatePathMapping(pathMapping)) {
            throw ValidationException("At least one port should be defined")
        }

        for (mapping in pathMapping.entries) {
            if (!validatePath(mapping.key)) {
                throw ValidationException("Invalid workflow path")
            }
            if (!validatePort(mapping.value)) {
                throw ValidationException("Invalid workflow port")
            }
        }

        val workflow = Workflow(UUID.randomUUID(), image, memoryLimit, cpuLimit, minDeployments, maxDeployments, algorithm, pathMapping)
        scalingClient.addWorkflow(workflow)
        workflowRepository.create(workflow)
        return workflow
    }

    fun update(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms) {
        if (!validateDeploymentLimits(minDeployments, maxDeployments)) {
            throw ValidationException("Invalid deployment limits")
        }
        scalingClient.updateWorkflow(id, minDeployments, maxDeployments, algorithm)
        workflowRepository.update(id, minDeployments, maxDeployments, algorithm)
    }

    fun delete(id: UUID) {
        scalingClient.deleteWorkflow(id)
        workflowRepository.delete(id)
    }
}
